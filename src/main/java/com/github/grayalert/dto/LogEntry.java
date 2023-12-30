package com.github.grayalert.dto;

import lombok.Data;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

@Data
@SuperBuilder(toBuilder = true)
@Slf4j
public class LogEntry {
    private String id;
    private String message;
    private String traceId;
    private String loggerName;
    private String source;
    private Long timestamp;
    private String graylogBaseUrl;

    // Constructors, getters, and setters


    // Rest of the code (Getters, Setters, etc.)
}
