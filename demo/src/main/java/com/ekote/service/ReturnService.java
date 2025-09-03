package com.ekote.service;

import com.ekote.repositories.IssueRepository;
import com.ekote.repositories.GunRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ReturnService {
    private final IssueRepository issueRepository;
    private final GunRepository gunRepository;

    public ReturnService(IssueRepository issueRepository, GunRepository gunRepository) {
        this.issueRepository = issueRepository;
        this.gunRepository = gunRepository;
    }

    public String returnGun(String userId, String uniqueIdentifier) {
        return issueRepository.findLatestIssuedGun(userId, uniqueIdentifier)
                .map(issue -> {
                    // update issue table
                    issue.setReturnDate(LocalDateTime.now());
                    issue.setStatus("returned");
                    issueRepository.save(issue);

                    // update gun quantity
                    gunRepository.findById(issue.getGunId()).ifPresent(gun -> {
                        gun.setQuantity(gun.getQuantity() + 1);
                        gunRepository.save(gun);
                    });

                    return "Gun returned successfully";
                })
                .orElse("Gun not issued or not found");
    }
}
