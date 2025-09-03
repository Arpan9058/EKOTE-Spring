package com.ekote.controller;

import com.ekote.entities.User;
import com.ekote.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class LoginController {

    private final AuthService authService;

    public LoginController(AuthService authService) {
        this.authService = authService;
    }



    @PostMapping("/login")
    public String login(
            @RequestParam("user_mail") String mail,
            @RequestParam("user_pass") String pass,
            HttpSession session) {

        User user = authService.authenticate(mail, pass);

        if (user == null) {
            return "redirect:/login?error=invalid_credentials";
        }

        session.setAttribute("user_id", user.getId());
        session.setAttribute("role", user.getRole());

        if ("admin".equals(user.getRole())) {
            return "redirect:/dashboard";
        } else {
            return "redirect:/login?error=not_admin";
        }

    }

}
