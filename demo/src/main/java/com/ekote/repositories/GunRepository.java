package com.ekote.repositories;

import com.ekote.entities.Gun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GunRepository extends JpaRepository<Gun, Integer> {

    // Find guns by name
    List<Gun> findByGunName(String gunName);

    // Find guns by model
    List<Gun> findByModel(String model);

    // Find guns by name and model
    Optional<Gun> findByGunNameAndModel(String gunName, String model);


}
