package com.github.grayalert.core;


import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AppNameExtractor {

    private final Map<String, String> sourceToAppNameMap = new ConcurrentHashMap<>();
    //accounting-service-123fb3-br12 -> accounting-service
    private static final Pattern kubernetesPodPattern = Pattern.compile("^(.*?)-[a-f0-9]+-[a-z0-9]+$");

    public String getAppName(String source) {
        String appName = sourceToAppNameMap.computeIfAbsent(source, this::extractAppName);
        return appName;
    }


    private String extractAppName(String source) {
        Matcher mat = kubernetesPodPattern.matcher(source);
        String appName = source;
        if (mat.matches()) {
            appName = mat.group(1);
        }
        return appName;
    }
}
