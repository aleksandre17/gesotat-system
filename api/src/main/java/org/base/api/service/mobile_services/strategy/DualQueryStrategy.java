package org.base.api.service.mobile_services.strategy;

import org.base.api.service.mobile_services.params.CompareParams;
import org.base.api.service.mobile_services.params.QueryParams;
import org.base.api.service.mobile_services.params.DualParams;
import org.base.core.service.QueryBuilder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Strategy for dual endpoint query, optimized for MSSQL.
 */
@Component("dualStrategy")
public class DualQueryStrategy implements TableQueryStrategy<DualParams> {
    private final String tableName;

    public DualQueryStrategy() {
        this.tableName = "[dbo].[vehicles1000]";
    }

    @Override
    public String getTableName() {
        return tableName;
    }

    private Integer parseVType(String vType) {
        try {
            return Integer.parseInt(vType);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid v_type: " + vType);
        }
    }

    @Override
    public void configureQuery(QueryBuilder query, QueryParams<DualParams> params) {
        DualParams p = params.getParams();

        Integer type = params.getParams().getVType() != null ? parseVType(params.getParams().getVType()) : 0;

        query.append("SELECT t.year AS name")
                .addRoundedColumn("vehicles", 1, "data1")
                .addRoundedColumn("vehicles1000", 0, "data2")
                .append(" FROM [dbo].[vehicles1000] t WHERE 1=1")
                .addFilter("t.type", type, "=");
        //query.mergeParameters(List.of(p.getVType()));
    }

    @Override
    public List<String> getAttributes() {
        return List.of("vehicles", "vehicles1000", "year");
    }

    @Override
    public List<String> getGroupBy() {
        return List.of();
    }

    @Override
    public boolean isCombinedQuery() {
        return false;
    }

    private String sanitizeColumn(String column) {
        return column.replaceAll("[^a-zA-Z0-9_.\\[\\]]", "");
    }

    @Override
    public TableQueryStrategy<DualParams> setTableName(String tableName) {
        //this.tableName = tableName;
        return this;
    }
}
