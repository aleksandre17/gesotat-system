package org.base.api.service.mobile_services.strategy;

import org.base.api.service.mobile_services.params.FuelQuantityParams;
import org.base.api.service.mobile_services.params.QueryParams;
import org.base.api.service.mobile_services.params.LicenseDualParams;
import org.base.core.service.QueryBuilder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Strategy for license-dual endpoint queries.
 */
@Component("licenseDualStrategy")
public class LicenseDualQueryStrategy implements TableQueryStrategy<LicenseDualParams> {
    private static final String MAIN_TABLE = "[dbo].[licenses]";
    private static final String EOY_TABLE = "[dbo].[licenses]";
    private String tableName;
    public LicenseDualQueryStrategy() {
        // Default constructor
    }

    @Override
    public String getTableName() {
        return MAIN_TABLE;
    }

    @Override
    public void configureQuery(QueryBuilder query, QueryParams<LicenseDualParams> params) {
        // Main table query
        query.appendSubQuery("mainQuery", qb -> qb
                .append("SELECT SUM(quantity) AS data2, year AS name ")
                .append("FROM ").append(tableName)
                .append(" WHERE year != 2026 AND ")
                .append("type = 1")
                .addGroupBy("year")
                .addOrderBy("year", "ASC"));

        // EOY table query
        query.appendSubQuery("eoyQuery", qb -> qb
                .append("SELECT SUM(quantity) AS data1, year AS name ")
                .append("FROM ").append(tableName)
                .append(" WHERE year != 2026 AND ")
                .append("type = 2")
                .addGroupBy("year")
                .addOrderBy("year", "ASC"));
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
    public TableQueryStrategy<LicenseDualParams> setTableName(String tableName) {
        this.tableName = tableName;
        return this;
    }
}

