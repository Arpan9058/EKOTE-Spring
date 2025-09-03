package com.ekote.repositories;

import com.ekote.dto.RecordBookDTO;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface RecordBookRepository extends CrudRepository<com.ekote.entities.GunDetails, Integer> {

    @Query("SELECT new com.ekote.dto.RecordBookDTO(" +
            "gi.uniqueIdentifier, g.gunName, g.model, g.registeredBy, " +
            "gi.manufacturedDate, gi.manufacturedPlace, gi.maintenanceDate, gi.nextMaintenanceDate, " +
            "i.issueDate, i.returnDate, i.status, " +
            "r.name, r.mail, r.role) " +
            "FROM User r " +
            "JOIN Issue i ON r.id = i.userId " +
            "JOIN Gun g ON g.id = i.gunId " +
            "JOIN GunInfo gi ON gi.gun = g AND gi.uniqueIdentifier = i.uniqueIdentifier")
    List<RecordBookDTO> getRecordBook();
}
