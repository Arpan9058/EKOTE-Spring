package com.ekote.controller;

import com.ekote.dto.GunDetailsDTO;
import com.ekote.dto.RecordBookDTO;
import com.ekote.service.GunService;
import com.ekote.service.RecordBookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/gunDetails")
public class GunPostQR {

    @Autowired
    private GunService gunService;

//    public RecordBookController(RecordBookService recordBookService) {
//        this.recordBookService = recordBookService;
//    }

    @GetMapping("/{uniqueIdentifier}")
    public String getGunInfoByUniqueIdentifier(@PathVariable("uniqueIdentifier") String uniqueIdentifier, Model model) {
        System.out.println("Fetching records for uniqueIdentifier: " + uniqueIdentifier);

        // Fetch the current gun details for the unique identifier
        Optional<RecordBookDTO> gunDetails = gunService.getGunDetailsByUniqueIdentifier(uniqueIdentifier);
        System.out.println("GunDetails: " + gunDetails);

        // Fetch the past records for the unique identifier
        List<RecordBookDTO> pastRecords = gunService.getPastRecordsByUniqueIdentifier(uniqueIdentifier);
        System.out.println("Past Records: " + pastRecords);

        // If gun details are found, pass them to the model
        if (gunDetails.isPresent()) {
            model.addAttribute("gunDetails", gunDetails.get());
        } else {
            model.addAttribute("error", "Gun details not found for the provided unique identifier.");
        }

        // Pass the list of past records
        model.addAttribute("pastRecords", pastRecords);

        return "gun-info";  // Thymeleaf template name
    }

}
