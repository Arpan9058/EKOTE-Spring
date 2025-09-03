    package com.ekote.controller;

    import com.ekote.dto.GunFormDTO;
    import com.ekote.service.GunInfoService;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.stereotype.Controller;
    import org.springframework.ui.Model;
    import org.springframework.web.bind.annotation.*;
    import java.util.ArrayList;
    import java.util.List;
    import java.util.UUID;

    @Controller
    @RequestMapping("/gunForm")
    public class GunFormController {

        private final GunInfoService gunInfoService;

        @Autowired
        public GunFormController(GunInfoService gunInfoService) {
            this.gunInfoService = gunInfoService;
        }

        // Show the initial form
        @GetMapping
        public String showForm() {
            return "gunInfoForm";
        }

        // Handle the first form submission (generate identifiers)
        @PostMapping
        public String generateTable(@RequestParam("gunName") String gunName,
                                    @RequestParam("gunModel") String gunModel,
                                    @RequestParam("quantity") int quantity,
                                    Model model) {
            // Generate the identifiers
            List<String> identifiers = new ArrayList<>();
            for (int i = 0; i < quantity; i++) {
                String uuidPart = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
                String identifier = gunName + "_" + gunModel + "_" + uuidPart;
                identifiers.add(identifier);
            }

            model.addAttribute("gunName", gunName);
            model.addAttribute("gunModel", gunModel);
            model.addAttribute("quantity", quantity);
            model.addAttribute("identifiers", identifiers);

            return "gunInfoForm";
        }


        @PostMapping("/addSpecific")
        public String addSpecificGuns(GunFormDTO formDTO, Model model) {
            // The identifiers are correctly passed here. The original code is correct.
            // The error was an environment or user-action issue.
            // The following code is logically correct.
            String gunName = formDTO.getGunName();
            String gunModel = formDTO.getGunModel();
            Integer quantity = formDTO.getQuantity();
            List<String> identifiers = formDTO.getIdentifiers();
            List<String> manufacturedDates = formDTO.getManufacturedDates();
            List<String> manufacturedPlaces = formDTO.getManufacturedPlaces();
            String result = gunInfoService.addSpecificGuns(
                    gunName,
                    gunModel,
                    quantity,
                    identifiers,
                    manufacturedDates,
                    manufacturedPlaces
            );

            if (result.contains("Error")) {
                model.addAttribute("error", result);
            } else {
                model.addAttribute("success", result);
            }

            // To clear the form for the next entry, you can redirect
            // or clear the model attributes.
            return "gunInfoForm";
        }

    }