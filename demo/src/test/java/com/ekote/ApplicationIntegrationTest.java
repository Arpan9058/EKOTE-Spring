package com.ekote;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.ekote.entities.Gun;
import com.ekote.entities.GunInfo;
import com.ekote.entities.User;
import com.ekote.repositories.GunInfoRepository;
import com.ekote.repositories.GunRepository;
import com.ekote.repositories.UserRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ApplicationIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired UserRepository users;
  @Autowired GunRepository guns;
  @Autowired GunInfoRepository items;

  @Test
  void maintainedBrowserBundleIsAvailableWithoutAuthentication() throws Exception {
    mvc.perform(get("/vendor/materialize/materialize.min.js"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Materialize v2.4.0")));
  }

  @Test
  void personalInventoryRecordsRequireAdministrator() throws Exception {
    for (String path : new String[] {"/recordBook", "/gunDetails/SECRET-ITEM"}) {
      mvc.perform(get(path).with(user("member").roles("USER"))).andExpect(status().isForbidden());
    }
  }

  @Test
  void rendersPublicFormsWithCsrfTokens() throws Exception {
    for (String path : new String[] {"/login", "/register"}) {
      mvc.perform(get(path))
          .andExpect(status().isOk())
          .andExpect(content().string(containsString("name=\"_csrf\"")));
    }
  }

  @Test
  void rejectsRegistrationWithoutCsrf() throws Exception {
    mvc.perform(post("/register").param("user_name", "Test")).andExpect(status().isForbidden());
  }

  @Test
  void publicRegistrationCannotChooseAdminRoleAndLoginWorks() throws Exception {
    mvc.perform(
            post("/register")
                .with(csrf())
                .param("user_name", "Member")
                .param("user_mail", "member@example.test")
                .param("user_pass", "long-password")
                .param("user_id", "101")
                .param("user_role", "ADMIN"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("login?success=registration_successful"));
    User stored = users.findByMail("member@example.test").orElseThrow();
    assertEquals("USER", stored.getRole());
    assertNotEquals("long-password", stored.getPassword());
    mvc.perform(
            post("/login")
                .with(csrf())
                .param("username", stored.getMail())
                .param("password", "long-password"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/"));
  }

  @Test
  void anonymousInventoryAccessRequiresLogin() throws Exception {
    mvc.perform(get("/guns/list")).andExpect(status().is3xxRedirection());
  }

  @Test
  void ordinaryUserCannotAccessAdminDashboard() throws Exception {
    mvc.perform(get("/dashboard").with(user("member").roles("USER")))
        .andExpect(status().isForbidden());
  }

  @Test
  void inventoryMutationRequiresCsrf() throws Exception {
    mvc.perform(post("/guns/add").with(user("admin").roles("ADMIN")))
        .andExpect(status().isForbidden());
  }

  @Test
  void blankInventoryNameReturnsStructuredValidationError() throws Exception {
    mvc.perform(
            post("/guns/add")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .param("gun_name", " ")
                .param("model", "M1")
                .param("quantity", "2")
                .param("registered_by", "Officer"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("error"))
        .andExpect(jsonPath("$.errors").isNotEmpty());
  }

  @Test
  void negativeInventoryAdjustmentIsRejected() throws Exception {
    mvc.perform(
            post("/guns/add")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .param("gun_name", "Rifle")
                .param("model", "M1")
                .param("quantity", "-2")
                .param("registered_by", "Officer"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void validInventoryRoundTripPersistsStock() throws Exception {
    mvc.perform(
            post("/guns/add")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .param("gun_name", "Rifle")
                .param("model", "M1")
                .param("quantity", "2")
                .param("registered_by", "Officer"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("success"));
    mvc.perform(get("/guns/list").with(user("admin").roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].gunName").value("Rifle"))
        .andExpect(jsonPath("$[0].quantity").value(2));
  }

  @Test
  void healthCheckIncludesDatabaseAndExposesOnlyStatus() throws Exception {
    mvc.perform(get("/actuator/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"))
        .andExpect(jsonPath("$.components").doesNotExist());
  }

  @Test
  void ordinaryUserCannotMutateInventoryEvenWithCsrf() throws Exception {
    mvc.perform(
            post("/guns/add")
                .with(user("member").roles("USER"))
                .with(csrf())
                .param("gun_name", "Rifle")
                .param("model", "M1")
                .param("quantity", "2")
                .param("registered_by", "Member"))
        .andExpect(status().isForbidden());
  }

  @Test
  void malformedBatchReturnsValidationErrorBeforeWriting() throws Exception {
    mvc.perform(
            post("/gunForm/addSpecific")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .param("gunName", "Rifle")
                .param("gunModel", "M1")
                .param("quantity", "2")
                .param("identifiers", "ITEM-1")
                .param("manufacturedDates", "2024-01-01")
                .param("manufacturedPlaces", "Factory"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("error"));
  }

  @Test
  void identifierGenerationRejectsUnboundedQuantity() throws Exception {
    mvc.perform(
            post("/gunForm")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .param("gunName", "Rifle")
                .param("gunModel", "M1")
                .param("quantity", "1000000"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void registrationCannotOverwriteExistingUserById() throws Exception {
    User original = new User();
    original.setId(101L);
    original.setMail("original@example.test");
    original.setName("Original");
    original.setPassword("original-hash");
    original.setRole("ADMIN");
    users.saveAndFlush(original);
    mvc.perform(
            post("/register")
                .with(csrf())
                .param("user_name", "Attacker")
                .param("user_mail", "other@example.test")
                .param("user_pass", "long-password")
                .param("user_id", "101"))
        .andExpect(redirectedUrl("register?error=duplicate_identity"));
    assertEquals("original@example.test", users.findById(101L).orElseThrow().getMail());
    assertEquals("original-hash", users.findById(101L).orElseThrow().getPassword());
  }

  @Test
  void issueAndReturnRoundTripRestoresStockAndPreventsRepeatedReturn() throws Exception {
    User member = new User();
    member.setId(101L);
    member.setMail("member@example.test");
    member.setPassword("hash");
    member.setRole("USER");
    users.saveAndFlush(member);
    Gun gun = new Gun();
    gun.setGunName("Rifle");
    gun.setModel("M1");
    gun.setQuantity(2);
    guns.saveAndFlush(gun);
    GunInfo item = new GunInfo();
    item.setGun(gun);
    item.setUniqueIdentifier("ITEM-1");
    item.setManufacturedDate(LocalDate.of(2024, 1, 1));
    item.setManufacturedPlace("Factory");
    items.saveAndFlush(item);
    mvc.perform(
            post("/issue")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .param("userId", "101")
                .param("uniqueIdentifier", "ITEM-1"))
        .andExpect(status().isOk());
    assertEquals(1, guns.findById(gun.getId()).orElseThrow().getQuantity());
    mvc.perform(
            post("/return")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .param("userId", "101")
                .param("uniqueIdentifier", "ITEM-1"))
        .andExpect(status().isOk());
    assertEquals(2, guns.findById(gun.getId()).orElseThrow().getQuantity());
    mvc.perform(
            post("/return")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .param("userId", "101")
                .param("uniqueIdentifier", "ITEM-1"))
        .andExpect(status().isBadRequest());
    assertEquals(2, guns.findById(gun.getId()).orElseThrow().getQuantity());
  }

  @Test
  void inventoryPageProvidesCsrfHeaderForAjaxAndLogoutForm() throws Exception {
    mvc.perform(get("/inventory_management").with(user("admin").roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("name=\"_csrf_header\"")))
        .andExpect(content().string(containsString("action=\"/logout\"")));
  }
}
