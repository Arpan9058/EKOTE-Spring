package com.ekote.controller;

import com.ekote.service.MaintenanceService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/updateMaintenance")
public class MaintenanceController {

    private final MaintenanceService service;

    public MaintenanceController(MaintenanceService service) {
        this.service = service;
    }

    @PostMapping
    public void updateMaintenance(
            @RequestParam("gun_id") String gunId,
            @RequestParam("maintenance_freq") int frequency,
            HttpServletResponse response) throws Exception {

        boolean updated = service.updateMaintenance(gunId, frequency);

        if (updated) {
            response.sendRedirect("Maintenance?success=Details_added");
        } else {
            response.sendRedirect("Maintenance?error=failed");
        }
    }
}
