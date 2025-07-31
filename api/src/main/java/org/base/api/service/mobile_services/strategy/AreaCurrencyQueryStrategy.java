package org.base.api.service.mobile_services.strategy;

import org.base.api.service.mobile_services.params.QueryParams;
import org.base.api.service.mobile_services.params.AreaQuantityOrCurrencyParams;
import org.base.core.service.QueryBuilder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Strategy for area-currency endpoint query, optimized for MSSQL.
 */
@Component("areaCurrencyStrategy")
public class AreaCurrencyQueryStrategy implements TableQueryStrategy<AreaQuantityOrCurrencyParams> { //insted CombinedImpExpStrategy
    private final String vehicleTableName = "[dbo].[vehicles_imp_exp]"; // Default table name

    @Override
    public String getTableName() {
        return vehicleTableName; // Default table
    }

    public QueryBuilder configureMinYearQuery(String tableName) {
        QueryBuilder query = new QueryBuilder();
        query.append("SELECT MIN(year) AS minYear FROM " + query.sanitizeColumn(tableName));
        return query;
    }

//    @Override
//    public void configureQuery(QueryBuilder query, QueryParams<TradeParams> params) {
//        TradeParams p = params.getParams();
//        String column = p.getCurrencyColumn() != null ? p.getCurrencyColumn() : "quantity";
//        query.append("SELECT year, SUM(").append(column).append(") AS ").append(column).append(" FROM (")
//                .append("SELECT year, ").append(column).append(" AS ").append(column)
//                .append(" FROM vehicle_imp_exp WHERE e_i = ? AND year != 2026");
//        query.addUnionQuery(
//                "SELECT year, " + column + " AS " + column + " FROM others_imp_exp WHERE e_i = ? AND year != 2026",
//                p.getEi()
//        );
//        query.append(") t")
//                .addGroupBy("year")
//                .addOrderBy("year", "ASC");
//        query.getParams().add(p.getEi());
//    }

    @Override
    public void configureQuery(QueryBuilder query, QueryParams<AreaQuantityOrCurrencyParams> params) {
        AreaQuantityOrCurrencyParams p = params.getParams();

        String tableName = vehicleTableName;
        boolean isCombined = !"1".equals(p.getType()) && !"2".equals(p.getType());

        String selectedColumn = Objects.equals(p.getSelected(), "currency") ? p.getCurrencyName() : p.getSelected();

        if (isCombined) {
            // Combined query for vehicle_imp_exp and others_imp_exp
            query.append("SELECT year, SUM(currency) AS currency " +
                    "FROM ( " +
                    "SELECT year, SUM(" + selectedColumn + ") AS currency " +
                    "FROM " + vehicleTableName + " " +
                    "WHERE year != 2026 AND e_i = ? " +
                    "AND type = 1 " +
                    "GROUP BY year " +
                    "UNION ALL " +
                    "SELECT year, SUM(" + selectedColumn + ") AS currency " +
                    "FROM " + vehicleTableName + " " +
                    "WHERE year != 2026 AND e_i = ? " +
                    "AND type = 2 " +
                    "GROUP BY year " +
                    ") combined " +
                    "GROUP BY year " +
                    "ORDER BY year ASC");
            query.mergeParameters(List.of(p.getEI(), p.getEI()));
        } else {
            // Single table query
            query.append("SELECT year, SUM(" + selectedColumn + ") AS currency ");
            if ("1".equals(p.getType())) {
                query.append(", e_i, fuel, vehicle ");
            } else {
                query.append(", e_i ");
            }
            query.append("FROM " + tableName + " " +
                    "WHERE year != 2026 AND e_i = ? AND type = ").append(p.getType());
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
            query.mergeParameters((List<Object>) (List<?>)parameters);
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


    @Override
    public TableQueryStrategy<AreaQuantityOrCurrencyParams> setTableName(String tableName) {
        //this.tableName = tableName;
        return this;
    }
}
