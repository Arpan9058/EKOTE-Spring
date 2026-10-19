package com.ekote.repositories;

import com.ekote.dto.GunDetailsDTO;
import com.ekote.entities.GunInfo;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Read-only report queries over the canonical inventory and issue tables. */
public interface GunDetailsRepository extends Repository<GunInfo, String> {
  @Query(
      "SELECT new com.ekote.dto.GunDetailsDTO("
          + "gi.uniqueIdentifier, g.gunName, g.model, g.quantity, g.registeredBy, "
          + "gi.maintenanceDate, gi.nextMaintenanceDate, i.issueDate, i.returnDate, i.status, "
          + "r.name, r.mail, r.role) "
          + "FROM User r JOIN Issue i ON r.id = i.userId "
          + "JOIN Gun g ON g.id = i.gunId "
          + "JOIN GunInfo gi ON gi.gun = g AND gi.uniqueIdentifier = i.uniqueIdentifier "
          + "WHERE gi.uniqueIdentifier = :uniqueIdentifier ORDER BY i.issueDate DESC, i.id DESC")
  List<GunDetailsDTO> findGunDetails(@Param("uniqueIdentifier") String uniqueIdentifier);
}
