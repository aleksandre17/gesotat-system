package org.base.mobile.strategy;

import org.base.mobile.params.QueryParams;
import org.base.mobile.params.TradeParams;
import org.base.mobile.params.TreemapParams;
import org.base.core.service.QueryBuilder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component("vehicleImpExpStrategy")
public class VehicleImpExpStrategy implements TableQueryStrategy<TradeParams> {
    private static final String TABLE_NAME = "vehicle_imp_exp";

    private TradeParams tradeParams;

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public void configureQuery(QueryBuilder query, QueryParams<TradeParams> params) {
        tradeParams = params.getParams();
        TradeParams p = params.getParams();
        query.append(" FROM ").append(TABLE_NAME).append(" t WHERE t.year != 2026")
                .addFilter("e_i", p.getEI(), "?");
        if ("1".equals(p.getType())) {
            query.addFilter("fuel", p.getFuel() != null ? p.getFuel() : 1, "?")
                    .addFilter("vehicle", p.getVehicle() != null ? p.getVehicle() : 1, "?");
        }
    }

    @Override
    public List<String> getAttributes() {
        List<String> attrs = new ArrayList<>(List.of("year", "e_i"));
        if ("1".equals(tradeParams.getType())) {
            attrs.addAll(List.of("fuel", "vehicle"));
        }
        return attrs;
    }

    @Override
    public List<String> getGroupBy() {
        List<String> group = new ArrayList<>(List.of("year", "e_i"));
        if ("1".equals(tradeParams.getType())) {
            group.addAll(List.of("fuel", "vehicle"));
        }
        return group;
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
