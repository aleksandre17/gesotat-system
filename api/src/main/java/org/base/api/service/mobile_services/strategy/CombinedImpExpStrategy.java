package org.base.api.service.mobile_services.strategy;

import org.base.api.service.mobile_services.params.ColorsParams;
import org.base.api.service.mobile_services.params.QueryParams;
import org.base.api.service.mobile_services.params.TradeParams;
import org.base.core.service.QueryBuilder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("combinedImpExpStrategy")
public class CombinedImpExpStrategy implements TableQueryStrategy<TradeParams> {
    private static final String TABLE_NAME = "vehicle_imp_exp"; // Primary table for minYear

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public void configureQuery(QueryBuilder query, QueryParams<TradeParams> params) {
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

    @Override
    public TableQueryStrategy<TradeParams> setTableName(String tableName) {
        //this.tableName = tableName;
        return this;
    }
}
