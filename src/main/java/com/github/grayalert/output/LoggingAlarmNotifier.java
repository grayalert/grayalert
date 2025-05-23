package com.github.grayalert.output;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LoggingAlarmNotifier implements AlarmNotifier {

    public void notifyMessage(String message) {
        log.info(message);
    }
}
