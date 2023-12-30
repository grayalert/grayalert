package com.github.grayalert.dto;

import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder(toBuilder = true)
public class LogBucket {
    String appName;
    String loggerName;

    @Override
    public String toString() {
        return appName + ":" + loggerName;
    }
}
