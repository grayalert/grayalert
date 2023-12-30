package com.github.grayalert.core;

import com.github.grayalert.dto.*;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.Trie;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class MessageProcessor {
    private final MessageTokeniser messageTokeniser;


    public Collection<Notification> processBucket(LogBucket logBucket, List<LogEntry> logEntries, Trie<String, LogMessageAccumulator> trie, ProgressNotifier progressNotifier) {
        Map<LogMessageAccumulator, Notification> notifications = new HashMap<>();

        for (LogEntry logEntry : logEntries) {
            String msg = logEntry.getMessage();
            if (trie.isEmpty()) {
                LogMessageAccumulator firstLogMessageAccumulator = createFirstLogTree(logBucket, logEntry);
                addNotification(logBucket, notifications, firstLogMessageAccumulator, Notification.Category.NEW_BUCKET);
                trie.put(msg, firstLogMessageAccumulator);
            } else {
                boolean found = false;
                List<String> parts = messageTokeniser.progressiveSplit(msg);
                for (String subMsg : parts) {
                    //this is the gist of anomaly detection in this project
                    //a message whose prefix already exists in the trie is considered a non-anomaly
                    SortedMap<String, LogMessageAccumulator> prefixTrie = trie.prefixMap(subMsg);
                    if (prefixTrie != null && !prefixTrie.isEmpty()) {
                        LogMessageAccumulator logMessageAccumulator = prefixTrie.get(prefixTrie.lastKey());
                        LogOccurrence logOccurrence = createFromMessage(logEntry);
                        logOccurrence.setLogBucket(logBucket);
                        logMessageAccumulator.setLast(logOccurrence);
                        logMessageAccumulator.setCount(logMessageAccumulator.getCount() + 1);
                        addNotification(logBucket, notifications, logMessageAccumulator, Notification.Category.LAST_OCCURRENCE);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    LogMessageAccumulator firstLogMessageAccumulator = createFirstLogTree(logBucket, logEntry);
                    addNotification(logBucket, notifications, firstLogMessageAccumulator, Notification.Category.FIRST_OCCURRENCE);
                    trie.put(msg, firstLogMessageAccumulator);
                }

            }
            progressNotifier.increment(logBucket);
        }
        progressNotifier.close();
        return notifications.values();
    }

    private static void addNotification(LogBucket logBucket, Map<LogMessageAccumulator, Notification> notifications, LogMessageAccumulator logMessageAccumulator, Notification.Category category) {
        Notification notification = Notification.builder().logMessageAccumulator(logMessageAccumulator).logBucket(logBucket).category(category).build();
        notifications.put(logMessageAccumulator, notification);
    }

    private static LogMessageAccumulator createFirstLogTree(LogBucket logBucket, LogEntry logEntry) {
        LogOccurrence logOccurrence = createFromMessage(logEntry);
        logOccurrence.setLogBucket(logBucket);
        return LogMessageAccumulator.builder().first(logOccurrence).build();
    }

    private static LogOccurrence createFromMessage(LogEntry logEntry) {
        return LogOccurrence.builder().
                message(logEntry.getMessage()).
                baseUrl(logEntry.getGraylogBaseUrl()).
                id(logEntry.getId()).
                traceId(logEntry.getTraceId()).
                timestamp(logEntry.getTimestamp()).build();
    }


}
