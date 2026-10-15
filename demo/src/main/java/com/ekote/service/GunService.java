package com.ekote.service;

import com.ekote.dto.RecordBookDTO;
import com.ekote.entities.Gun;
import com.ekote.entities.MovementType;
import com.ekote.repositories.GunInfoRepository;
import com.ekote.repositories.GunRepository;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GunService {
  private static final Logger log = LoggerFactory.getLogger(GunService.class);

  private final GunRepository gunRepository;

  private final GunInfoRepository gunInfoRepository;
  private final InventoryStockService stock;

  public GunService(
      GunRepository gunRepository, GunInfoRepository info, InventoryStockService stock) {
    this.gunRepository = gunRepository;
    this.gunInfoRepository = info;
    this.stock = stock;
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

  @Transactional
  public String addOrUpdateGun(
      String gunName, String model, int quantity, String registeredBy, String operation) {
    if (quantity <= 0) {
      throw new IllegalArgumentException("Quantity must be positive");
    }
    if (!"add".equalsIgnoreCase(operation) && !"subtract".equalsIgnoreCase(operation)) {
      throw new IllegalArgumentException("Operation must be add or subtract");
    }
    Optional<Gun> existing = gunRepository.findByGunNameAndModel(gunName, model);

    if (existing.isPresent()) {
      Gun gun =
          stock
              .lockGun(existing.get().getId())
              .orElseThrow(() -> new IllegalArgumentException("Inventory no longer exists"));
      int delta = "subtract".equalsIgnoreCase(operation) ? -quantity : quantity;
      if (delta < 0 && gun.getQuantity() < quantity)
        return "Not enough guns in inventory to subtract";
      try {
        stock.apply(gun, delta, delta > 0 ? MovementType.ADD : MovementType.SUBTRACT, null);
      } catch (ArithmeticException exception) {
        return "Inventory quantity exceeds the supported maximum";
      }
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
      stock.recordNew(newGun);
      return "Gun added to inventory!";
    }
  }

  @Transactional
  public Optional<RecordBookDTO> getGunDetailsByUniqueIdentifier(String uniqueIdentifier) {
    log.debug("Fetching records for identifier {}", uniqueIdentifier);
    Optional<RecordBookDTO> gunInfo =
        gunInfoRepository.getGunDetailsByUniqueIdentifier(uniqueIdentifier);
    return gunInfo;
  }

  @Transactional
  public List<RecordBookDTO> getPastRecordsByUniqueIdentifier(String uniqueIdentifier) {
    log.debug("Fetching past records for identifier {}", uniqueIdentifier);
    return gunInfoRepository.getPastRecordsByUniqueIdentifier(
        uniqueIdentifier); // Implement this method
  }
}
