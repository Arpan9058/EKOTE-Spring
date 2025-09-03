package com.ekote.service;

import com.ekote.dto.GunDetailsDTO;
import com.ekote.dto.RecordBookDTO;
import com.ekote.entities.Gun;
import com.ekote.entities.GunInfo;
import com.ekote.repositories.GunInfoRepository;
import com.ekote.repositories.GunRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class GunService {

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
        System.out.println("Fetching records for uniqueIdentifier: " + uniqueIdentifier);
        Optional<RecordBookDTO> gunInfo = gunInfoRepository.getGunDetailsByUniqueIdentifier(uniqueIdentifier);
        System.out.println("GunInfo: " + gunInfo);
        return gunInfo;
    }
    @Transactional
    public List<RecordBookDTO> getPastRecordsByUniqueIdentifier(String uniqueIdentifier) {
        System.out.println("Fetching past records for uniqueIdentifier: " + uniqueIdentifier);
        return gunInfoRepository.getPastRecordsByUniqueIdentifier(uniqueIdentifier);  // Implement this method
    }
}
