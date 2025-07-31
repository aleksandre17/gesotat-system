package org.base.api.service.mobile_services.strategy;

import org.base.api.repository.mobile.GlobalRepository;
import org.base.core.service.QueryBuilder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;



@Component("eoyesTabelStrategy")
public class EoyesTableStrategy implements TableSelectionStrategy {
    private static final List<Integer> ALL_QUARTERS = List.of(1, 2, 3, 4);
    private static final String TABLE_NAME = "[dbo].[auto_main]";
    private String tableName;

    @Override
    public String getTableName() {
        return tableName;
    }

    @Override
    public Optional<Integer> determineYear(GlobalRepository repository, Integer year) {
        if (year != null) {
            return Optional.of(year);
        }
        return repository.getMaxYear(TABLE_NAME);
    }

    @Override
    public List<Integer> determineQuarters(String quarter) {
        if (getTableName().equals(TABLE_NAME)) {
            if (quarter == null || quarter.equals("99")) {
                return ALL_QUARTERS;
            }
            try {
                return List.of(Integer.parseInt(quarter));
            } catch (NumberFormatException e) {
                return ALL_QUARTERS;
            }
        }
        return null;
    }

    @Override
    public void configureRaceQuery(QueryBuilder query) {
        // Not used for main_auto in getRace
    }

    @Override
    public TableSelectionStrategy setTableName(String tableName) {
        this.tableName = tableName;
        return this;
    }
}
