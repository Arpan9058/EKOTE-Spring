package com.ekote.controller;

import com.ekote.service.GunDetailsService;
import com.ekote.service.GunService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/guns")
public class GunController {
  private static final Logger log = LoggerFactory.getLogger(GunController.class);

  private final GunService gunService;

  @Autowired private GunDetailsService gunDetailsService;

  public GunController(GunService gunService) {
    this.gunService = gunService;
  }

  @DeleteMapping("/delete/{id}")
  public ResponseEntity<String> deleteGun(@PathVariable int id) {
    boolean deleted = gunService.deleteGun(id);
    if (deleted) {
      return ResponseEntity.ok("Gun deleted successfully.");
    } else {
      return ResponseEntity.status(404).body("Gun not found with ID: " + id);
    }
  }

  @GetMapping("/list")
  public List<com.ekote.entities.Gun> getInventory() {
    return gunService.getAllGuns();
  }

  @GetMapping("/details/{uniqueIdentifier}")
  public List<com.ekote.dto.GunDetailsDTO> getGunDetails(@PathVariable String uniqueIdentifier) {
    return gunDetailsService.getGunDetails(uniqueIdentifier);
  }

  @PostMapping("/add")
  public Map<String, String> addGun(
      @RequestParam @NotBlank String gun_name,
      @RequestParam @NotBlank String model,
      @RequestParam @Min(1) int quantity,
      @RequestParam @NotBlank String registered_by,
      @RequestParam(defaultValue = "add") @Pattern(regexp = "(?i)add|subtract") String operation) {

    String message = gunService.addOrUpdateGun(gun_name, model, quantity, registered_by, operation);

    Map<String, String> response = new HashMap<>();
    log.debug("Inventory adjustment result: {}", message);
    response.put(
        "status", (message.contains("added") || message.contains("updated")) ? "success" : "error");
    response.put("message", message);
    return response;
  }
}
