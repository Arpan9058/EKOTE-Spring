package com.ekote.service;

import com.ekote.dto.GunDetailsDTO;
import com.ekote.repositories.GunDetailsRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GunDetailsService {
  private final GunDetailsRepository repository;

  public GunDetailsService(GunDetailsRepository repository) {
    this.repository = repository;
  }

  public List<GunDetailsDTO> getGunDetails(String uniqueIdentifier) {
    return repository.findGunDetails(uniqueIdentifier);
  }
}
