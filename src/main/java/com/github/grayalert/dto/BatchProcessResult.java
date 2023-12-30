package com.github.grayalert.dto;

import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.util.Collection;

@Data
@SuperBuilder(toBuilder = true)
public class BatchProcessResult {
    String baseGraylogUrl;
    Collection<Notification> notifications;
    long durationInMsec;
}
