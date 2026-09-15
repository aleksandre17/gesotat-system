package org.base.mobile.strategy;

import org.base.mobile.params.LicenseSankeyParams;
import org.base.mobile.params.QueryParams;
import org.base.mobile.params.TradeParams;
import org.base.core.service.QueryBuilder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("othersImpExpStrategy")
public class OthersImpExpStrategy implements TableQueryStrategy<TradeParams> {
    private static final String TABLE_NAME = "others_imp_exp";

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public void configureQuery(QueryBuilder query, QueryParams<TradeParams> params) {
        TradeParams p = params.getParams();
        query.append(" FROM ").append(TABLE_NAME).append(" t WHERE t.year != 2026")
                .addFilter("e_i", p.getEI(), "?");
    }

    @Override
    public List<String> getAttributes() {
        return List.of("year", "e_i");
    }

    @Override
    public List<String> getGroupBy() {
        return List.of("year", "e_i");
    }

    @Override
    public boolean isCombinedQuery() {
        return false;
    }

    @Override
    public TableQueryStrategy<TradeParams> setTableName(String tableName) {
        //this.tableName = tableName;
        return this;
    }
}
