package org.base.mobile.strategy;

import org.base.mobile.params.AreaQuantityOrCurrencyParams;
import org.base.mobile.params.QueryParams;
import org.base.mobile.params.TopFiveParams;
import org.base.mobile.params.TradeParams;
import org.base.core.model.ClassificationTableType;
import org.base.core.service.QueryBuilder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component("tradeStrategy")
public class TradeQueryStrategy implements TableQueryStrategy<AreaQuantityOrCurrencyParams> {
    //private final String tableName;

    private final String tableName = "[dbo].[vehicles_imp_exp]";
    public int topCountriesLimit;
    private  int yearRange;


    public TradeQueryStrategy() { //String type
        //this.tableName = "2".equals(type) ? "others_imp_exp" : "vehicle_imp_exp";
        this.topCountriesLimit = 5;
        this.yearRange = 8;
    }

    @Override
    public String getTableName() { return tableName; }

    public QueryBuilder configureMinYearQuery(String tableName) {
        QueryBuilder query = new QueryBuilder();
        query.append("SELECT MIN(year) AS minYear FROM " + query.sanitizeColumn(tableName));
        return query;
    }

    @Override
    public void configureQuery(QueryBuilder query, QueryParams<AreaQuantityOrCurrencyParams> params) {
        AreaQuantityOrCurrencyParams p = params.getParams();

        String attribute = sanitizeColumn(p.getAttribute());

        ClassificationTableType clConfig = ClassificationTableType.fromFilter("country");
        // CTE for aggregated data
        query.addCte("agg_data",
                "SELECT t.year, t.country, SUM(t." + attribute + ") AS value, " +
                        "cl." + sanitizeColumn(p.getLangName()) + " AS country_name " +
                        "FROM " + tableName + " t " +
                        "LEFT JOIN " + clConfig.getTableName() + " cl ON t.country = cl.id " +
                        "WHERE t.year != 2026 AND t.e_i = ? " +
                        ("1".equals(p.getType()) ? "AND t.fuel = ? AND t.type = 1 AND t.vehicle = ? " : "AND t.type = 2") +
                        "GROUP BY t.year, t.country, cl." + sanitizeColumn(p.getLangName()));

        // CTE for top countries
        query.addCte("top_countries",
                "SELECT TOP " + topCountriesLimit + " country, country_name " +
                        "FROM agg_data " +
                        "GROUP BY country, country_name " +
                        "ORDER BY SUM(value) DESC");

        // Main query
        query.append("SELECT tc.country_name AS name, ad.year, ad.value " +
                "FROM top_countries tc " +
                "JOIN agg_data ad ON tc.country = ad.country " +
                "ORDER BY ad.year ASC");

        List<String> parameters = new ArrayList<>(List.of(p.getEI()));
        if ("1".equals(p.getType())) {
            parameters.add(p.getFuel());
            parameters.add(p.getVehicle());
        }
        query.mergeParameters((List<Object>) (List<?>)parameters);
    }

    /**
     * WITH top_countries AS (
     *   SELECT t.country, SUM(t.quantity) AS total_value
     *   FROM vehicle_imp_exp t
     *   WHERE t.year != 2026 AND t.e_i = ?
     *   GROUP BY t.country
     *   ORDER BY total_value DESC LIMIT 5
     * )
     * SELECT t.year, t.country, cc.name_en AS country_name, SUM(t.quantity) AS value
     * FROM vehicle_imp_exp t
     * LEFT JOIN country_cl cc ON t.country = cc.id
     * WHERE t.year != 2026 AND t.e_i = ?
     * AND t.country IN (SELECT country FROM top_countries)
     * GROUP BY t.year, t.country, cc.name_en
     * ORDER BY t.year ASC;
     *
     * Determines the minimum year for the trade data.
     * For "vehicle_imp_exp", it uses the minYear from the repository.
     * For "others_imp_exp", it defaults to 2020.
     */
//    @Override
//    public void configureQuery(QueryBuilder query, QueryParams<TradeParams> params) {
//        TradeParams p = params.getParams();
//        String attribute = "1".equals(p.getSelector()) ? "quantity" : p.getCurrencyColumn();
//        String cteQuery = "SELECT t.country, SUM(t." + attribute + ") AS total_value " +
//                "FROM " + tableName + " t WHERE t.year != 2026 AND t.e_i = ? " +
//                ("1".equals(p.getType()) ? "AND t.fuel = ? AND t.vehicle = ? " : "") +
//                "GROUP BY t.country ORDER BY total_value DESC LIMIT 5";
//        query.addCte("top_countries", cteQuery);
//        query.append("SELECT t.year, t.country, cc.").append(params.getLangName()).append(" AS country_name, ")
//                .append("SUM(t.").append(attribute).append(") AS value ")
//                .append("FROM ").append(tableName).append(" t ")
//                .append("LEFT JOIN country_cl cc ON t.country = cc.id ")
//                .append("WHERE t.year != 2026 AND t.e_i = ? ")
//                .append("AND t.country IN (SELECT country FROM top_countries) ");
//        if ("1".equals(p.getType())) {
//            query.append("AND t.fuel = ? AND t.vehicle = ? ");
//        }
//        query.addGroupBy("t.year", "t.country", "cc." + params.getLangName())
//                .addOrderBy("t.year", "ASC");
//        query.getParams().add(p.getEi());
//        if ("1".equals(p.getType())) {
//            query.getParams().add(p.getFuel() != null ? p.getFuel() : 1);
//            query.getParams().add(p.getVehicle() != null ? p.getVehicle() : 1);
//        }
//        query.getParams().add(p.getEi());
//        if ("1".equals(p.getType())) {
//            query.getParams().add(p.getFuel() != null ? p.getFuel() : 1);
//            query.getParams().add(p.getVehicle() != null ? p.getVehicle() : 1);
//        }
//    }

    @Override
    public List<String> getAttributes() {
        List<String> attrs = new ArrayList<>(List.of("year", "e_i", "country"));
//        if ("1".equals(params.type)) {
//            attrs.addAll(List.of("fuel", "vehicle"));
//        }
        return attrs;
    }

    @Override
    public List<String> getGroupBy() {
        List<String> group = new ArrayList<>(List.of("year", "country", "e_i"));
//        if ("1".equals(params.type)) {
//            group.addAll(List.of("fuel", "vehicle"));
//        }
        return group;
    }

    public int getYearRange() {
        return yearRange;
    }

    private String sanitizeColumn(String column) {
        return column.replaceAll("[^a-zA-Z0-9_.\\[\\]]", "");
    }

    private String sanitizeTableName(String tableName) {
        return tableName.replaceAll("[^a-zA-Z0-9_]", "");
    }

    @Override
    public boolean isCombinedQuery() { return false; }

    @Override
    public TableQueryStrategy<AreaQuantityOrCurrencyParams> setTableName(String tableName) {
        //this.tableName = tableName;
        return this;
    }
}
