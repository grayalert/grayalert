package com.github.grayalert.core;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class UTCClock {
    private final Clock clock;

    public Long getCurrentTimeMillis() {
        return clock.millis();
    }
}
