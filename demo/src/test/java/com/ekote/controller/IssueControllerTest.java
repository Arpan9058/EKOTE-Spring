package com.ekote.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.ekote.entities.*;
import com.ekote.repositories.*;
import com.ekote.service.IssueService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class IssueControllerTest {
  @Mock UserRepository users;
  @Mock GunRepository guns;
  @Mock GunInfoRepository info;
  @Mock IssueRepository issues;
  MockMvc mvc;
  Gun gun;

  @BeforeEach
  void setUp() {
    mvc =
        MockMvcBuilders.standaloneSetup(
                new IssueController(users, new IssueService(users, guns, info, issues)))
            .build();
  }

  private void available(int quantity) {
    gun = new Gun();
    gun.setId(7);
    gun.setQuantity(quantity);
    GunInfo item = new GunInfo();
    item.setGun(gun);
    item.setUniqueIdentifier("ITEM-1");
    when(users.findById(10L)).thenReturn(Optional.of(new User()));
    when(info.findByUniqueIdentifier("ITEM-1")).thenReturn(Optional.of(item));
    when(guns.findById(7)).thenReturn(Optional.of(gun));
  }

  @Test
  void refusesAlreadyIssuedItemWithoutChangingStock() throws Exception {
    available(3);
    when(issues.existsByGunIdAndUniqueIdentifierAndStatusAndReturnDateIsNull(7, "ITEM-1", "issued"))
        .thenReturn(true);
    mvc.perform(post("/issue").param("userId", "10").param("uniqueIdentifier", "ITEM-1"))
        .andExpect(status().isBadRequest())
        .andExpect(content().string(containsString("already issued")));
    verify(guns, never()).save(any());
    verify(issues, never()).save(any());
  }

  @Test
  void refusesZeroStockWithoutCreatingIssue() throws Exception {
    available(0);
    mvc.perform(post("/issue").param("userId", "10").param("uniqueIdentifier", "ITEM-1"))
        .andExpect(status().isBadRequest())
        .andExpect(content().string(containsString("not available")));
    verify(guns, never()).save(any());
    verify(issues, never()).save(any());
  }

  @Test
  void issuesAvailableItemAndDecrementsStock() throws Exception {
    available(3);
    mvc.perform(post("/issue").param("userId", "10").param("uniqueIdentifier", "ITEM-1"))
        .andExpect(status().isOk());
    verify(guns).save(argThat(g -> g.getQuantity() == 2));
    verify(issues)
        .save(
            argThat(
                i ->
                    i.getUserId() == 10L
                        && i.getGunId() == 7
                        && "ITEM-1".equals(i.getUniqueIdentifier())
                        && "issued".equals(i.getStatus())
                        && i.getIssueDate() != null));
  }

  @Test
  void refusesUnknownUser() throws Exception {
    when(users.findById(10L)).thenReturn(Optional.empty());
    mvc.perform(post("/issue").param("userId", "10").param("uniqueIdentifier", "ITEM-1"))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(guns, info, issues);
  }
}
