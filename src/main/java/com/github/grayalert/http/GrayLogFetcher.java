package com.github.grayalert.http;

import com.github.grayalert.core.UTCClock;
import com.github.grayalert.dto.LogEntry;
import feign.Feign;
import feign.Logger;
import feign.Logger.JavaLogger;
import feign.Logger.Level;
import feign.Request.Options;
import feign.Response;
import feign.Util;
import feign.auth.BasicAuthRequestInterceptor;
import feign.codec.ErrorDecoder;
import feign.codec.ErrorDecoder.Default;
import feign.slf4j.Slf4jLogger;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
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
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.http.ResponseEntity;

@RequiredArgsConstructor
@Slf4j
public class GrayLogFetcher implements LogFetcher {

  private final CSVLogParser csvLogParser;
  private final String baseUrl;
  private final String username;
  private final String password;
  private final UTCClock utcClock;

  @Override
  public Iterator<LogEntry> fetchLogEntries(Long minTimestamp) {
    String query = "level: 3";
    // Calculate timerange in seconds, ensuring it's at most 24h - otherwise we just consume the same logs
    int duration = (int) (utcClock.getCurrentTimeMillis() / 1000 - minTimestamp / 1000);
    log.info("Duration is {} seconds", duration);
    int timerange = Math.min(86400, duration);
    BasicAuthRequestInterceptor requestInterceptor = new BasicAuthRequestInterceptor(username,
        password);
    Options options = new Options(Duration.ofSeconds(3), Duration.ofSeconds(5), false);
    GraylogClient client = Feign.builder()
        .requestInterceptor(requestInterceptor)
        .logger(new Slf4jLogger(getClass()))
        .logLevel(Level.BASIC)
        .errorDecoder(new MyErrorDecoder())
        .options(options)
        .target(GraylogClient.class, baseUrl);

    String fields = "_id,timestamp,source,message,trace_id,traceId,logger_name,LoggerName";
    log.info("Fetching logs from {} graylog with query {} and timerange {}", baseUrl, query,
        timerange);
    Response response;
    try {
      response = client.getLogs(fields, query, timerange);
      log.info("Received {} characters large response from {}", response.body().length(), baseUrl);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(response.body()
          .asInputStream()));
      return new LogEntryIterator(reader, csvLogParser, baseUrl);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public class LogEntryIterator implements Iterator<LogEntry> {

    private final Iterator<CSVRecord> csvRecordIterator;
    private final CSVLogParser csvLogParser;
    private final String baseUrl;

    public LogEntryIterator(Reader reader, CSVLogParser csvLogParser, String baseUrl)
        throws IOException {
      CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader());
      this.csvRecordIterator = parser.iterator();
      this.csvLogParser = csvLogParser;
      this.baseUrl = baseUrl;
    }

    @Override
    public boolean hasNext() {
      return csvRecordIterator.hasNext();
    }

    @Override
    public LogEntry next() {
      CSVRecord csvRecord = csvRecordIterator.next();
      LogEntry logEntry = csvLogParser.parseCSVRecord(csvRecord);
      logEntry.setGraylogBaseUrl(baseUrl);
      return logEntry;
    }
  }


  private static class MyErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String s, Response response) {
      String bodyString = "";
      try {
        bodyString = Util.toString(response.body().asReader(Util.UTF_8));
      } catch (IOException e) {
        log.error(e.getMessage(), e);
      }
      String msg = "Received status: " + response.status() + " due to reason " + response.reason()
          + " with body " + bodyString;
      IllegalArgumentException e = new IllegalArgumentException(msg);
      log.error(e.getMessage(), e);
      return e;
    }
  }
}
