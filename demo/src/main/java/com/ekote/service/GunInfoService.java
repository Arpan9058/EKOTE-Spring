package com.ekote.service;

import com.ekote.entities.Gun;
import com.ekote.entities.GunInfo;
import com.ekote.repositories.GunInfoRepository;
import com.ekote.repositories.GunRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class GunInfoService {

    private final GunRepository gunRepository;
    private final GunInfoRepository gunInfoRepository;

    public GunInfoService(GunRepository gunRepository, GunInfoRepository gunInfoRepository) {
        this.gunRepository = gunRepository;
        this.gunInfoRepository = gunInfoRepository;
    }

    public String addSpecificGuns(String gunName, String gunModel, int quantity,
                                  List<String> identifiers,
                                  List<String> manufacturedDates,
                                  List<String> manufacturedPlaces) {
        Optional<Gun> gunOpt = gunRepository.findByGunNameAndModel(gunName, gunModel);
        if (gunOpt.isEmpty()) {
            return "Error: Gun with specified name and model not found.";
        }

        Gun gun = gunOpt.get();

        for (int i = 0; i < quantity; i++) {
            GunInfo gunInfo = new GunInfo();
            gunInfo.setUniqueIdentifier(identifiers.get(i));
            gunInfo.setGun(gun);
            gunInfo.setManufacturedDate(LocalDate.parse(manufacturedDates.get(i)));
            gunInfo.setManufacturedPlace(manufacturedPlaces.get(i));
            gunInfoRepository.save(gunInfo);
        }

        return "Gun information successfully added.";
    }

}
