package com.github.grayalert.config;

import com.github.grayalert.core.UTCClock;
import com.github.grayalert.http.GrayLogFetcher;
import com.github.grayalert.input.MultiSourceLogFetcher;
import lombok.Data;
import com.github.grayalert.input.CSVLogParser;
import com.github.grayalert.input.LogFetcher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


import java.time.Clock;
import java.time.ZoneId;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@ConfigurationProperties(prefix = "graylog")
@Data
@Slf4j
public class GraylogConfiguration {

    private final Map<String, GraylogParams> instances;

    @Data
    public static class GraylogParams {
        String baseUrl;
        String webUrl;
        String username;
        String password;
    }

    @Bean
    public MultiSourceLogFetcher multiSourceLogFetcher(CSVLogParser csvLogParser, UTCClock utcClock) {
        Collection<LogFetcher> fetchers = instances.values().stream().
                filter(graylogParams -> graylogParams.getBaseUrl() != null && !graylogParams.getBaseUrl().trim().isEmpty()).
                map(graylogParams ->
                createGraylogFetcher(csvLogParser, utcClock, graylogParams)).collect(Collectors.toList());
        if (fetchers.isEmpty()) {
            throw new IllegalArgumentException("No logFetchers configured");
        }
        return new MultiSourceLogFetcher(fetchers);
    }

    @Bean
    public Clock clock() {
        // Return a fixed clock set to 2024-03-20T15:02:00.000Z
        return Clock.system(ZoneId.of("UTC"));
    }


    private GrayLogFetcher createGraylogFetcher(CSVLogParser csvLogParser, UTCClock utcClock, GraylogParams graylogParams) {
        try {
            new URI(graylogParams.getBaseUrl());
        } catch (URISyntaxException e) {
            log.error("Cannot parse {} due to {}", graylogParams.getBaseUrl(), e.getMessage(), e);
            throw new RuntimeException(e);
        }
        return new GrayLogFetcher(csvLogParser, graylogParams.getBaseUrl(), graylogParams.getUsername(), graylogParams.getPassword(), utcClock);
    }
}
