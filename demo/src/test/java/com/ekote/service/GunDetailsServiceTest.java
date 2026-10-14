package com.ekote.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.ekote.repositories.GunDetailsRepository;
import java.sql.Date;
import java.sql.Timestamp;
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

  @Test
  void convertsJdbcDatesNumbersAndNullableFields() {
    Object[] row = {
      "Member",
      "member@example.test",
      "USER",
      "Rifle",
      "M1",
      3L,
      "Officer",
      Date.valueOf("2024-01-01"),
      null,
      Timestamp.valueOf("2024-02-01 12:00:00"),
      null,
      "issued",
      "ITEM"
    };
    when(repository.findGunDetails("ITEM")).thenReturn(java.util.Collections.singletonList(row));
    var result = new GunDetailsService(repository).getGunDetails("ITEM").getFirst();
    assertEquals(3, result.getQuantity());
    assertEquals("2024-01-01", result.getMaintenanceDate());
    assertNull(result.getNextMaintenanceDate());
    assertNull(result.getReturnDate());
    assertEquals("2024-02-01 12:00:00.0", result.getIssueDate());
    assertEquals("ITEM", result.getGunIdentifier());
    assertEquals("member@example.test", result.getUserEmail());
  }
}
