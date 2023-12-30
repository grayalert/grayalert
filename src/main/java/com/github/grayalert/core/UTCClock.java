package com.github.grayalert.core;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.ZoneId;

@Service
public class UTCClock {

    private final Clock clock = Clock.system(ZoneId.of("UTC"));
    public Long getCurrentTimeMillis() {
        return clock.millis();
    }
}
