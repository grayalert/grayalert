package com.github.grayalert.core;

import com.github.grayalert.dto.BatchProcessResult;
import com.github.grayalert.dto.LogEntry;
import org.junit.jupiter.api.Test;
import com.github.grayalert.persistence.LogExample;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BatchProcessorTest {

    @Test
    public void testInjectTheSameMessageMultipleTimes() {
        BatchProcessor processor = initBatchProcessor();
        LogEntry logEntry = LogEntry.builder().message("banana").source("currenthost").timestamp(123L).id("id").traceId("traceId").loggerName("com.my.class").build();
        List<LogEntry> logEntries = List.of(logEntry);
        BatchProcessResult ret1 = processor.processRecords(logEntries);
        List<LogExample> examples1 = processor.saveExamples();
        assertEquals(1, ret1.getNotifications().iterator().next().getLogMessageAccumulator().getCount());
        BatchProcessResult ret2 = processor.processRecords(logEntries);
        List<LogExample> examples2 = processor.saveExamples();
        assertEquals(2, ret2.getNotifications().iterator().next().getLogMessageAccumulator().getCount());

        BatchProcessor processor2 = initBatchProcessor();
        processor2.loadExamples(examples2);
        BatchProcessResult ret3 = processor2.processRecords(logEntries);
        assertEquals(3, ret3.getNotifications().iterator().next().getLogMessageAccumulator().getCount());
    }

    //TODO: implement a lot more tests

    private static BatchProcessor initBatchProcessor() {
        MessageProcessor messageProcessor = new MessageProcessor(new MessageTokeniser());
        BatchProcessor processor = new BatchProcessor(messageProcessor, new LogBucketSplitter(new AppNameExtractor()), new GraylogLinkBuilder());
        return processor;
    }
}