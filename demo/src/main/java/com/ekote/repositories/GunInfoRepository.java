    package com.ekote.repositories;
    
    import com.ekote.dto.GunDetailsDTO;
    import com.ekote.dto.RecordBookDTO;
    import com.ekote.entities.GunInfo;
    import org.springframework.data.jpa.repository.JpaRepository;
    import org.springframework.data.jpa.repository.Query;
    import org.springframework.data.repository.query.Param;

    import java.util.List;
    import java.util.Optional;
    
    public interface GunInfoRepository extends JpaRepository<GunInfo, Integer> {
        Optional<GunInfo> findByUniqueIdentifier(String uniqueIdentifier);
        boolean existsByUniqueIdentifier(String uniqueIdentifier);
//
        @Query("SELECT new com.ekote.dto.RecordBookDTO(" +
                "gi.uniqueIdentifier, g.gunName, g.model, g.registeredBy, " +
                "gi.manufacturedDate, gi.manufacturedPlace, gi.maintenanceDate, gi.nextMaintenanceDate, " +
                "i.issueDate, i.returnDate, i.status, " +
                "r.name, r.mail, r.role) " +
                "FROM User r " +
                "JOIN Issue i ON r.id = i.userId " +
                "JOIN Gun g ON g.id = i.gunId " +
                "JOIN GunInfo gi ON gi.gun = g AND gi.uniqueIdentifier = i.uniqueIdentifier " +
                "WHERE gi.uniqueIdentifier = :uniqueIdentifier")
        Optional<RecordBookDTO> getGunDetailsByUniqueIdentifier(@Param("uniqueIdentifier") String uniqueIdentifier);

        @Query("SELECT new com.ekote.dto.RecordBookDTO(" +
                "gi.uniqueIdentifier, g.gunName, g.model, g.registeredBy, " +
                "gi.manufacturedDate, gi.manufacturedPlace, gi.maintenanceDate, gi.nextMaintenanceDate, " +
                "i.issueDate, i.returnDate, i.status, " +
                "r.name, r.mail, r.role) " +
                "FROM User r " +
                "JOIN Issue i ON r.id = i.userId " +
                "JOIN Gun g ON g.id = i.gunId " +
                "JOIN GunInfo gi ON gi.gun = g " +
                "WHERE gi.uniqueIdentifier = :uniqueIdentifier")
        List<RecordBookDTO> getPastRecordsByUniqueIdentifier(@Param("uniqueIdentifier") String uniqueIdentifier);
    }
