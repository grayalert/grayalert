package com.github.grayalert.web;

import com.github.grayalert.persistence.DBManager;
import com.github.grayalert.persistence.LogExample;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class RESTController {

    private final DBManager dbManager;

    public RESTController(DBManager dbManager) {
        this.dbManager = dbManager;
    }

    @GetMapping("/logs")
    public List<LogExample> getLogs() {
        return dbManager.load();
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