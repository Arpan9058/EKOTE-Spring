package com.ekote.service;

import com.ekote.dto.RecordBookDTO;
import com.ekote.entities.Gun;
import com.ekote.repositories.GunInfoRepository;
import com.ekote.repositories.GunRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class GunService {
    private static final Logger log = LoggerFactory.getLogger(GunService.class);

    private final GunRepository gunRepository;

    @Autowired
    private GunInfoRepository gunInfoRepository;

    public GunService(GunRepository gunRepository) {
        this.gunRepository = gunRepository;
    }

    public boolean deleteGun(int gunId) {
        if (gunRepository.existsById(gunId)) {
            gunRepository.deleteById(gunId);
            return true;
        }
        return false;
    }

    public List<Gun> getAllGuns() {
        return gunRepository.findAll();
    }

    public String addOrUpdateGun(String gunName, String model, int quantity, String registeredBy, String operation) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (!"add".equalsIgnoreCase(operation) && !"subtract".equalsIgnoreCase(operation)) {
            throw new IllegalArgumentException("Operation must be add or subtract");
        }
        Optional<Gun> existing = gunRepository.findByGunNameAndModel(gunName, model);

        if (existing.isPresent()) {
            Gun gun = existing.get();
            int updatedQuantity = operation.equalsIgnoreCase("subtract")
                    ? gun.getQuantity() - quantity
                    : gun.getQuantity() + quantity;

            if (updatedQuantity < 0) {
                return "Not enough guns in inventory to subtract";
            }

            gun.setQuantity(updatedQuantity);
            gunRepository.save(gun);
            return "Gun quantity updated successfully!";
        } else {
            if ("subtract".equalsIgnoreCase(operation)) {
                return "Not enough guns in inventory to subtract";
            }
            Gun newGun = new Gun();
            newGun.setGunName(gunName);
            newGun.setModel(model);
            newGun.setQuantity(quantity);
            newGun.setRegisteredBy(registeredBy);

            gunRepository.save(newGun);
            return "Gun added to inventory!";
        }

    }

    @Transactional
    public Optional<RecordBookDTO> getGunDetailsByUniqueIdentifier(String uniqueIdentifier) {
        log.debug("Fetching records for identifier {}", uniqueIdentifier);
        Optional<RecordBookDTO> gunInfo = gunInfoRepository.getGunDetailsByUniqueIdentifier(uniqueIdentifier);
        return gunInfo;
    }
    @Transactional
    public List<RecordBookDTO> getPastRecordsByUniqueIdentifier(String uniqueIdentifier) {
        log.debug("Fetching past records for identifier {}", uniqueIdentifier);
        return gunInfoRepository.getPastRecordsByUniqueIdentifier(uniqueIdentifier);  // Implement this method
    }
}
