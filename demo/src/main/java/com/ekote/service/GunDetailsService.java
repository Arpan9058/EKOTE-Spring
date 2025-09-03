package com.ekote.service;

import com.ekote.dto.GunDetailsDTO;
import com.ekote.repositories.GunDetailsRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GunDetailsService {
    private final GunDetailsRepository repository;

    public GunDetailsService(GunDetailsRepository repository) {
        this.repository = repository;
    }

    public List<GunDetailsDTO> getGunDetails(String uniqueIdentifier) {
        List<Object[]> results = repository.findGunDetails(uniqueIdentifier);
        return results.stream().map(row -> {
            GunDetailsDTO dto = new GunDetailsDTO();
            dto.setUserName((String) row[0]);
            dto.setUserEmail((String) row[1]);
            dto.setUserRole((String) row[2]);
            dto.setGunName((String) row[3]);
            dto.setModel((String) row[4]);
            dto.setQuantity((Integer) row[5]);
            dto.setRegisteredBy((String) row[6]);
            dto.setMaintenanceDate((String) row[7]);
            dto.setNextMaintenanceDate((String) row[8]);
            dto.setIssueDate(row[9] != null ? row[9].toString() : null);
            dto.setReturnDate(row[10] != null ? row[10].toString() : null);
            dto.setStatus((String) row[11]);
            dto.setGunIdentifier((String) row[12]);
            return dto;
        }).collect(Collectors.toList());
    }
}
