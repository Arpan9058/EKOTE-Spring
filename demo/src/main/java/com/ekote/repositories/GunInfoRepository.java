package com.ekote.repositories;

import com.ekote.dto.RecordBookDTO;
import com.ekote.entities.GunInfo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GunInfoRepository extends JpaRepository<GunInfo, String> {
  Optional<GunInfo> findByUniqueIdentifier(String uniqueIdentifier);

  boolean existsByUniqueIdentifier(String uniqueIdentifier);

  long countByNextMaintenanceDateLessThanEqual(java.time.LocalDate date);

  org.springframework.data.domain.Page<GunInfo> findByNextMaintenanceDateLessThanEqual(
      java.time.LocalDate date, org.springframework.data.domain.Pageable page);

  @Query(
      "SELECT new com.ekote.dto.RecordBookDTO("
          + "gi.uniqueIdentifier, g.gunName, g.model, g.registeredBy, "
          + "gi.manufacturedDate, gi.manufacturedPlace, gi.maintenanceDate, gi.nextMaintenanceDate, "
          + "i.issueDate, i.returnDate, i.status, "
          + "r.name, r.mail, r.role) "
          + "FROM User r "
          + "JOIN Issue i ON r.id = i.userId "
          + "JOIN Gun g ON g.id = i.gunId "
          + "JOIN GunInfo gi ON gi.gun = g AND gi.uniqueIdentifier = i.uniqueIdentifier "
          + "WHERE gi.uniqueIdentifier = :uniqueIdentifier "
          + "AND i.issueDate = (SELECT MAX(i2.issueDate) FROM Issue i2 WHERE i2.uniqueIdentifier = :uniqueIdentifier)")
  Optional<RecordBookDTO> getGunDetailsByUniqueIdentifier(
      @Param("uniqueIdentifier") String uniqueIdentifier);

  @Query(
      "SELECT new com.ekote.dto.RecordBookDTO("
          + "gi.uniqueIdentifier, g.gunName, g.model, g.registeredBy, "
          + "gi.manufacturedDate, gi.manufacturedPlace, gi.maintenanceDate, gi.nextMaintenanceDate, "
          + "i.issueDate, i.returnDate, i.status, "
          + "r.name, r.mail, r.role) "
          + "FROM User r "
          + "JOIN Issue i ON r.id = i.userId "
          + "JOIN Gun g ON g.id = i.gunId "
          + "JOIN GunInfo gi ON gi.gun = g AND gi.uniqueIdentifier = i.uniqueIdentifier "
          + "WHERE gi.uniqueIdentifier = :uniqueIdentifier")
  List<RecordBookDTO> getPastRecordsByUniqueIdentifier(
      @Param("uniqueIdentifier") String uniqueIdentifier);
}
