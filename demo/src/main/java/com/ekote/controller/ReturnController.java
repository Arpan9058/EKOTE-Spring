package com.ekote.controller;

import com.ekote.service.ReturnService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
//@RequestMapping("/guns")
public class ReturnController {
    private final ReturnService returnService;

    public ReturnController(ReturnService returnService) {
        this.returnService = returnService;
    }

    @PostMapping("/return")
    public ResponseEntity<String> returnGun(@RequestParam String userId,
                                            @RequestParam String uniqueIdentifier) {
        String result = returnService.returnGun(userId, uniqueIdentifier);
        if (result.contains("successfully")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }
}
