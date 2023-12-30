package com.github.grayalert.input;

import com.github.grayalert.dto.LogEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MultiSourceLogFetcher implements LogFetcher {
    private final Collection<LogFetcher> fetchers;

    @Override
    public List<LogEntry> fetchLogEntries(Long minTimestamp) {
        return fetchers.stream().
                map(fetcher -> fetcher.fetchLogEntries(minTimestamp)).
                flatMap(List::stream).
                collect(Collectors.toList());
    }
}
