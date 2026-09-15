package org.base.mobile.strategy;

import org.base.mobile.repository.GlobalRepository;
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
                return List.of(Integer.parseInt(convertQuarterToFloat(quarter)));
            } catch (NumberFormatException e) {
                return ALL_QUARTERS;
            }
        }
        return null;
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
        // Not used for main_auto in getRace
    }

    @Override
    public TableSelectionStrategy setTableName(String tableName) {
        this.tableName = tableName;
        return this;
    }
}
