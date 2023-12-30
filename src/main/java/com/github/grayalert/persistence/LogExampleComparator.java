package com.github.grayalert.persistence;

import java.util.Comparator;

public class LogExampleComparator implements Comparator<LogExample> {

    @Override
    public int compare(LogExample o1, LogExample o2) {
        int tsComparison = o2.calculateLastTimestamp().compareTo(o1.calculateLastTimestamp());
        if (tsComparison != 0) {
            return tsComparison;
        }
        // Compare by appName
        int appNameComparison = o1.getAppName().compareTo(o2.getAppName());
        if (appNameComparison != 0) {
            return appNameComparison;
        }

        // Compare by loggerName
        int loggerNameComparison = o1.getLoggerName().compareTo(o2.getLoggerName());
        if (loggerNameComparison != 0) {
            return loggerNameComparison;
        }

        // Finally, compare by message
        return o1.getMessage().compareTo(o2.getMessage());
    }
}
