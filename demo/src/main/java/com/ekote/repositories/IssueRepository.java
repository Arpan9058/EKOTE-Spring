package com.ekote.repositories;

import com.ekote.entities.Issue;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface IssueRepository extends JpaRepository<Issue, Integer> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select i from Issue i where i.gunId=:gunId and i.uniqueIdentifier=:identifier and i.status='issued' and i.returnDate is null order by i.id desc limit 1")
  Optional<Issue> findActiveForUpdate(int gunId, String identifier);

  long countByStatusAndReturnDateIsNull(String status);

  boolean existsByGunIdAndUniqueIdentifierAndStatusAndReturnDateIsNull(
      int gunId, String uniqueIdentifier, String status);

  @Query(
      "SELECT i FROM Issue i WHERE i.userId = :userId AND i.uniqueIdentifier = :uniqueIdentifier "
          + "AND i.status = 'issued' ORDER BY i.issueDate DESC LIMIT 1")
  Optional<Issue> findLatestIssuedGun(Long userId, String uniqueIdentifier);
}
