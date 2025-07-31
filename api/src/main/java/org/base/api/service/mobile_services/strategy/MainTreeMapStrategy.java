package org.base.api.service.mobile_services.strategy;

import org.base.api.repository.mobile.GlobalRepository;
import org.base.core.service.QueryBuilder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component("mainTreeMapStrategy")
public class MainTreeMapStrategy implements TableSelectionStrategy {
    private static final List<Integer> ALL_QUARTERS = List.of(1, 2, 3, 4);
    private static final String TABLE_NAME = "main_tree_map";

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public Optional<Integer> determineYear(GlobalRepository repository, Integer year) {
        return Optional.ofNullable(year);
    }

    @Override
    public List<Integer> determineQuarters(String quarter) {
        if (quarter == null || quarter.equals("99")) {
            return ALL_QUARTERS;
        }
        try {
            return List.of(Integer.parseInt(quarter));
        } catch (NumberFormatException e) {
            return ALL_QUARTERS;
        }
    }

    @Override
    public void configureRaceQuery(QueryBuilder query) {
        query.append("SELECT t.year, t.quarter, t.brand AS name, SUM(t.quantity) AS value ")
                .append("FROM ").append(TABLE_NAME).append(" t ")
                .addGroupBy("t.quarter", "t.year", "t.brand")
                .addOrderBy("t.year", "ASC")
                .addOrderBy("t.quarter", "ASC")
                .addOrderBy("SUM(t.quantity)", "DESC");
    }

    @Override
    public TableSelectionStrategy setTableName(String tableName) {
        return this;
    }
}
