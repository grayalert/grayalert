package com.github.grayalert.dto;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder(toBuilder = true)
public class Notification {
    public enum Category { FIRST_OCCURRENCE, NEW_BUCKET, LAST_OCCURRENCE}
    LogBucket logBucket;
    LogMessageAccumulator logMessageAccumulator;
    Category category;
}
