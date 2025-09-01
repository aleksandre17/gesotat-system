package org.base.api.repository.mobile.mysql;

import org.base.api.entity.mobile.mysql.Main;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MainRepository extends JpaRepository<Main, Long> {
}