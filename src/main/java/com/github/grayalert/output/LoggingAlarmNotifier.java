package com.github.grayalert.output;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoggingAlarmNotifier implements AlarmNotifier {

    public void notifyMessage(String message) {
        log.info(message);
    }
}
