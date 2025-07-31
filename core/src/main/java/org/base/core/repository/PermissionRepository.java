package org.base.core.repository;

 import org.base.core.entity.Permission;
 import org.springframework.data.jpa.repository.JpaRepository;
 import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Optional findByName(String name);
}
