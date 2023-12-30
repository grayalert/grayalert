package com.github.grayalert.output;


import com.github.grayalert.core.GraylogLinkBuilder;
import com.github.grayalert.dto.BatchProcessResult;
import lombok.RequiredArgsConstructor;
import com.github.grayalert.dto.Notification;

import java.util.List;
import java.util.Objects;


@RequiredArgsConstructor
public class AlarmManager {
    public static final int MAX_MESSAGE_LENGTH = 100;
    private final List<AlarmNotifier> notifiers;
    private final GraylogLinkBuilder graylogLinkBuilder;

    public void process(BatchProcessResult result) {
        List<String> newBucketMessages = result.getNotifications().stream().filter(p -> p.getCategory() == Notification.Category.NEW_BUCKET).
                map(this::toString).filter(Objects::nonNull).toList();
        List<String> firstOccurrenceMessages = result.getNotifications().stream().filter(p -> p.getCategory() == Notification.Category.FIRST_OCCURRENCE).
                map(this::toString).filter(Objects::nonNull).toList();

        if (newBucketMessages.isEmpty() && firstOccurrenceMessages.isEmpty()) {
            return;
        }
        // MS teams supports HTML, not sure about other channels.
        StringBuilder sb = new StringBuilder();
        int messageCount = result.getNotifications().stream().mapToInt(x -> x.getLogMessageAccumulator().getCount()).sum();
        sb.append("<h1>Analysed " + messageCount + " messages and found interesting:</h1>");
        if (!newBucketMessages.isEmpty()) {
            sb.append("<h2>New alert categories:</h2> <ul>\n");
            newBucketMessages.forEach(x -> sb.append(x).append("\n"));
            sb.append("</ul>");
        }
        if (!firstOccurrenceMessages.isEmpty()) {
            sb.append("<h2>New first errors:</h2> <ul>\n");
            firstOccurrenceMessages.forEach(x -> sb.append(x).append("\n"));
            sb.append("</ul>");
        }
        notifiers.forEach(notifier -> notifier.notifyMessage(sb.toString()));

    }


    private String toString(Notification notification) {
        String message = notification.getLogMessageAccumulator().getFirst().getMessage();
        String shortenedMessage;
        if (message.length() < MAX_MESSAGE_LENGTH) {
            shortenedMessage = message;
        } else {
            shortenedMessage = message.substring(0,MAX_MESSAGE_LENGTH) + "...";
        }

        return "<li><b>" + notification.getLogBucket().toString() + "</b>: " + graylogLinkBuilder.getGraylogLink(notification.getLogMessageAccumulator()) + ": " + shortenedMessage + "</li>";
    }
}
