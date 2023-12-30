package com.github.grayalert.web;

import com.github.grayalert.core.BatchProcessor;
import com.github.grayalert.core.UTCClock;
import com.github.grayalert.persistence.LogExampleComparator;
import lombok.RequiredArgsConstructor;
import com.github.grayalert.persistence.LogExample;


import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


@RequiredArgsConstructor
@Controller
public class RESTController {

    private final BatchProcessor batchProcessor;
    private final UTCClock utcClock;

    public List<LogExample> getAllLogs() {
        List<LogExample> logExamples = batchProcessor.saveExamples();
        Collections.sort(logExamples, new LogExampleComparator());
        return logExamples;
    }

    @GetMapping("/")
    public String index(@RequestParam(name = "maxAge", required = false) Integer maxAge, Model model) {
        List<LogExample> rows = getAllLogs();
        if (maxAge != null) {
            rows = rows.stream().filter(p -> filterByAge(maxAge, p)).collect(Collectors.toList());
        }
        model.addAttribute("rows", rows);
        model.addAttribute("rowCount", rows.size());
        return "list";
    }

    private boolean filterByAge(Integer maxAge, LogExample p) {
        long ageInMsec = utcClock.getCurrentTimeMillis() - p.calculateLastTimestamp();
        return ageInMsec/1000 < maxAge;
    }

}