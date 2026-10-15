package com.ekote;

import static org.junit.jupiter.api.Assertions.*;

import com.ekote.entities.*;
import com.ekote.repositories.*;
import com.ekote.service.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.IntFunction;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
class StockConcurrencyIntegrationTest {
  @Autowired GunRepository guns;
  @Autowired GunInfoRepository items;
  @Autowired UserRepository users;
  @Autowired IssueRepository issues;
  @Autowired StockMovementRepository movements;
  @Autowired IssueService issuing;
  @Autowired ReturnService returning;
  @Autowired InventoryStockService stock;
  @Autowired PlatformTransactionManager transactions;
  Gun gun;

  @BeforeEach
  void fixture() {
    movements.deleteAll();
    issues.deleteAll();
    items.deleteAll();
    guns.deleteAll();
    users.deleteAll();
    User user = new User();
    user.setId(707L);
    user.setMail("concurrent@example.test");
    user.setPassword("unused-hash");
    user.setRole("USER");
    users.saveAndFlush(user);
    gun = new Gun();
    gun.setGunName("Concurrent rifle");
    gun.setModel("T1");
    gun.setQuantity(1);
    gun = guns.saveAndFlush(gun);
    for (String identifier : List.of("CONCURRENT-A", "CONCURRENT-B")) {
      GunInfo item = new GunInfo();
      item.setGun(gun);
      item.setUniqueIdentifier(identifier);
      item.setManufacturedDate(LocalDate.of(2024, 1, 1));
      item.setManufacturedPlace("Test factory");
      items.saveAndFlush(item);
    }
  }

  @AfterEach
  void cleanup() {
    movements.deleteAll();
    issues.deleteAll();
    items.deleteAll();
    guns.deleteAll();
    users.deleteAll();
  }

  @Test
  void parallelIssueOfSameItemSucceedsExactlyOnce() throws Exception {
    assertEquals(1, race(i -> issuing.issueGun(707L, "CONCURRENT-A").success()));
    assertEquals(0, guns.findById(gun.getId()).orElseThrow().getQuantity());
    assertEquals(1, issues.count());
    assertEquals(1, movements.count());
    var movement = movements.findAll().getFirst();
    assertEquals(-1, movement.getDelta());
    assertEquals(MovementType.ISSUE, movement.getOperation());
  }

  @Test
  void differentItemsCannotOversellLastUnit() throws Exception {
    assertEquals(
        1,
        race(i -> issuing.issueGun(707L, i % 2 == 0 ? "CONCURRENT-A" : "CONCURRENT-B").success()));
    assertEquals(0, guns.findById(gun.getId()).orElseThrow().getQuantity());
    assertEquals(1, issues.count());
  }

  @Test
  void parallelReturnsRestoreStockExactlyOnce() throws Exception {
    assertTrue(issuing.issueGun(707L, "CONCURRENT-A").success());
    assertEquals(
        1,
        race(i -> returning.returnGun(707L, "CONCURRENT-A").equals("Gun returned successfully")));
    assertEquals(1, guns.findById(gun.getId()).orElseThrow().getQuantity());
    assertEquals(2, movements.count());
    assertNotNull(issues.findAll().getFirst().getReturnDate());
  }

  @Test
  void outerRollbackUndoesIssueStockAndAuditTogether() {
    assertThrows(
        IllegalStateException.class,
        () ->
            new TransactionTemplate(transactions)
                .executeWithoutResult(
                    status -> {
                      assertTrue(issuing.issueGun(707L, "CONCURRENT-A").success());
                      throw new IllegalStateException("Simulated failure after issue");
                    }));
    assertEquals(1, guns.findById(gun.getId()).orElseThrow().getQuantity());
    assertEquals(0, issues.count());
    assertEquals(0, movements.count());
  }

  @Test
  void stockWritesRequireTransactionAndRejectOverflow() {
    assertThrows(
        org.springframework.transaction.IllegalTransactionStateException.class,
        () -> stock.apply(gun, 1, MovementType.ADD, null));
    gun.setQuantity(Integer.MAX_VALUE);
    guns.saveAndFlush(gun);
    assertThrows(
        ArithmeticException.class,
        () ->
            new TransactionTemplate(transactions)
                .executeWithoutResult(
                    status -> {
                      Gun locked = stock.lockGun(gun.getId()).orElseThrow();
                      stock.apply(locked, 1, MovementType.ADD, null);
                    }));
    assertEquals(Integer.MAX_VALUE, guns.findById(gun.getId()).orElseThrow().getQuantity());
    assertEquals(0, movements.count());
  }

  private long race(IntFunction<Boolean> action) throws Exception {
    int workers = 8;
    var executor = Executors.newFixedThreadPool(workers);
    var ready = new CountDownLatch(workers);
    var start = new CountDownLatch(1);
    List<Future<Boolean>> results = new ArrayList<>();
    try {
      for (int i = 0; i < workers; i++) {
        int index = i;
        results.add(
            executor.submit(
                () -> {
                  ready.countDown();
                  if (!start.await(20, TimeUnit.SECONDS))
                    throw new TimeoutException("Start barrier");
                  return action.apply(index);
                }));
      }
      assertTrue(ready.await(20, TimeUnit.SECONDS));
      start.countDown();
      long accepted = 0;
      for (var result : results) if (result.get(30, TimeUnit.SECONDS)) accepted++;
      return accepted;
    } finally {
      start.countDown();
      executor.shutdownNow();
      assertTrue(executor.awaitTermination(20, TimeUnit.SECONDS));
    }
  }
}
