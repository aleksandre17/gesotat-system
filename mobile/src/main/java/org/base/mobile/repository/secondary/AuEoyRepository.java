package org.base.mobile.repository.secondary;

import org.base.mobile.entity.AuEoy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for auto_eoes table (secondary "auto" database).
 */
@Repository
public interface AuEoyRepository extends JpaRepository<AuEoy, Long> {

    List<AuEoy> findByYear(Integer year);

    List<AuEoy> findByBrand(String brand);
}

