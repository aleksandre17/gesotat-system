package org.base.api.repository.mysql;

import org.base.api.entity.mysql.AgricultureData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AgricultureDataRepository extends JpaRepository<AgricultureData, Long> {
    // Spring Data JPA will automatically create all the standard database methods for you.
    // You can add custom query methods here later if you need them.
}