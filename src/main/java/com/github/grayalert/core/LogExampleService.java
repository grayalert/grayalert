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

    public List<LogExample> loadAndSetHtmlLinks(Long maxAge) {
        // Create baseUrlMappings from GraylogConfiguration

        // Load LogExamples and set htmlLink
        List<LogExample> logExamples = dbManager.load(maxAge);
        logExamples.forEach(logExample -> {
            String htmlLink = graylogLinkBuilder.getGraylogHtmlLinks(logExample);
            logExample.setLinkHtml(htmlLink);
        });

        return logExamples;
    }
}