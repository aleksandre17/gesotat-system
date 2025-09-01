package org.base.api.repository.mobile.mysql;

import org.base.api.entity.mobile.mysql.Prices;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PricesRepository extends JpaRepository<Prices, Integer> {
}