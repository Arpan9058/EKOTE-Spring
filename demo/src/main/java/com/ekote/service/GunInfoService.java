package com.ekote.service;

import com.ekote.entities.Gun;
import com.ekote.entities.GunInfo;
import com.ekote.repositories.GunInfoRepository;
import com.ekote.repositories.GunRepository;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GunInfoService {
  private final GunRepository gunRepository;
  private final GunInfoRepository gunInfoRepository;

  public GunInfoService(GunRepository guns, GunInfoRepository items) {
    gunRepository = guns;
    gunInfoRepository = items;
  }

  @Transactional
  public String addSpecificGuns(
      String gunName,
      String gunModel,
      int quantity,
      List<String> identifiers,
      List<String> manufacturedDates,
      List<String> manufacturedPlaces) {
    if (quantity < 1
        || quantity > 1000
        || !complete(identifiers, quantity)
        || !complete(manufacturedDates, quantity)
        || !complete(manufacturedPlaces, quantity)) {
      return "Error: Each item requires an identifier, manufacturing date and place.";
    }
    if (new HashSet<>(identifiers).size() != quantity) {
      return "Error: Identifiers in a batch must be unique.";
    }
    List<LocalDate> dates;
    try {
      dates = manufacturedDates.stream().map(LocalDate::parse).toList();
    } catch (DateTimeParseException exception) {
      return "Error: Manufacturing dates must be valid ISO dates (yyyy-MM-dd).";
    }
    Optional<Gun> gun = gunRepository.findByGunNameAndModel(gunName, gunModel);
    if (gun.isEmpty()) {
      return "Error: Gun with specified name and model not found.";
    }
    for (String identifier : identifiers) {
      if (gunInfoRepository.existsByUniqueIdentifier(identifier)) {
        return "Error: An identifier is already registered.";
      }
    }
    for (int index = 0; index < quantity; index++) {
      GunInfo item = new GunInfo();
      item.setUniqueIdentifier(identifiers.get(index));
      item.setGun(gun.get());
      item.setManufacturedDate(dates.get(index));
      item.setManufacturedPlace(manufacturedPlaces.get(index));
      gunInfoRepository.save(item);
    }
    return "Gun information successfully added.";
  }

  private boolean complete(List<String> values, int quantity) {
    return values != null
        && values.size() == quantity
        && values.stream().allMatch(value -> value != null && !value.isBlank());
  }
}
