package com.ekote.controller;

import com.ekote.entities.*;
import com.ekote.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;

@Controller
@RequestMapping("/issue")
public class IssueController {

    private final UserRepository userRepository;
    private final GunRepository gunRepository;

    @Autowired
    private GunInfoRepository gunInfoRepository;
    private final IssueRepository issueRepository;

    public IssueController(UserRepository userRepository,
                           GunRepository gunRepository,
                           GunInfoRepository gunInfoRepository,
                           IssueRepository issueRepository) {
        this.userRepository = userRepository;
        this.gunRepository = gunRepository;
        this.gunInfoRepository = gunInfoRepository;
        this.issueRepository = issueRepository;
    }

    @GetMapping("/findUser")
    public String findUser(@RequestParam("user_id") Long userId, Model model) {
        User user = userRepository.findById(userId).orElse(null);

        if (user == null) {
            model.addAttribute("userNotFound", true);  // Show error message in the view
        } else {
            model.addAttribute("user", user);  // Add user details to the model
        }

        return "issueReturn";  // Return to the same view with user details or error
    }

    // ✅ Issue a gun
    @PostMapping
    public ResponseEntity<String> issueGun(
            @RequestParam Long userId,
            @RequestParam String uniqueIdentifier) {

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body("❌ User not found.");
        }

        Optional<GunInfo> optionalGunInfo = gunInfoRepository.findByUniqueIdentifier(uniqueIdentifier);

        if (optionalGunInfo.isEmpty()) {
            return ResponseEntity.badRequest().body("❌ Gun not found.");
        }

        GunInfo gunInfo = optionalGunInfo.get();

        String gunInfoId = gunInfo.getUniqueIdentifier();
        int gunId = gunInfo.getGun().getId();

        Gun gun = gunRepository.findById(gunId).orElse(null);
        if (gun == null) {
            return ResponseEntity.badRequest().body("❌ Gun model not found in inventory.");
        }

        boolean alreadyIssued = issueRepository.existsByGunIdAndUniqueIdentifierAndStatusAndReturnDateIsNull(
                gun.getId(), uniqueIdentifier, "issued");
        if (alreadyIssued) {
            return ResponseEntity.badRequest().body("⚠️ Gun is already issued.");
        }

        if (gun.getQuantity() <= 0) {
            return ResponseEntity.badRequest().body("⚠️ Gun not available in inventory.");
        }

        gun.setQuantity(gun.getQuantity() - 1);
        gunRepository.save(gun);

        Issue issue = new Issue();
        issue.setUserId(userId);
        issue.setGunId(gun.getId());
        issue.setUniqueIdentifier(uniqueIdentifier);
        issue.setIssueDate(LocalDateTime.now());
        issue.setStatus("issued");
        issueRepository.save(issue);

        return ResponseEntity.ok("✅ Gun issued successfully.");
    }
}
