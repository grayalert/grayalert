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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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

    @Transactional
    @PostConstruct
    public void init() {
        this.minTimestamp = utcClock.getCurrentTimeMillis() - LOAD_PERIOD_SECONDS * 1000;
        List<LogExample> examples = dbManager.load();
        log.info("Loaded {} examples from database", examples.size());
        if (!examples.isEmpty()) {
            // we are only interested in messages that have been logged since
            this.minTimestamp = examples.stream().mapToLong(x -> x.getLastTimestamp() != null ? x.getLastTimestamp() : x.getFirstTimestamp()).max().getAsLong();
        }
        batchProcessor.loadExamples(examples);
        this.scheduler = Executors.newScheduledThreadPool(1);
        scheduler.schedule(this::fetch, 0, TimeUnit.SECONDS);
    }


    public void fetch() {
        try {
            if (utcClock.getCurrentTimeMillis() - minTimestamp > LOAD_PERIOD_SECONDS * 1000) {
                log.info("minTimestamp {} is too far in the past, setting to now minus {} seconds", minTimestamp, LOAD_PERIOD_SECONDS);
                minTimestamp = utcClock.getCurrentTimeMillis() - LOAD_PERIOD_SECONDS * 1000;
            }
            log.info("Fetching records using minTimestamp {}", minTimestamp);        
            List<LogEntry> records = multiSourceLogFetcher.fetchLogEntries(minTimestamp);
            log.info("Fetched {} records using minTimestamp {}", records.size(), minTimestamp);
            processRecords(records);
            OptionalLong maxTimestamp = records.stream().mapToLong(LogEntry::getTimestamp).max();
            if (maxTimestamp.isPresent()) {
                log.info("Updated minTimestamp to {}", minTimestamp);
                this.minTimestamp = maxTimestamp.getAsLong();
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        scheduler.schedule(this::fetch, POLLING_PERIOD, TimeUnit.SECONDS);

    }


    private void processRecords(List<LogEntry> records) {
        try {
            Map<LogBucket, List<LogEntry>> buckets = logBucketSplitter.splitLogEntries(records);
            BatchProcessResult batchProcessResult = batchProcessor.processBuckets(buckets);
            alarmManager.process(batchProcessResult);
            List<LogExample> examples = batchProcessor.saveExamples();
            dbManager.save(examples);
            log.info("Processed {} records in {} msec", batchProcessResult.getNotifications().size() ,batchProcessResult.getDurationInMsec());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

    }

}
