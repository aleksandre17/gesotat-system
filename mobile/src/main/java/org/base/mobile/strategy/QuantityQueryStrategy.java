package org.base.mobile.strategy;

import org.base.mobile.params.AreaQuantityOrCurrencyParams;
import org.base.mobile.params.QueryParams;
import org.base.mobile.params.TradeParams;
import org.base.core.service.QueryBuilder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Strategy for quantity endpoint query, optimized for MSSQL.
 */
@Component("quantityStrategy")
public class QuantityQueryStrategy implements TableQueryStrategy<AreaQuantityOrCurrencyParams> {
    private final String vehicleTableName = "vehicle_imp_exp";
    private final String othersTableName = "others_imp_exp";

    @Override
    public String getTableName() {
        return vehicleTableName; // Default table
    }

    public QueryBuilder configureMinYearQuery(String tableName) {
        QueryBuilder query = new QueryBuilder();
        query.append("SELECT MIN(year) AS minYear FROM " + sanitizeTableName(tableName));
        return query;
    }

    @Override
    public void configureQuery(QueryBuilder query, QueryParams<AreaQuantityOrCurrencyParams> params) {
        AreaQuantityOrCurrencyParams p = params.getParams();
        String tableName = "1".equals(p.getType()) || !"2".equals(p.getType()) ? vehicleTableName : othersTableName;
        boolean isCombined = !"1".equals(p.getType()) && !"2".equals(p.getType());

        if (isCombined) {
            // Combined query for vehicle_imp_exp and others_imp_exp
            query.append("SELECT year, SUM(quantity) AS quantity " +
                    "FROM ( " +
                    "SELECT year, SUM(quantity) AS quantity " +
                    "FROM " + vehicleTableName + " " +
                    "WHERE year != 2026 AND e_i = ? " +
                    "GROUP BY year " +
                    "UNION ALL " +
                    "SELECT year, SUM(quantity) AS quantity " +
                    "FROM " + othersTableName + " " +
                    "WHERE year != 2026 AND e_i = ? " +
                    "GROUP BY year " +
                    ") combined " +
                    "GROUP BY year " +
                    "ORDER BY year ASC");
            query.mergeParameters(List.of(p.getEI(), p.getEI()));
        } else {
            // Single table query
            query.append("SELECT year, SUM(quantity) AS quantity ");
            if ("1".equals(p.getType())) {
                query.append(", e_i, fuel, vehicle ");
            } else {
                query.append(", e_i ");
            }
            query.append("FROM " + tableName + " " +
                    "WHERE year != 2026 AND e_i = ? ");
            List<String> parameters = new ArrayList<>(List.of(p.getEI()));
            if ("1".equals(p.getType())) {
                query.append("AND fuel = ? AND vehicle = ? ");
                parameters.add(p.getFuel());
                parameters.add(p.getVehicle());
            }
            query.append("GROUP BY year");
            if ("1".equals(p.getType())) {
                query.append(", e_i, fuel, vehicle");
            } else {
                query.append(", e_i");
            }
            query.append(" ORDER BY year ASC");
            query.mergeParameters((List<Object>) (List<?>) parameters);
        }
    }

    @Override
    public List<String> getAttributes() {
        return List.of("year");
    }

    @Override
    public List<String> getGroupBy() {
        return List.of("year");
    }

    @Override
    public boolean isCombinedQuery() {
        return true;
    }

    private String sanitizeColumn(String column) {
        return column.replaceAll("[^a-zA-Z0-9_.\\[\\]]", "");
    }

    private String sanitizeTableName(String tableName) {
        return tableName.replaceAll("[^a-zA-Z0-9_]", "");
    }

    @Override
    public TableQueryStrategy<AreaQuantityOrCurrencyParams> setTableName(String tableName) {
        //this.tableName = tableName;
        return this;
    }
}
