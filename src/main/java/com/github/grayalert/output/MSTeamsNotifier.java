package com.github.grayalert.output;

import com.github.grayalert.http.MSTeamsClient;
import com.github.grayalert.http.MSTeamsMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class MSTeamsNotifier implements AlarmNotifier {
    private final MSTeamsClient msTeamsClient;
    private final String msTeamsPath;


    @Override
    public void notifyMessage(String message) {
        try {
            MSTeamsMessage msTeamsMessage = MSTeamsMessage.builder().text(shortenMessage(message)).build();
            String response = msTeamsClient.postMessage(msTeamsPath, msTeamsMessage);
            log.info("Received response {} for message {}", response, message);
        } catch (Exception e) {
            log.error("Unable to send due to {}", e.getMessage(), e);
        }

    }

    private String shortenMessage(String message) {
        if (message.length() > 8000) {
            return message.substring(0, 4000) + "..." + message.substring(4000);
        }
        return message;
    }
}
