package com.ekote.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.ekote.entities.GunInfo;
import com.ekote.repositories.GunInfoRepository;
import java.time.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MaintenanceServiceTest {
  final GunInfoRepository repository = mock(GunInfoRepository.class);
  final MaintenanceService service =
      new MaintenanceService(
          repository, Clock.fixed(Instant.parse("2026-01-31T12:00:00Z"), ZoneOffset.UTC));

  @Test
  void schedulesFromPerformedDateAndHandlesShortMonth() {
    GunInfo item = new GunInfo();
    item.setManufacturedDate(LocalDate.of(2020, 1, 1));
    when(repository.findByUniqueIdentifier("A")).thenReturn(Optional.of(item));
    assertTrue(service.updateMaintenance("A", 1));
    assertEquals(LocalDate.of(2026, 1, 31), item.getMaintenanceDate());
    assertEquals(LocalDate.of(2026, 2, 28), item.getNextMaintenanceDate());
    assertEquals(1, item.getMaintenanceFreq());
    verify(repository).save(item);
  }

  @Test
  void rejectsInvalidIntervalsBeforeAccessingDatabase() {
    for (int interval : new int[] {-1, 0, 121, Integer.MAX_VALUE})
      assertThrows(IllegalArgumentException.class, () -> service.updateMaintenance("A", interval));
    verifyNoInteractions(repository);
  }

  @Test
  void missingItemDoesNotCreateMaintenanceRecord() {
    when(repository.findByUniqueIdentifier("absent")).thenReturn(Optional.empty());
    assertFalse(service.updateMaintenance("absent", 12));
    verify(repository, never()).save(any());
  }
}
