package com.ekote.repositories;

import static org.junit.jupiter.api.Assertions.*;

import com.ekote.entities.*;
import com.ekote.service.GunDetailsService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class GunDetailsRepositoryTest {
  @Autowired UserRepository users;
  @Autowired GunRepository guns;
  @Autowired GunInfoRepository items;
  @Autowired IssueRepository issues;
  @Autowired GunDetailsRepository details;

  @Test
  void recordsOnlyIncludeTheRequestedIndividualItem() {
    User user = new User();
    user.setId(9L);
    user.setName("Member");
    user.setMail("member@example.test");
    user.setRole("USER");
    users.saveAndFlush(user);
    Gun gun = new Gun();
    gun.setGunName("Rifle");
    gun.setModel("M1");
    gun.setQuantity(2);
    guns.saveAndFlush(gun);
    for (String identifier : new String[] {"ITEM-A", "ITEM-B"}) {
      GunInfo item = new GunInfo();
      item.setGun(gun);
      item.setUniqueIdentifier(identifier);
      item.setManufacturedDate(LocalDate.of(2024, 1, 1));
      item.setManufacturedPlace("Factory");
      item.setMaintenanceDate(LocalDate.of(2024, 2, 1));
      items.saveAndFlush(item);
    }
    Issue issue = new Issue();
    issue.setGunId(gun.getId());
    issue.setUserId(9L);
    issue.setUniqueIdentifier("ITEM-A");
    issue.setStatus("issued");
    issue.setIssueDate(LocalDateTime.of(2024, 3, 1, 12, 0));
    issues.saveAndFlush(issue);
    var service = new GunDetailsService(details);
    var records = service.getGunDetails("ITEM-A");
    assertEquals(1, records.size());
    assertEquals("ITEM-A", records.getFirst().getGunIdentifier());
    assertEquals("2024-02-01", records.getFirst().getMaintenanceDate());
    assertTrue(service.getGunDetails("ITEM-B").isEmpty());
    assertEquals(1, items.getPastRecordsByUniqueIdentifier("ITEM-A").size());
    assertTrue(items.getPastRecordsByUniqueIdentifier("ITEM-B").isEmpty());
  }
}
