package com.ekote.repositories;

import com.ekote.entities.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
  @org.springframework.data.jpa.repository.Modifying
  @org.springframework.transaction.annotation.Transactional
  @org.springframework.data.jpa.repository.Query(
      value =
          "INSERT INTO register (id,name,mail,password,role) VALUES (:id,:name,:mail,:password,'USER')",
      nativeQuery = true)
  int insertRegistration(Long id, String name, String mail, String password);

  Optional<User> findByMail(String mail);
}
