package com.github.grayalert.web;

import com.github.grayalert.core.LogExampleService;
import com.github.grayalert.persistence.LogExample;
import com.github.grayalert.persistence.DBManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class WebController {

  private final LogExampleService logExampleService;

  @GetMapping("/")
  public String index(Model model, @RequestParam(value = "maxAge", required = false) Long maxAge) {
    List<LogExample> records = logExampleService.loadAndSetHtmlLinks(maxAge);
    model.addAttribute("rows", records);
    model.addAttribute("rowCount", records.size());
    return "list";
  }
}
