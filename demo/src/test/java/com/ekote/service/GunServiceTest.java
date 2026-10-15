package com.ekote.service;

import static com.ekote.support.StockFixtures.stock;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.ekote.entities.Gun;
import com.ekote.repositories.GunInfoRepository;
import com.ekote.repositories.GunRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GunServiceTest {
  @Mock GunRepository repository;
  GunService service;

  @BeforeEach
  void setUp() {
    service = new GunService(repository, mock(GunInfoRepository.class), stock(repository));
  }

  @Test
  void createsInventoryWhenAddingNewModel() {
    doAnswer(
            invocation -> {
              Gun gun = invocation.getArgument(0);
              gun.setId(1);
              return gun;
            })
        .when(repository)
        .save(any());
    when(repository.findByGunNameAndModel("Rifle", "M1")).thenReturn(Optional.empty());
    assertEquals(
        "Gun added to inventory!", service.addOrUpdateGun("Rifle", "M1", 3, "Officer", "add"));
    verify(repository)
        .save(
            argThat(
                g ->
                    g.getQuantity() == 3
                        && "Rifle".equals(g.getGunName())
                        && "M1".equals(g.getModel())
                        && "Officer".equals(g.getRegisteredBy())));
  }

  @Test
  void addsToExistingQuantity() {
    Gun gun = existing(5);
    service.addOrUpdateGun("Rifle", "M1", 3, "Officer", "add");
    assertEquals(8, gun.getQuantity());
    verify(repository).save(gun);
  }

  @Test
  void subtractsFromExistingQuantity() {
    Gun gun = existing(5);
    service.addOrUpdateGun("Rifle", "M1", 3, "Officer", "subtract");
    assertEquals(2, gun.getQuantity());
    verify(repository).save(gun);
  }

  @Test
  void refusesInsufficientStockWithoutWriting() {
    Gun gun = existing(2);
    assertEquals(
        "Not enough guns in inventory to subtract",
        service.addOrUpdateGun("Rifle", "M1", 3, "Officer", "subtract"));
    assertEquals(2, gun.getQuantity());
    verify(repository, never()).save(any());
  }

  @Test
  void subtractingMissingModelDoesNotCreateInventory() {
    when(repository.findByGunNameAndModel("Rifle", "M1")).thenReturn(Optional.empty());
    assertEquals(
        "Not enough guns in inventory to subtract",
        service.addOrUpdateGun("Rifle", "M1", 3, "Officer", "subtract"));
    verify(repository, never()).save(any());
  }

  @Test
  void rejectsNegativeQuantityBeforeReadingOrWriting() {
    assertThrows(
        IllegalArgumentException.class,
        () -> service.addOrUpdateGun("Rifle", "M1", -1, "Officer", "subtract"));
    verifyNoInteractions(repository);
  }

  @Test
  void rejectsUnknownOperation() {
    assertThrows(
        IllegalArgumentException.class,
        () -> service.addOrUpdateGun("Rifle", "M1", 1, "Officer", "unknown"));
    verifyNoInteractions(repository);
  }

  private Gun existing(int quantity) {
    Gun gun = new Gun();
    gun.setId(1);
    gun.setQuantity(quantity);
    when(repository.findForUpdateById(1)).thenReturn(Optional.of(gun));
    when(repository.findByGunNameAndModel("Rifle", "M1")).thenReturn(Optional.of(gun));
    return gun;
  }
}
