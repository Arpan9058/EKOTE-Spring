package com.ekote.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.ekote.entities.User;
import com.ekote.repositories.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
  @Mock UserRepository repository;
  UserService service;
  BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

  @BeforeEach
  void setUp() {
    service = new UserService(repository, encoder);
  }

  @Test
  void hashesPasswordBeforeSaving() {
    User user = user("long-password");
    when(repository.findByMail(user.getMail())).thenReturn(Optional.empty());
    assertTrue(service.registerUser(user));
    assertNotEquals("long-password", user.getPassword());
    assertTrue(encoder.matches("long-password", user.getPassword()));
    verify(repository).save(user);
  }

  @Test
  void refusesDuplicateEmailWithoutWriting() {
    User user = user("long-password");
    when(repository.findByMail(user.getMail())).thenReturn(Optional.of(new User()));
    assertFalse(service.registerUser(user));
    verify(repository, never()).save(any());
  }

  @Test
  void refusesDuplicateIdWithoutOverwritingAnotherUser() {
    User user = user("long-password");
    when(repository.findByMail(user.getMail())).thenReturn(Optional.empty());
    when(repository.existsById(101L)).thenReturn(true);
    assertFalse(service.registerUser(user));
    verify(repository, never()).save(any());
  }

  @Test
  void rejectsEmptyPasswordWithoutWriting() {
    rejectsPassword("");
  }

  @Test
  void rejectsNullPasswordWithoutWriting() {
    rejectsPassword(null);
  }

  private void rejectsPassword(String password) {
    User user = user(password);
    when(repository.findByMail(user.getMail())).thenReturn(Optional.empty());
    assertThrows(IllegalArgumentException.class, () -> service.registerUser(user));
    verify(repository, never()).save(any());
  }

  private User user(String password) {
    User user = new User();
    user.setId(101L);
    user.setMail("member@example.test");
    user.setPassword(password);
    return user;
  }
}
