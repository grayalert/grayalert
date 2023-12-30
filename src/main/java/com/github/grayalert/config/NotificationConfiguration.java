package com.github.grayalert.config;

import com.github.grayalert.output.AlarmNotifier;
import com.github.grayalert.core.GraylogLinkBuilder;
import com.github.grayalert.http.MSTeamsClient;
import com.github.grayalert.output.AlarmManager;
import com.github.grayalert.output.LoggingAlarmNotifier;
import com.github.grayalert.output.MSTeamsNotifier;
import feign.Feign;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.slf4j.Slf4jLogger;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "notification")
@Data
@Slf4j
public class NotificationConfiguration {

    private Boolean loggingEnabled;
    private String msTeamsUrl;

    @Bean
    public AlarmManager alarmManager(GraylogLinkBuilder graylogLinkBuilder) {
        List<AlarmNotifier> notifiers = new ArrayList<>();
        if (loggingEnabled != null && loggingEnabled) {
            notifiers.add(new LoggingAlarmNotifier());
        }
        if (msTeamsUrl != null && !msTeamsUrl.isEmpty()) {
            try {
                URI uri = new URI(msTeamsUrl);
                String baseurl = uri.getScheme() + "://" + uri.getHost();
                MSTeamsClient teamsClient = Feign.builder()
                        .logger(new Slf4jLogger("MSTeamsClient"))
                        .encoder(new JacksonEncoder())
                        .decoder(new JacksonDecoder())
                        .target(MSTeamsClient.class, baseurl);
                notifiers.add(new MSTeamsNotifier(teamsClient, uri.getPath()));
            } catch (URISyntaxException e) {
                log.error("Couldn't configure msTeamsClient with url {} due to {}", msTeamsUrl, e.getMessage(), e);
            }
        }
        return new AlarmManager(notifiers, graylogLinkBuilder);
    }
}
