package com.github.grayalert.core;

import com.github.grayalert.config.GraylogConfiguration;
import com.github.grayalert.core.GraylogLinkBuilder;
import com.github.grayalert.persistence.DBManager;
import com.github.grayalert.persistence.LogExample;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LogExampleService {

    private final DBManager dbManager;
    private final GraylogLinkBuilder graylogLinkBuilder;
    private final GraylogConfiguration graylogConfiguration;

    public List<LogExample> loadAndSetHtmlLinks() {
        // Create baseUrlMappings from GraylogConfiguration
        Map<String, String> baseUrlMappings = graylogConfiguration.getInstances().entrySet().stream()
                .filter(entry -> entry.getValue().getBaseUrl() != null && entry.getValue().getWebUrl() != null)
                .collect(Collectors.toMap(
                        entry -> entry.getValue().getBaseUrl(),
                        entry -> entry.getValue().getWebUrl()
                ));

        // Load LogExamples and set htmlLink
        List<LogExample> logExamples = dbManager.load();
        logExamples.forEach(logExample -> {
            String htmlLink = graylogLinkBuilder.getGraylogHtmlLinks(logExample, baseUrlMappings);
            logExample.setLinkHtml(htmlLink);
        });

        return logExamples;
    }
}