package org.base.api.service.mobile_services.strategy;

import org.base.api.repository.mobile.GlobalRepository;
import org.base.core.service.QueryBuilder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component("defaultTreeMapStrategy")
public class DefaultTreeMapStrategy implements TableSelectionStrategy {
    private  String tableName;
    private static final List<Integer> ALL_QUARTERS = List.of(1, 2, 3, 4);
    //@Value("${app.defaultTreeMapTable:secondary_tree_map}")
    public DefaultTreeMapStrategy() {
        //this.tableName = tableName;
    }

    @Override
    public String getTableName() {
        return tableName;
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
        query.append("SELECT t.year, t.brand AS name, SUM(t.quantity) AS value ")
                .append("FROM ").append(tableName).append(" t ")
                .addGroupBy("t.year", "t.brand")
                .addOrderBy("t.year", "ASC")
                .addOrderBy("SUM(t.quantity)", "DESC");
    }

    @Override
    public TableSelectionStrategy setTableName(String tableName) {
        return this;
    }

}
