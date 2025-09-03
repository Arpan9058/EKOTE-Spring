package com.ekote.repositories;

import com.ekote.entities.Issue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IssueRepository extends JpaRepository<Issue, Integer> {
    boolean existsByGunIdAndUniqueIdentifierAndStatusAndReturnDateIsNull(int gunId, String uniqueIdentifier, String status);
    @Query("SELECT i FROM Issue i WHERE i.userId = :userId AND i.uniqueIdentifier = :uniqueIdentifier " +
            "AND i.status = 'issued' ORDER BY i.issueDate DESC LIMIT 1")
    Optional<Issue> findLatestIssuedGun(String userId, String uniqueIdentifier);
}
