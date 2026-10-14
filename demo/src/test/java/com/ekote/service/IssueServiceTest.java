package com.ekote.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.ekote.entities.*;
import com.ekote.repositories.*;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IssueServiceTest {
  @Mock UserRepository users;
  @Mock GunRepository guns;
  @Mock GunInfoRepository items;
  @Mock IssueRepository issues;
  IssueService service;

  @BeforeEach
  void setup() {
    service = new IssueService(users, guns, items, issues);
  }

  @Test
  void missingUserDoesNotReadInventory() {
    when(users.findById(10L)).thenReturn(Optional.empty());
    assertFalse(service.issueGun(10L, "ITEM").success());
    verifyNoInteractions(guns, items, issues);
  }

  @Test
  void missingItemDoesNotWrite() {
    when(users.findById(10L)).thenReturn(Optional.of(new User()));
    when(items.findByUniqueIdentifier("ITEM")).thenReturn(Optional.empty());
    assertFalse(service.issueGun(10L, "ITEM").success());
    verifyNoInteractions(guns, issues);
  }

  @Test
  void missingInventoryDoesNotCreateIssue() {
    item();
    when(guns.findById(7)).thenReturn(Optional.empty());
    assertFalse(service.issueGun(10L, "ITEM").success());
    verifyNoInteractions(issues);
  }

  @Test
  void alreadyIssuedItemDoesNotChangeStock() {
    Gun gun = available(2);
    when(issues.existsByGunIdAndUniqueIdentifierAndStatusAndReturnDateIsNull(7, "ITEM", "issued"))
        .thenReturn(true);
    assertFalse(service.issueGun(10L, "ITEM").success());
    assertEquals(2, gun.getQuantity());
    verify(guns, never()).save(any());
    verify(issues, never()).save(any());
  }

  @Test
  void zeroStockDoesNotCreateIssue() {
    available(0);
    assertFalse(service.issueGun(10L, "ITEM").success());
    verify(guns, never()).save(any());
    verify(issues, never()).save(any());
  }

  @Test
  void successDecrementsStockAndRecordsTheIssue() {
    Gun gun = available(2);
    assertTrue(service.issueGun(10L, "ITEM").success());
    assertEquals(1, gun.getQuantity());
    verify(guns).save(gun);
    verify(issues)
        .save(
            argThat(
                i ->
                    i.getUserId() == 10L
                        && i.getGunId() == 7
                        && "ITEM".equals(i.getUniqueIdentifier())
                        && "issued".equals(i.getStatus())
                        && i.getIssueDate() != null));
  }

  private Gun item() {
    Gun gun = new Gun();
    gun.setId(7);
    GunInfo item = new GunInfo();
    item.setGun(gun);
    item.setUniqueIdentifier("ITEM");
    when(users.findById(10L)).thenReturn(Optional.of(new User()));
    when(items.findByUniqueIdentifier("ITEM")).thenReturn(Optional.of(item));
    return gun;
  }

  private Gun available(int quantity) {
    Gun gun = item();
    gun.setQuantity(quantity);
    when(guns.findById(7)).thenReturn(Optional.of(gun));
    return gun;
  }
}
