package com.ekote.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.ekote.entities.Gun;
import com.ekote.repositories.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GunInfoServiceTest {
  @Mock GunRepository guns;
  @Mock GunInfoRepository items;
  GunInfoService service;

  @BeforeEach
  void setup() {
    service = new GunInfoService(guns, items);
  }

  @Test
  void savesCompleteBatch() {
    when(guns.findByGunNameAndModel("Rifle", "M1")).thenReturn(Optional.of(new Gun()));
    assertEquals(
        "Gun information successfully added.",
        service.addSpecificGuns(
            "Rifle",
            "M1",
            2,
            List.of("A", "B"),
            List.of("2024-01-01", "2024-02-01"),
            List.of("Factory", "Factory")));
    verify(items)
        .save(
            argThat(
                i ->
                    "A".equals(i.getUniqueIdentifier())
                        && i.getManufacturedDate().toString().equals("2024-01-01")));
    verify(items).save(argThat(i -> "B".equals(i.getUniqueIdentifier())));
  }

  @Test
  void mismatchedCountsDoNotPartiallyWrite() {
    assertTrue(
        service
            .addSpecificGuns(
                "Rifle", "M1", 2, List.of("A"), List.of("2024-01-01"), List.of("Factory"))
            .startsWith("Error"));
    verifyNoInteractions(guns, items);
  }

  @Test
  void invalidLaterDateDoesNotPartiallyWrite() {
    assertTrue(
        service
            .addSpecificGuns(
                "Rifle",
                "M1",
                2,
                List.of("A", "B"),
                List.of("2024-01-01", "2024-02-30"),
                List.of("Factory", "Factory"))
            .startsWith("Error"));
    verifyNoInteractions(guns, items);
  }

  @Test
  void duplicateBatchIdentifiersAreRejected() {
    assertTrue(
        service
            .addSpecificGuns(
                "Rifle",
                "M1",
                2,
                List.of("A", "A"),
                List.of("2024-01-01", "2024-02-01"),
                List.of("Factory", "Factory"))
            .startsWith("Error"));
    verifyNoInteractions(guns, items);
  }

  @Test
  void existingIdentifierCannotBeOverwritten() {
    when(guns.findByGunNameAndModel("Rifle", "M1")).thenReturn(Optional.of(new Gun()));
    when(items.existsByUniqueIdentifier("A")).thenReturn(false);
    when(items.existsByUniqueIdentifier("B")).thenReturn(true);
    assertTrue(
        service
            .addSpecificGuns(
                "Rifle",
                "M1",
                2,
                List.of("A", "B"),
                List.of("2024-01-01", "2024-02-01"),
                List.of("Factory", "Factory"))
            .startsWith("Error"));
    verify(items, never()).save(any());
  }

  @Test
  void missingInventoryIsRejected() {
    when(guns.findByGunNameAndModel("Rifle", "M1")).thenReturn(Optional.empty());
    assertTrue(
        service
            .addSpecificGuns(
                "Rifle", "M1", 1, List.of("A"), List.of("2024-01-01"), List.of("Factory"))
            .startsWith("Error"));
    verifyNoInteractions(items);
  }

  @Test
  void zeroQuantityIsRejected() {
    assertTrue(
        service
            .addSpecificGuns("Rifle", "M1", 0, List.of(), List.of(), List.of())
            .startsWith("Error"));
    verifyNoInteractions(guns, items);
  }

  @Test
  void missingListsAreRejected() {
    assertTrue(service.addSpecificGuns("Rifle", "M1", 1, null, null, null).startsWith("Error"));
    verifyNoInteractions(guns, items);
  }
}
