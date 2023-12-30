package com.github.grayalert.core;

import com.github.grayalert.dto.LogMessageAccumulator;
import com.github.grayalert.dto.LogOccurrence;
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
}
