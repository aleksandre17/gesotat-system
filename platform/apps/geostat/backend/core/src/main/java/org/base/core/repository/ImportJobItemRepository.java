package org.base.core.repository;

import org.base.core.entity.data.ImportJobItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImportJobItemRepository extends JpaRepository<ImportJobItem, Long> {
    List<ImportJobItem> findByImportJobIdOrderById(Long importJobId);
}
