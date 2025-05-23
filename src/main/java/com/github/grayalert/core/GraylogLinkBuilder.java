package com.github.grayalert.core;

import com.github.grayalert.config.GraylogConfiguration;
import com.github.grayalert.dto.LogMessageAccumulator;
import com.github.grayalert.dto.LogOccurrence;
import com.github.grayalert.persistence.LogExample;
import jakarta.annotation.PostConstruct;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GraylogLinkBuilder {

  private final GraylogConfiguration graylogConfiguration;
  private Map<String, String> baseUrlMappings = Map.of();

  private Map<String, String> provideBaseUrlToWebUrlMappings() {
    Map<String, String> baseUrlMappings = graylogConfiguration.getInstances().entrySet().stream()
        .filter(
            entry -> entry.getValue().getBaseUrl() != null && entry.getValue().getWebUrl() != null)
        .collect(Collectors.toMap(
            entry -> entry.getValue().getBaseUrl(),
            entry -> entry.getValue().getWebUrl()
        ));

    return baseUrlMappings;
  }

  @PostConstruct
  public void init() {
    baseUrlMappings = provideBaseUrlToWebUrlMappings();
  }

  public String getGraylogLink(LogMessageAccumulator logMessageAccumulator) {
    StringBuilder html = new StringBuilder();

    if (logMessageAccumulator.getFirst() != null) {
      LogOccurrence first = logMessageAccumulator.getFirst();
      LogExample firstExample = new LogExample();
      firstExample.setBaseUrl(first.getBaseUrl());
      firstExample.setFirstTraceId(first.getTraceId());
      firstExample.setFirstTimestamp(first.getTimestamp());
      String firstUrl = calculateAbsoluteUrl(firstExample, TraceType.FIRST);
      html.append("<a href=\"").append(firstUrl).append("\">first</a>");
    }

    if (logMessageAccumulator.getLast() != null) {
      if (html.length() > 0) {
        html.append("&nbsp;");
      }
      LogOccurrence last = logMessageAccumulator.getLast();
      LogExample lastExample = new LogExample();
      lastExample.setBaseUrl(last.getBaseUrl());
      lastExample.setLastTraceId(last.getTraceId());
      lastExample.setLastTimestamp(last.getTimestamp());
      String lastUrl = calculateAbsoluteUrl(lastExample, TraceType.LAST);
      html.append("<a href=\"").append(lastUrl).append("\">last</a>");
    }

    return html.toString();
  }


  public String calculateAbsoluteUrl(LogExample logExample,
      TraceType traceType) {
    String traceId =
        traceType == TraceType.FIRST ? logExample.getFirstTraceId() : logExample.getLastTraceId();
    Long timestamp = traceType == TraceType.FIRST ? logExample.getFirstTimestamp()
        : logExample.getLastTimestamp();

    if (traceId == null || timestamp == null) {
      throw new IllegalArgumentException(
          traceType + "TraceId and " + traceType + "Timestamp must not be null");
    }

    // Map the base URL
    String baseUrl = null;
    if (logExample.getBaseUrl() != null) {
       baseUrl = baseUrlMappings.getOrDefault(logExample.getBaseUrl(), logExample.getBaseUrl());
    }


    // Format the timestamps
    Instant instant = Instant.ofEpochMilli(timestamp);
    String from = DateTimeFormatter.ISO_INSTANT.format(instant.minusSeconds(2));
    String to = DateTimeFormatter.ISO_INSTANT.format(instant.plusSeconds(2));

    // Encode the query parameters
    String encodedTraceId = URLEncoder.encode(traceId, StandardCharsets.UTF_8);
    String query = URLEncoder.encode("trace_id:" + encodedTraceId + " OR traceId:" + encodedTraceId,
        StandardCharsets.UTF_8);

    // Build the URL
    return String.format(
        "%s/search?rangetype=absolute&q=%s&from=%s&to=%s",
        baseUrl, query, from, to
    );
  }

  public enum TraceType {
    FIRST,
    LAST
  }

  public String getGraylogHtmlLinks(LogExample logExample) {
    StringBuilder html = new StringBuilder();

    if (logExample.getFirstTraceId() != null && logExample.getFirstTimestamp() != null) {
      String firstUrl = calculateAbsoluteUrl(logExample, TraceType.FIRST);
      html.append("<a href=\"").append(firstUrl).append("\">first</a>");
    }

    if (logExample.getLastTraceId() != null && logExample.getLastTimestamp() != null) {
      if (html.length() > 0) {
        html.append("&nbsp;");
      }
      String lastUrl = calculateAbsoluteUrl(logExample, TraceType.LAST);
      html.append("<a href=\"").append(lastUrl).append("\">last</a>");
    }

    return html.toString();
  }
}
