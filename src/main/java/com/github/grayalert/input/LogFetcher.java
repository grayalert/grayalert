package com.github.grayalert.input;

import com.github.grayalert.dto.LogEntry;

import java.util.List;

public interface LogFetcher {
    List<LogEntry> fetchLogEntries(Long minTimestamp);
}
