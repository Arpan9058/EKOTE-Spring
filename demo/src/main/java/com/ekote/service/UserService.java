package com.ekote.service;

import com.ekote.entities.User;
import com.ekote.repositories.UserRepository;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  // Find user by ID
  public Optional<User> findById(Long id) {
    return userRepository.findById(id);
  }

  // Register user
  public boolean registerUser(User user) {
    // Check if email already exists
    Optional<User> existingUser = userRepository.findByMail(user.getMail());
    if (existingUser.isPresent() || userRepository.existsById(user.getId())) {
      return false; // Email or user ID is already registered
    }

    if (user.getPassword() == null || user.getPassword().isEmpty()) {
      throw new IllegalArgumentException("Password cannot be null or empty");
    }

    // Hash the password before saving
    String encodedPassword = passwordEncoder.encode(user.getPassword());
    user.setPassword(encodedPassword);

    try {
      return userRepository.insertRegistration(
              user.getId(), user.getName(), user.getMail(), encodedPassword)
          == 1;
    } catch (org.springframework.dao.DataIntegrityViolationException duplicate) {
      // INSERT never merges into another identity, even if concurrent preflight checks raced.
      return false;
    }
  }
}
