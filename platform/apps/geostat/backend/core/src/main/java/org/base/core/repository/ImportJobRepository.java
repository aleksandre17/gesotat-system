package org.base.core.repository;

import org.base.core.entity.data.ImportJob;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportJobRepository extends JpaRepository<ImportJob, Long> {
}
