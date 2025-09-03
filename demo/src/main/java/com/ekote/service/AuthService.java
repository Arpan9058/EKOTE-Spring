package com.ekote.service;

import com.ekote.entities.User;
import com.ekote.repositories.UserRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User authenticate(String email, String password) {
        return userRepository.findByMail(email)
                .filter(user -> BCrypt.checkpw(password, user.getPassword()))
                .orElse(null);
    }
}
