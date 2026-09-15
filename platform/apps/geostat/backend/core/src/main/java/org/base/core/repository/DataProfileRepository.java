package org.base.core.repository;

import org.base.core.entity.data.DataProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DataProfileRepository extends JpaRepository<DataProfile, Long> {
    Optional<DataProfile> findByProfileCodeAndEnabledTrue(String profileCode);
    Optional<DataProfile> findByPageId(Long pageId);
}
