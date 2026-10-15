package com.ekote.service;

import com.ekote.entities.MovementType;
import com.ekote.repositories.GunInfoRepository;
import com.ekote.repositories.IssueRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReturnService {
  private final IssueRepository issues;
  private final GunInfoRepository items;
  private final InventoryStockService stock;

  public ReturnService(
      IssueRepository issues, GunInfoRepository items, InventoryStockService stock) {
    this.issues = issues;
    this.items = items;
    this.stock = stock;
  }

  @Transactional
  public String returnGun(Long userId, String identifier) {
    var item = items.findByUniqueIdentifier(identifier);
    if (item.isEmpty()) return "Gun not issued or not found";
    var gun = stock.lockGun(item.get().getGun().getId());
    if (gun.isEmpty()) return "Gun model not found in inventory";
    // All mutations acquire the inventory row before reading the active issue under a current-read
    // lock.
    var active = issues.findActiveForUpdate(gun.get().getId(), identifier);
    if (active.isEmpty() || !active.get().getUserId().equals(userId))
      return "Gun not issued or not found";
    var issue = active.get();
    stock.apply(gun.get(), 1, MovementType.RETURN, identifier);
    issue.setReturnDate(LocalDateTime.now());
    issue.setStatus("returned");
    issues.save(issue);
    return "Gun returned successfully";
  }
}
