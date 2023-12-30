package com.github.grayalert.input;

import com.github.grayalert.dto.LogEntry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;

import java.io.FileReader;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class CSVLogParser {

    public List<LogEntry> readFromCSV(String filePath) {
        List<LogEntry> records = new ArrayList<>();

        try (CSVParser parser = new CSVParser(new FileReader(filePath), CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
            for (CSVRecord csvRecord : parser) {
                LogEntry logEntry = parseCSVRecord(csvRecord);
                records.add(logEntry);
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }

        return records;
    }

    public LogEntry parseCSVRecord(CSVRecord csvRecord) {
        LogEntry record = LogEntry.builder().build();
        record.setMessage(csvRecord.get("message"));
        String trace_id = csvRecord.get("trace_id");
        String traceId = csvRecord.get("traceId");
        if (traceId != null && !traceId.isEmpty()) {
            record.setTraceId(traceId);
        } else {
            record.setTraceId(trace_id);
        }
        record.setId(csvRecord.get("_id"));

        String logger_name = csvRecord.get("logger_name");
        String loggerName = csvRecord.get("LoggerName");
        if (loggerName != null && !loggerName.isEmpty()) {
            record.setLoggerName(loggerName);
        } else {
            record.setLoggerName(logger_name);
        }
        record.setSource(csvRecord.get("source"));
        Instant instant = Instant.parse(csvRecord.get("timestamp"));

        // Convert the Instant to milliseconds since the Unix epoch
        long unixTimestampMillis = instant.toEpochMilli();
        record.setTimestamp(unixTimestampMillis);
        return record;
    }
}
