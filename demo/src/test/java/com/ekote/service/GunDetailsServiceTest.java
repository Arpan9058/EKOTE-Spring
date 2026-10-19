package com.ekote.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.ekote.repositories.GunDetailsRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GunDetailsServiceTest {
  @Mock GunDetailsRepository repository;

  @Test
  void missingIdentifierReturnsEmptyList() {
    when(repository.findGunDetails("missing")).thenReturn(List.of());
    assertTrue(new GunDetailsService(repository).getGunDetails("missing").isEmpty());
  }
}
