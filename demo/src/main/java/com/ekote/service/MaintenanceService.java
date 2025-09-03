package com.ekote.service;

import com.ekote.repositories.GunInfoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class MaintenanceService {

    private final GunInfoRepository repository;

    public MaintenanceService(GunInfoRepository repository) {
        this.repository = repository;
    }

    public boolean updateMaintenance(String gunId, int frequency) {
        return repository.findByUniqueIdentifier(gunId).map(gunInfo -> {
            LocalDate manufacturedDate = gunInfo.getManufacturedDate();
            LocalDate maintenanceDate = manufacturedDate;
            LocalDate nextMaintenanceDate = manufacturedDate.plusMonths(frequency);

            gunInfo.setMaintenanceDate(maintenanceDate);
            gunInfo.setNextMaintenanceDate(nextMaintenanceDate);
            gunInfo.setMaintenanceFreq(frequency);

            repository.save(gunInfo);
            return true;
        }).orElse(false);
    }
}
