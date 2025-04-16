package com.github.grayalert.http;

import com.github.grayalert.core.UTCClock;
import com.github.grayalert.dto.LogEntry;
import feign.Feign;
import feign.auth.BasicAuthRequestInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import com.github.grayalert.input.CSVLogParser;
import com.github.grayalert.input.LogFetcher;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class GrayLogFetcher implements LogFetcher {

    private final CSVLogParser csvLogParser;
    private final String baseUrl;
    private final String username;
    private final String password;
    private final UTCClock utcClock;

    @Override
    public List<LogEntry> fetchLogEntries(Long minTimestamp) {
        String query = "level: 3";
        // Calculate timerange in seconds, ensuring it's at least 24 hours
        int timerange = Math.max(86400, (int) (utcClock.getCurrentTimeMillis() / 1000 - minTimestamp / 1000));
        BasicAuthRequestInterceptor requestInterceptor = new BasicAuthRequestInterceptor(username, password);
        GraylogClient client = Feign.builder()
                .requestInterceptor(requestInterceptor)
                .target(GraylogClient.class, baseUrl);

        String fields = "_id,timestamp,source,message,trace_id,traceId,logger_name,LoggerName";
        log.info("Fetching logs from {} graylog with query {} and timerange {}", baseUrl, query, timerange);
        String response = client.getLogs(fields, query, timerange);
        log.info("Received {} characters large response from {}", response.length(), baseUrl);
        try {
            CSVParser parser = new CSVParser(new StringReader(response), CSVFormat.DEFAULT.withFirstRecordAsHeader());
            List<LogEntry> records = new ArrayList<>();
            for (CSVRecord csvRecord : parser) {
                LogEntry record = csvLogParser.parseCSVRecord(csvRecord);
                record.setGraylogBaseUrl(baseUrl);
                records.add(record);
            }
            return records;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
