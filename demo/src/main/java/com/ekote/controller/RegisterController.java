package com.ekote.controller;

import com.ekote.entities.User;
import com.ekote.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class RegisterController {

  private final UserService userService;

  public RegisterController(UserService userService) {
    this.userService = userService;
  }

  @PostMapping("/register")
  public void registerUser(
      @RequestParam("user_name") @NotBlank String name,
      @RequestParam("user_mail") @NotBlank @Email String mail,
      @RequestParam("user_pass") @NotBlank @Size(min = 8, max = 72) String pass,
      @RequestParam("user_id") @Positive Long id,
      HttpServletResponse response)
      throws IOException {

    User user = new User();
    user.setId(id);
    user.setName(name);
    user.setPassword(pass); // will be hashed inside service
    user.setMail(mail);
    user.setRole("USER");

    boolean success = userService.registerUser(user);

    if (success) {
      response.sendRedirect("login?success=registration_successful");
    } else {
      response.sendRedirect("register?error=duplicate_identity");
    }
  }
}
