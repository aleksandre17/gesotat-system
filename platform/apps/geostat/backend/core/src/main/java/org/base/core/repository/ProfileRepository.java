package org.base.core.repository;

import org.base.core.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProfileRepository extends JpaRepository<Profile, Long> {
    Optional<Profile> findByDisplayName(String displayName);
    Optional<Profile> findByUserId(Long userId);
}
