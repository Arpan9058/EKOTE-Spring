package com.ekote.service;

import com.ekote.entities.*;
import com.ekote.repositories.*;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IssueService {
  private final UserRepository userRepository;
  private final InventoryStockService stock;
  private final GunInfoRepository gunInfoRepository;
  private final IssueRepository issueRepository;

  public IssueService(
      UserRepository users,
      InventoryStockService stock,
      GunInfoRepository info,
      IssueRepository issues) {
    userRepository = users;
    this.stock = stock;
    gunInfoRepository = info;
    issueRepository = issues;
  }

  public record IssueResult(boolean success, String message) {}

  @Transactional
  public IssueResult issueGun(Long userId, String uniqueIdentifier) {
    User user = userRepository.findById(userId).orElse(null);
    if (user == null) {
      return new IssueResult(false, "❌ User not found.");
    }

    Optional<GunInfo> optionalGunInfo = gunInfoRepository.findByUniqueIdentifier(uniqueIdentifier);

    if (optionalGunInfo.isEmpty()) {
      return new IssueResult(false, "❌ Gun not found.");
    }

    GunInfo gunInfo = optionalGunInfo.get();

    int gunId = gunInfo.getGun().getId();

    Gun gun = stock.lockGun(gunId).orElse(null);
    if (gun == null) {
      return new IssueResult(false, "❌ Gun model not found in inventory.");
    }

    boolean alreadyIssued =
        issueRepository.findActiveForUpdate(gun.getId(), uniqueIdentifier).isPresent();
    if (alreadyIssued) {
      return new IssueResult(false, "⚠️ Gun is already issued.");
    }

    if (gun.getQuantity() <= 0) {
      return new IssueResult(false, "⚠️ Gun not available in inventory.");
    }

    stock.apply(gun, -1, MovementType.ISSUE, uniqueIdentifier);

    Issue issue = new Issue();
    issue.setUserId(userId);
    issue.setGunId(gun.getId());
    issue.setUniqueIdentifier(uniqueIdentifier);
    issue.setIssueDate(LocalDateTime.now());
    issue.setStatus("issued");
    issueRepository.save(issue);

    return new IssueResult(true, "✅ Gun issued successfully.");
  }
}
