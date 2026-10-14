package com.ekote.controller;

import com.ekote.entities.User;
import com.ekote.repositories.UserRepository;
import com.ekote.service.IssueService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/issue")
public class IssueController {
  private final UserRepository userRepository;
  private final IssueService issueService;

  public IssueController(UserRepository users, IssueService service) {
    userRepository = users;
    issueService = service;
  }

  @GetMapping("/findUser")
  public String findUser(@RequestParam("user_id") @Positive Long userId, Model model) {
    User user = userRepository.findById(userId).orElse(null);
    if (user == null) {
      model.addAttribute("userNotFound", true);
    } else {
      model.addAttribute("user", user);
    }
    return "issueReturn";
  }

  @PostMapping
  public ResponseEntity<String> issueGun(
      @RequestParam @Positive Long userId, @RequestParam @NotBlank String uniqueIdentifier) {
    IssueService.IssueResult result = issueService.issueGun(userId, uniqueIdentifier);
    return ResponseEntity.status(result.success() ? 200 : 400).body(result.message());
  }
}
