package org.base.core.repository;

import org.base.core.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Auth path: eager-fetch roles + permissions in one query to avoid N+1
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles r LEFT JOIN FETCH r.permissions WHERE u.username = :username")
    Optional<User> findByUsernameWithAuth(@Param("username") String username);

    // Management path: no associations needed, mapped to DTO
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);
}