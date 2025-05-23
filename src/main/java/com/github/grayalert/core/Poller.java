package com.github.grayalert.core;

import com.github.grayalert.dto.LogEntry;
import com.github.grayalert.dto.BatchProcessResult;
import com.github.grayalert.dto.LogBucket;
import com.github.grayalert.input.MultiSourceLogFetcher;
import com.github.grayalert.output.AlarmManager;
import com.github.grayalert.persistence.DBManager;
import com.github.grayalert.persistence.LogExample;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class Poller {
    private static final int LOAD_PERIOD_SECONDS = 60; // better to take fewer records
    private static final int POLLING_PERIOD = 60;

    private final BatchProcessor batchProcessor;
    private final LogBucketSplitter logBucketSplitter;
    private final DBManager dbManager;
    private final MultiSourceLogFetcher multiSourceLogFetcher;
    private final UTCClock utcClock;
    private final AlarmManager alarmManager;
    private Long minTimestamp;
    private ScheduledExecutorService scheduler;
    private final AtomicBoolean isInitialized = new AtomicBoolean(false);

    @Transactional
    @PostConstruct
    public void init() {
        if (!isInitialized.getAndSet(true)) {
            this.minTimestamp = utcClock.getCurrentTimeMillis() - LOAD_PERIOD_SECONDS * 1000;
            List<LogExample> examples = dbManager.load(null);
            log.info("Loaded {} examples from database", examples.size());
            if (!examples.isEmpty()) {
                // we are only interested in messages that have been logged since
                this.minTimestamp = examples.stream().mapToLong(x -> x.getLastTimestamp() != null ? x.getLastTimestamp() : x.getFirstTimestamp()).max().getAsLong();
            }
            batchProcessor.loadExamples(examples);
            this.scheduler = Executors.newScheduledThreadPool(1);
            scheduler.schedule(this::fetch, 0, TimeUnit.SECONDS);
        }
    }

    public void fetch() {
            if (utcClock.getCurrentTimeMillis() - minTimestamp > LOAD_PERIOD_SECONDS * 1000) {
                log.info("minTimestamp {} is too far in the past, setting to now minus {} seconds", minTimestamp, LOAD_PERIOD_SECONDS);
                minTimestamp = utcClock.getCurrentTimeMillis() - LOAD_PERIOD_SECONDS * 1000;
            }
            log.info("Fetching records using minTimestamp {}", minTimestamp);
        try {
            Iterator<LogEntry> it = multiSourceLogFetcher.fetchLogEntries(minTimestamp);
            log.info("Fetched {} records using minTimestamp {}", it, minTimestamp);
            List<LogEntry> records = processRecords(it);
            OptionalLong maxTimestamp = records.stream().mapToLong(LogEntry::getTimestamp).max();
            if (maxTimestamp.isPresent()) {
                this.minTimestamp = maxTimestamp.getAsLong();
                log.info("Updated minTimestamp to {}", this.minTimestamp);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        if (scheduler != null) {
            scheduler.schedule(this::fetch, POLLING_PERIOD, TimeUnit.SECONDS);
        }
    }

    private List<LogEntry> processRecords(Iterator<LogEntry> iterator) {
        try {
            AtomicInteger counter = new AtomicInteger(0);
            List<LogEntry> records = StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false
            ).peek(item -> {if (counter.incrementAndGet() % 100_000 == 0) {
                log.info("Received {} so far and the current item is {}", counter.get(), item);
            }}).collect(Collectors.toList());
            Map<LogBucket, List<LogEntry>> buckets = logBucketSplitter.splitLogEntries(records);
            BatchProcessResult batchProcessResult = batchProcessor.processBuckets(buckets);
            alarmManager.process(batchProcessResult);
            List<LogExample> examples = batchProcessor.saveExamples();
            dbManager.save(examples);
            log.info("Processed {} records in {} msec", batchProcessResult.getNotifications().size() ,batchProcessResult.getDurationInMsec());
            return records;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return Collections.emptyList();
    }

    // For testing purposes
    public void triggerFetch() {
        fetch();
    }
}
