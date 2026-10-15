package com.ekote.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.ekote.entities.*;
import com.ekote.repositories.*;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

class InventoryReportServiceTest {
  final GunRepository guns = mock(GunRepository.class);
  final GunInfoRepository items = mock(GunInfoRepository.class);
  final IssueRepository issues = mock(IssueRepository.class);
  final StockMovementRepository movements = mock(StockMovementRepository.class);
  final InventoryReportService service =
      new InventoryReportService(
          guns,
          items,
          issues,
          movements,
          Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));

  @Test
  void csvEscapesQuotesCommasNewlinesAndSpreadsheetExpressions() {
    assertEquals("\"' =SUM(A1)\"", InventoryReportService.csvCell(" =SUM(A1)"));
    assertEquals("\"'@SUM(A1)\"", InventoryReportService.csvCell("@SUM(A1)"));
    assertEquals("\"a,\"\"b\"\"\nc\"", InventoryReportService.csvCell("a,\"b\"\nc"));
    assertEquals("\"\"", InventoryReportService.csvCell(null));
    assertEquals("\"'\tformula\"", InventoryReportService.csvCell("\tformula"));
  }

  @Test
  void rejectsUnboundedPagesAndInvalidFilters() {
    assertThrows(IllegalArgumentException.class, () -> service.lowStock(-1, 0, 20));
    assertThrows(IllegalArgumentException.class, () -> service.lowStock(5, -1, 20));
    assertThrows(IllegalArgumentException.class, () -> service.lowStock(5, 0, 101));
    assertThrows(IllegalArgumentException.class, () -> service.due(0, 0));
    assertThrows(IllegalArgumentException.class, () -> service.history(0, null, null, 0, 10));
    assertThrows(
        IllegalArgumentException.class,
        () -> service.history(null, Instant.MAX, Instant.MIN, 0, 10));
    verifyNoInteractions(guns, items, issues, movements);
  }

  @SuppressWarnings("unchecked")
  @Test
  void oversizedExportRequiresNarrowerFiltersInsteadOfSilentlyDroppingRows() {
    Gun gun = new Gun();
    gun.setId(1);
    gun.setGunName("Rifle");
    gun.setModel("M1");
    var movement = new StockMovement(gun, 0, 1, MovementType.ADD, null, "admin");
    when(movements.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(movement), PageRequest.of(0, 5000), 5001));
    assertThrows(IllegalArgumentException.class, () -> service.export(null, null, null));
  }
}
