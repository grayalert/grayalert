package com.github.grayalert.core;

import com.github.grayalert.dto.*;
import com.github.grayalert.persistence.LogExample;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.Trie;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


@Service
@Slf4j
@RequiredArgsConstructor
public class BatchProcessor {
    public static final int MAX_SHORT_MESSAGE_LENGTH = 200;
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private final MessageProcessor messageProcessor;
    private final LogBucketSplitter logBucketSplitter;
    private final GraylogLinkBuilder graylogLinkBuilder;
    private final Map<LogBucket, Trie<String, LogMessageAccumulator>> tries = new ConcurrentHashMap<>();

    public BatchProcessResult processBuckets(Map<LogBucket, List<LogEntry>> buckets) {

        Set<LogBucket> logBuckets = buckets.keySet();
        addBuckets(logBuckets);
        long stime = System.currentTimeMillis();

        List<Future<Collection<Notification>>> futures = buckets.entrySet().stream().map(entry -> pool.submit(() ->
                messageProcessor.processBucket(entry.getKey(), entry.getValue(), tries.get(entry.getKey()), new ConsoleProgressNotifier(entry)))).toList();
        Collection<Notification> processedMessages = futures.stream().map(f -> {
            try {
                return f.get();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new RuntimeException(e);
            }
        }).flatMap(Collection::stream).toList();

        long duration = System.currentTimeMillis() - stime;

        return BatchProcessResult.builder().notifications(processedMessages).durationInMsec(duration).build();
    }

    private void addBuckets(Set<LogBucket> logBuckets) {
        logBuckets.forEach(x -> tries.putIfAbsent(x, new PatriciaTrie<>()));
    }


    public BatchProcessResult processRecords(List<LogEntry> records) {
        Map<LogBucket, List<LogEntry>> buckets = logBucketSplitter.splitLogEntries(records);
        BatchProcessResult batchProcessResult = processBuckets(buckets);
        return batchProcessResult;
    }


    public List<LogExample> saveExamples() {
        List<LogExample> examples = new ArrayList<>();
        for (Map.Entry<LogBucket, Trie<String, LogMessageAccumulator>> entry : tries.entrySet()) {
            for (LogMessageAccumulator logMessageAccumulator : entry.getValue().values()) {
                LogExample example = new LogExample();
                LogOccurrence firstLogOccurrence = logMessageAccumulator.getFirst();
                String message = firstLogOccurrence.getMessage();
                example.setMessage(message);
                String baseUrl = firstLogOccurrence.getBaseUrl();
                String html = graylogLinkBuilder.getGraylogLink(logMessageAccumulator);
                LogBucket logBucket = entry.getKey();
                example.setAppName(logBucket.getAppName());
                example.setBaseUrl(baseUrl);
                example.setLinkHtml(html);
                if (message.length() > MAX_SHORT_MESSAGE_LENGTH) {
                    example.setShortMessage(message.substring(0, MAX_SHORT_MESSAGE_LENGTH) + "...");
                } else {
                    example.setShortMessage(message);
                }

                example.setId(HashUtil.sha256Hash(logBucket.getAppName(), logBucket.getLoggerName(), example.getMessage()));
                example.setLoggerName(logBucket.getLoggerName());
                example.setCount(logMessageAccumulator.getCount());
                example.setFirstTimestamp(firstLogOccurrence.getTimestamp());
                example.setFirstTraceId(firstLogOccurrence.getTraceId());
                example.setFirstGraylogId(firstLogOccurrence.getId());
                if (logMessageAccumulator.getLast() != null) {
                    example.setLastTimestamp(logMessageAccumulator.getLast().getTimestamp());
                    example.setLastTraceId(logMessageAccumulator.getLast().getTraceId());
                    example.setLastGraylogId(logMessageAccumulator.getLast().getId());

                }
                examples.add(example);
            }
        }
        return examples;
    }

    public void loadExamples(List<LogExample> examples) {
        Map<LogBucket, List<LogExample>> buckets = logBucketSplitter.splitLogExamples(examples);
        addBuckets(buckets.keySet());

        for (Map.Entry<LogBucket, List<LogExample>> entry : buckets.entrySet()) {
            Trie<String, LogMessageAccumulator> trie = tries.get(entry.getKey());
            for (LogExample logExample : entry.getValue()) {
                LogOccurrence firstLogOccurrence = LogOccurrence.builder().
                        id(logExample.getFirstGraylogId()).
                        traceId(logExample.getFirstTraceId()).
                        message(logExample.getMessage()).
                        baseUrl(logExample.getBaseUrl()).
                        logBucket(entry.getKey()).
                        timestamp(logExample.getFirstTimestamp()).
                        build();
                LogMessageAccumulator logMessageAccumulator = LogMessageAccumulator.builder().
                        count(logExample.getCount()).
                        first(firstLogOccurrence).
                        build();
                if (logExample.getLastTimestamp() != null) {
                    LogOccurrence lastLogOccurrence = LogOccurrence.builder().
                            id(logExample.getLastGraylogId()).
                            traceId(logExample.getLastTraceId()).
                            message(logExample.getMessage()).
                            logBucket(entry.getKey()).
                            baseUrl(logExample.getBaseUrl()).
                            timestamp(logExample.getLastTimestamp()).
                            build();
                    logMessageAccumulator.setLast(lastLogOccurrence);

                }
                trie.put(logExample.getMessage(), logMessageAccumulator);
            }

        }
    }
}
