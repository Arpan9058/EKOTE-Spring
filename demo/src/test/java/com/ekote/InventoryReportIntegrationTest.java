package com.ekote;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.ekote.entities.*;
import com.ekote.repositories.*;
import com.ekote.service.*;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InventoryReportIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired GunService inventory;
  @Autowired GunRepository guns;
  @Autowired GunInfoRepository items;
  @Autowired StockMovementRepository movements;
  @Autowired MeterRegistry metrics;
  @Autowired PlatformTransactionManager transactions;

  @Test
  @Transactional
  void reportsAreAdminOnlyAndValidateFilters() throws Exception {
    mvc.perform(get("/api/inventory/summary").with(user("member").roles("USER")))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/inventory/summary")).andExpect(status().is3xxRedirection());
    mvc.perform(
            get("/api/inventory/movements").with(user("admin").roles("ADMIN")).param("size", "101"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            get("/api/inventory/movements")
                .with(user("admin").roles("ADMIN"))
                .param("from", "2026-02-01T00:00:00Z")
                .param("to", "2026-01-01T00:00:00Z"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            get("/api/inventory/movements")
                .with(user("admin").roles("ADMIN"))
                .param("gunId", "not-a-number"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @Transactional
  void auditIsAttributedAndReportsAreBounded() throws Exception {
    mvc.perform(
            post("/guns/add")
                .with(user("auditor@example.test").roles("ADMIN"))
                .with(csrf())
                .param("gun_name", "=FORMULA")
                .param("model", "Report-model")
                .param("quantity", "3")
                .param("registered_by", "Officer"))
        .andExpect(status().isOk());
    int id = guns.findByGunNameAndModel("=FORMULA", "Report-model").orElseThrow().getId();
    mvc.perform(
            get("/api/inventory/movements")
                .with(user("admin").roles("ADMIN"))
                .param("gunId", String.valueOf(id)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].actor").value("auditor@example.test"))
        .andExpect(jsonPath("$.content[0].delta").value(3))
        .andExpect(jsonPath("$.totalElements").value(1));
    mvc.perform(
            get("/api/inventory/low-stock")
                .with(user("admin").roles("ADMIN"))
                .param("threshold", "3")
                .param("size", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1));
    mvc.perform(get("/api/inventory/summary").with(user("admin").roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.models").value(1))
        .andExpect(jsonPath("$.availableUnits").value(3));
    String csv =
        mvc.perform(get("/api/inventory/movements.csv").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", "no-store"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertTrue(csv.contains("\"'=FORMULA\""));
  }

  @Test
  @Transactional
  void maintenanceDueIncludesTodayAndExcludesUnscheduledOrFuture() throws Exception {
    Gun gun = new Gun();
    gun.setGunName("Maintenance rifle");
    gun.setModel("T");
    gun.setQuantity(2);
    guns.saveAndFlush(gun);
    for (int i = 0; i < 3; i++) {
      GunInfo item = new GunInfo();
      item.setGun(gun);
      item.setUniqueIdentifier("DUE-" + i);
      item.setManufacturedPlace("Factory");
      item.setManufacturedDate(LocalDate.of(2024, 1, 1));
      item.setNextMaintenanceDate(
          i == 0
              ? LocalDate.now(ZoneOffset.UTC)
              : i == 1 ? LocalDate.now(ZoneOffset.UTC).plusDays(1) : null);
      items.saveAndFlush(item);
    }
    mvc.perform(get("/api/inventory/maintenance-due").with(user("admin").roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].identifier").value("DUE-0"));
    mvc.perform(get("/api/inventory/summary").with(user("admin").roles("ADMIN")))
        .andExpect(jsonPath("$.maintenanceDue").value(1));
    mvc.perform(
            post("/updateMaintenance")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .param("gun_id", "DUE-0")
                .param("maintenance_freq", "0"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void metricsCountCommittedStockChangesAndIgnoreRollback() {
    double before = metrics.counter("inventory.movements", "operation", "ADD").count();
    try {
      assertThrows(
          IllegalStateException.class,
          () ->
              new TransactionTemplate(transactions)
                  .executeWithoutResult(
                      s -> {
                        inventory.addOrUpdateGun("Metrics rifle", "rollback", 1, "Tester", "add");
                        throw new IllegalStateException("rollback");
                      }));
      assertEquals(before, metrics.counter("inventory.movements", "operation", "ADD").count());
      inventory.addOrUpdateGun("Metrics rifle", "commit", 2, "Tester", "add");
      assertEquals(before + 1, metrics.counter("inventory.movements", "operation", "ADD").count());
    } finally {
      movements.deleteAll();
      guns.findByGunNameAndModel("Metrics rifle", "commit").ifPresent(guns::delete);
    }
  }

  @Test
  void metricsRequireAdmin() throws Exception {
    mvc.perform(get("/actuator/metrics").with(user("member").roles("USER")))
        .andExpect(status().isForbidden());
    mvc.perform(get("/actuator/metrics").with(user("admin").roles("ADMIN")))
        .andExpect(status().isOk());
    mvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
  }
}
