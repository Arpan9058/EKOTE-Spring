package com.ekote.repositories;

import com.ekote.entities.GunDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GunDetailsRepository extends JpaRepository<GunDetails, String> {

    List<GunDetails> findByGunName(String gunName);

    List<GunDetails> findByModel(String model);

    GunDetails findByUniqueIdentifier(String uniqueIdentifier);

    List<GunDetails> findByUserEmail(String userEmail);

    boolean existsByUniqueIdentifier(String uniqueIdentifier);

    List<GunDetails> findByStatus(String status);

    @Query(value = "SELECT r.name as userName, r.mail as userEmail, r.role as userRole, " +
            "g.gun_name as gunName, g.model as model, g.quantity as quantity, g.registered_by as registeredBy, " +
            "gi.maintenance_date as maintenanceDate, gi.next_maintenance_date as nextMaintenanceDate, " +
            "i.issue_date as issueDate, i.return_date as returnDate, i.status as status, " +
            "gi.unique_identifier as gunIdentifier " +
            "FROM User r " +
            "JOIN Issue i ON r.id = i.user_id " +
            "JOIN guns g ON g.id = i.gun_id " +
            "JOIN guns_info gi ON gi.gun_id = g.id " +
            "WHERE gi.unique_identifier = :uniqueIdentifier",
            nativeQuery = true)
    List<Object[]> findGunDetails(@Param("uniqueIdentifier") String uniqueIdentifier);
}
