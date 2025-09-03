package com.ekote.service;

import com.ekote.entities.User;
import com.ekote.repositories.UserRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Validate user by email and password
    public boolean validateUser(String email, String password) {
        Optional<User> userOptional = userRepository.findByMail(email);

        // Check if user exists and password matches
        return userOptional.isPresent() && BCrypt.checkpw(password, userOptional.get().getPassword());
    }

    // Find user by ID
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    // Register user
    public boolean registerUser(User user) {
        // Check if email already exists
        Optional<User> existingUser = userRepository.findByMail(user.getMail());
        if (existingUser.isPresent()) {
            return false; // Email is already registered
        }

        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }

        // Hash the password before saving
        String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt(12));
        user.setPassword(hashedPassword);

        userRepository.save(user);
        return true; // Registration successful
    }

    // Example business logic for issuing a gun
    public boolean issueGun(String userId, String gunId) {
        // implement business logic (check gun availability, update status, etc.)
        return true;
    }

    // Example business logic for returning a gun
    public boolean returnGun(String userId, String gunId) {
        // implement logic for returning gun
        return true;
    }
}
