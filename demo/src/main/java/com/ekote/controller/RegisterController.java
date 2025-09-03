package com.ekote.controller;

import com.ekote.entities.User;
import com.ekote.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
public class RegisterController {

    private final UserService userService;

    public RegisterController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public void registerUser(
            @RequestParam("user_name") String name,
            @RequestParam("user_mail") String mail,
            @RequestParam("user_pass") String pass,
            @RequestParam("user_id") Long id,
            @RequestParam("user_role") String role,
            HttpServletResponse response) throws IOException {

        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setPassword(pass); // will be hashed inside service
        user.setMail(mail);
        user.setRole(role);

        boolean success = userService.registerUser(user);

        if (success) {
            response.sendRedirect("login?success=registration_successful");
        } else {
            response.sendRedirect("register?error=duplicate_mail");
        }
    }
}
