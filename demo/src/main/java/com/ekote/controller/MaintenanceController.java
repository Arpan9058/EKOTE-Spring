package com.ekote.controller;

import com.ekote.service.MaintenanceService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/updateMaintenance")
public class MaintenanceController {

  private final MaintenanceService service;

  public MaintenanceController(MaintenanceService service) {
    this.service = service;
  }

  @PostMapping
  public void updateMaintenance(
      @RequestParam("gun_id") @NotBlank @Size(max = 255) String gunId,
      @RequestParam("maintenance_freq") @Min(1) @Max(120) int frequency,
      HttpServletResponse response)
      throws Exception {

    boolean updated = service.updateMaintenance(gunId, frequency);

    if (updated) {
      response.sendRedirect("Maintenance?success=Details_added");
    } else {
      response.sendRedirect("Maintenance?error=failed");
    }
  }
}
