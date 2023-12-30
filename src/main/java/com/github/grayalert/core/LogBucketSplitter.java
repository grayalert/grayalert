package com.github.grayalert.core;

import com.github.grayalert.dto.LogEntry;
import lombok.RequiredArgsConstructor;
import com.github.grayalert.dto.LogBucket;
import com.github.grayalert.persistence.LogExample;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LogBucketSplitter {

    private final AppNameExtractor appNameExtractor;

    public Map<LogBucket, List<LogEntry>> splitLogEntries(List<LogEntry> logEntries) {
        Map<LogBucket, List<LogEntry>> buckets = logEntries.stream().collect(Collectors.groupingBy(this::extractLogBucket));
        return buckets;
    }

    public Map<LogBucket, List<LogExample>> splitLogExamples(List<LogExample> logEntries) {
        Map<LogBucket, List<LogExample>> buckets = logEntries.stream().collect(Collectors.groupingBy(this::extractLogBucket));
        return buckets;
    }

    private LogBucket extractLogBucket(LogExample logExample) {
        return LogBucket.builder().appName(logExample.getAppName()).loggerName(logExample.getLoggerName()).build();
    }

    private LogBucket extractLogBucket(LogEntry logEntry) {
        String source = logEntry.getSource();
        String appName = appNameExtractor.getAppName(source);
        LogBucket bucket = LogBucket.builder().appName(appName).loggerName(logEntry.getLoggerName()).build();
        return bucket;
    }

}
