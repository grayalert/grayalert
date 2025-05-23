package com.github.grayalert.core;

import com.github.grayalert.dto.LogMessageAccumulator;
import com.github.grayalert.dto.LogOccurrence;
import com.github.grayalert.persistence.LogExample;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class GraylogLinkBuilder {
    public String getGraylogLink(LogMessageAccumulator logMessageAccumulator) {
        LogOccurrence first = logMessageAccumulator.getFirst();
        String html = calculateLink(first, "first");
        if (logMessageAccumulator.getLast() != null) {
            html += "&nbsp;" + calculateLink(logMessageAccumulator.getLast(), "last");
        }
        return html;
    }

    private static String calculateLink(LogOccurrence first, String linkText) {
        String url = calculateUrl(first);
        String prefix = "<a href=\"";
        String suffix = "\">" + linkText + "</a>";
        String html = prefix + url + suffix;
        return html;
    }

    public String calculateUrl(LogMessageAccumulator logMessageAccumulator) {
        LogOccurrence occurrence;
        if (logMessageAccumulator.getLast() != null) {
            occurrence = logMessageAccumulator.getLast();
        } else {
            occurrence = logMessageAccumulator.getFirst();
        }
        return calculateUrl(occurrence);
    }

    private static String calculateUrl(LogOccurrence occurrence) {
        String baseUrl = occurrence.getBaseUrl();

        String prefix = baseUrl;
        String suffix = "&rangetype=relative&from=86400&timestamp=" + occurrence.getTimestamp();
        if (occurrence.getTraceId() != null && !occurrence.getTraceId().isEmpty()) {
            return prefix + "/search?q=trace_id:" + occurrence.getTraceId() + "%20OR%20traceId:" + occurrence.getTraceId() + suffix;
        } else if (occurrence.getId() != null && !occurrence.getId().isEmpty()) {
            return prefix + "/search?q=_id:" + occurrence.getId() + suffix;
        }
        return null;
    }

    public String calculateAbsoluteUrl(LogExample logExample, Map<String, String> baseUrlMappings, TraceType traceType) {
        String traceId = traceType == TraceType.FIRST ? logExample.getFirstTraceId() : logExample.getLastTraceId();
        Long timestamp = traceType == TraceType.FIRST ? logExample.getFirstTimestamp() : logExample.getLastTimestamp();

        if (traceId == null || timestamp == null) {
            throw new IllegalArgumentException(traceType + "TraceId and " + traceType + "Timestamp must not be null");
        }

        // Map the base URL
        String baseUrl = baseUrlMappings.getOrDefault(logExample.getBaseUrl(), logExample.getBaseUrl());
        if (baseUrl == null) {
            throw new IllegalArgumentException("Base URL mapping not found for: " + logExample.getBaseUrl());
        }

        // Format the timestamps
        Instant instant = Instant.ofEpochMilli(timestamp);
        String from = DateTimeFormatter.ISO_INSTANT.format(instant.minusSeconds(2));
        String to = DateTimeFormatter.ISO_INSTANT.format(instant.plusSeconds(2));

        // Encode the query parameters
        String encodedTraceId = URLEncoder.encode(traceId, StandardCharsets.UTF_8);
        String query = URLEncoder.encode("trace_id:" + encodedTraceId + " OR traceId:" + encodedTraceId, StandardCharsets.UTF_8);

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

    public String getGraylogHtmlLinks(LogExample logExample, Map<String, String> baseUrlMappings) {
        StringBuilder html = new StringBuilder();

        if (logExample.getFirstTraceId() != null && logExample.getFirstTimestamp() != null) {
            String firstUrl = calculateAbsoluteUrl(logExample, baseUrlMappings, TraceType.FIRST);
            html.append("<a href=\"").append(firstUrl).append("\">first</a>");
        }

        if (logExample.getLastTraceId() != null && logExample.getLastTimestamp() != null) {
            if (html.length() > 0) {
                html.append("&nbsp;");
            }
            String lastUrl = calculateAbsoluteUrl(logExample, baseUrlMappings, TraceType.LAST);
            html.append("<a href=\"").append(lastUrl).append("\">last</a>");
        }

        return html.toString();
    }
}
