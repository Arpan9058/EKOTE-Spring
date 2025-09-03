//package com.ekote.controller;
//
//import com.ekote.service.GunInfoService;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.HashMap;
//import java.util.Map;
//
//@RestController
//@RequestMapping("/guns-info")
//public class GunInfoController {
//
//    private final GunInfoService gunInfoService;
//
//    public GunInfoController(GunInfoService gunInfoService) {
//        this.gunInfoService = gunInfoService;
//    }
//
//    @PostMapping("/add")
//    public Map<String, String> addGunInfo(
//            @RequestParam String gunName,
//            @RequestParam String gunModel,
//            @RequestParam int quantity,
//            @RequestParam int last,
//            @RequestParam String[] manufacturedDate,
//            @RequestParam String[] manufacturedPlace) {
//
//        String message = gunInfoService.addSpecificGuns(gunName, gunModel, quantity, manufacturedDate, manufacturedPlace);
//
//        Map<String, String> response = new HashMap<>();
//        response.put("status", message.contains("successfully") ? "success" : "error");
//        response.put("message", message);
//        return response;
//    }
//}
