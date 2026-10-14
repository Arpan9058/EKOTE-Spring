package com.ekote.controller;

import com.ekote.dto.RecordBookDTO;
import com.ekote.service.RecordBookService;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/recordBook")
public class RecordBookController {

  private final RecordBookService recordBookService;

  public RecordBookController(RecordBookService recordBookService) {
    this.recordBookService = recordBookService;
  }

  @GetMapping
  public String getRecordBookPage(Model model) {
    List<RecordBookDTO> records = recordBookService.fetchRecordBook();
    model.addAttribute("recordBookList", records);
    return "recordBook"; // Thymeleaf template name without .html
  }
}
