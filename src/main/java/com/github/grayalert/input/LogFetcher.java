package com.github.grayalert.input;

import com.github.grayalert.dto.LogEntry;

import java.util.Iterator;
import java.util.List;

public interface LogFetcher {
    Iterator<LogEntry> fetchLogEntries(Long minTimestamp);
}
