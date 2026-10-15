package com.ekote.service;

import com.ekote.repositories.GunInfoRepository;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MaintenanceService {
  private final GunInfoRepository repository;
  private final Clock clock;

  public MaintenanceService(GunInfoRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
  }

  @Transactional
  public boolean updateMaintenance(String identifier, int frequency) {
    if (frequency < 1 || frequency > 120)
      throw new IllegalArgumentException("Maintenance interval must be between 1 and 120 months");
    return repository
        .findByUniqueIdentifier(identifier)
        .map(
            item -> {
              LocalDate performed = LocalDate.now(clock);
              item.setMaintenanceDate(performed);
              item.setNextMaintenanceDate(performed.plusMonths(frequency));
              item.setMaintenanceFreq(frequency);
              repository.save(item);
              return true;
            })
        .orElse(false);
  }
}
