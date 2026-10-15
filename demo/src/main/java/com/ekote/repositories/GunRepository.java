package com.ekote.repositories;

import com.ekote.entities.Gun;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface GunRepository extends JpaRepository<Gun, Integer> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select g from Gun g where g.id=:id")
  Optional<Gun> findForUpdateById(int id);

  @Query("select coalesce(sum(g.quantity), 0) from Gun g")
  long totalAvailable();

  org.springframework.data.domain.Page<Gun> findByQuantityLessThanEqual(
      int threshold, org.springframework.data.domain.Pageable page);

  // Find guns by name
  List<Gun> findByGunName(String gunName);

  // Find guns by model
  List<Gun> findByModel(String model);

  // Find guns by name and model
  Optional<Gun> findByGunNameAndModel(String gunName, String model);
}
