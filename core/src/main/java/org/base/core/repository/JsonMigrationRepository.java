package org.base.core.repository;

import org.base.core.entity.Migration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JsonMigrationRepository extends JpaRepository<Migration, String> {

    Optional<Migration> findByFilename(String filename);
}
