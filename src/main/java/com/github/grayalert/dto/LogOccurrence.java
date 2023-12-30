package com.github.grayalert.dto;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder(toBuilder = true)
public class LogOccurrence {
    String id;
    String message;
    String traceId;
    LogBucket logBucket;
    Long timestamp;
    String baseUrl;
}
