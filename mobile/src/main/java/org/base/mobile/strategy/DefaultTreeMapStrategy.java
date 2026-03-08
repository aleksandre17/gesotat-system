package org.base.mobile.strategy;

import org.base.mobile.repository.GlobalRepository;
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
            return List.of(Integer.parseInt(convertQuarterToFloat(quarter)));
        } catch (NumberFormatException e) {
            return ALL_QUARTERS;
        }
    }

    public static String convertQuarterToFloat(String quarter) {
        if (quarter == null) {
            return null;
        }
        // Handle Roman numerals
        switch (quarter.trim().toUpperCase()) {
            case "I":
                return "1";
            case "II":
                return "2";
            case "III":
                return "3";
            case "IV":
                return "4";
            default:
                // Try parsing as a numeric string (e.g., '1', '2.0')
                try {
                    return String.valueOf(quarter.trim());
                } catch (NumberFormatException e) {
                    // Return null for invalid values
                    return null;
                }
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
