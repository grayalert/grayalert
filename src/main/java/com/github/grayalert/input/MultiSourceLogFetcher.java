package com.github.grayalert.input;

import com.github.grayalert.dto.LogEntry;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
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
    public Iterator<LogEntry> fetchLogEntries(Long minTimestamp) {
        // Combine iterators into a single stream and return as an iterator
        return fetchers.stream()
            .map(fetcher -> fetcher.fetchLogEntries(minTimestamp)) // Stream<Iterator<LogEntry>>
            .flatMap(this::iteratorToStream) // Combine into a single Stream<LogEntry>
            .iterator();
    }

    private Stream<LogEntry> iteratorToStream(Iterator<LogEntry> iterator) {
        return StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED),
            false
        );
    }
}
