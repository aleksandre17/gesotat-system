package org.base.core.repository;

import org.base.core.entity.data.ImportTableMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImportTableMappingRepository extends JpaRepository<ImportTableMapping, Long> {
    List<ImportTableMapping> findByProfileIdAndEnabledTrue(Long profileId);
}
