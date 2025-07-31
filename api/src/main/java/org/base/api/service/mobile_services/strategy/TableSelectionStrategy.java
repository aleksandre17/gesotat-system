package org.base.api.service.mobile_services.strategy;

import org.base.api.repository.mobile.GlobalRepository;
import org.base.core.service.QueryBuilder;

import java.util.List;
import java.util.Optional;

public interface TableSelectionStrategy {
    String getTableName();
    Optional<Integer> determineYear(GlobalRepository repository, Integer year);
    List<Integer> determineQuarters(String quarter);
    void configureRaceQuery(QueryBuilder query);
    TableSelectionStrategy setTableName(String tableName);
}
