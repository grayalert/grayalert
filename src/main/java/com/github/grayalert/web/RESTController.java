package com.github.grayalert.web;

import com.github.grayalert.core.LogExampleService;
import com.github.grayalert.persistence.DBManager;
import com.github.grayalert.persistence.LogExample;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RESTController {

    private final DBManager dbManager;
    private final LogExampleService logExampleService;


    @GetMapping("/logs")
    public List<LogExample> getLogs() {
        return logExampleService.loadAndSetHtmlLinks(null);
    }

    @PostMapping("/logs/delete-old")
    public ResponseEntity<Void> deleteOldLogs(@RequestParam long ageInSeconds, @RequestParam(required = false) Long referenceTime) {
        if (referenceTime != null) {
            dbManager.deleteOlderThan(ageInSeconds, referenceTime);
        } else {
            dbManager.deleteOlderThan(ageInSeconds);
        }
        return ResponseEntity.ok().build();
    }
}