package com.github.grayalert.web;

import com.github.grayalert.persistence.LogExample;
import com.github.grayalert.persistence.DBManager;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class WebController {

    private final DBManager dbManager;

    public WebController(DBManager dbManager) {
        this.dbManager = dbManager;
    }

    @GetMapping("/")
    public String index(Model model) {
        List<LogExample> records = dbManager.load();
        model.addAttribute("rows", records);
        model.addAttribute("rowCount", records.size());
        return "list";
    }
} 