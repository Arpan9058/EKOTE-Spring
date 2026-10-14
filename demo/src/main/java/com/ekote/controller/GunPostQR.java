package com.ekote.controller;

import com.ekote.dto.RecordBookDTO;
import com.ekote.service.GunService;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/gunDetails")
public class GunPostQR {
  private static final Logger log = LoggerFactory.getLogger(GunPostQR.class);

  @Autowired private GunService gunService;

  @GetMapping("/{uniqueIdentifier}")
  public String getGunInfoByUniqueIdentifier(
      @PathVariable("uniqueIdentifier") String uniqueIdentifier, Model model) {
    log.debug("Fetching records for identifier {}", uniqueIdentifier);

    // Fetch the current gun details for the unique identifier
    Optional<RecordBookDTO> gunDetails =
        gunService.getGunDetailsByUniqueIdentifier(uniqueIdentifier);

    // Fetch the past records for the unique identifier
    List<RecordBookDTO> pastRecords = gunService.getPastRecordsByUniqueIdentifier(uniqueIdentifier);

    // If gun details are found, pass them to the model
    if (gunDetails.isPresent()) {
      model.addAttribute("gunDetails", gunDetails.get());
    } else {
      model.addAttribute("error", "Gun details not found for the provided unique identifier.");
    }

    // Pass the list of past records
    model.addAttribute("pastRecords", pastRecords);

    return "gun-info"; // Thymeleaf template name
  }
}
