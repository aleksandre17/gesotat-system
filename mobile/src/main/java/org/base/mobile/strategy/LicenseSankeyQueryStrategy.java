package org.base.mobile.strategy;

import org.base.mobile.params.LicenseGenderParams;
import org.base.mobile.params.QueryParams;
import org.base.mobile.params.LicenseSankeyParams;
import org.base.core.service.QueryBuilder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Strategy for license-sankey endpoint queries.
 */
@Component("licenseSankeyStrategy")
public class LicenseSankeyQueryStrategy implements TableQueryStrategy<LicenseSankeyParams> {
    private  String tableName;

    public LicenseSankeyQueryStrategy() {
        //this.tableName = tableName;
    }

    @Override
    public String getTableName() {
        return tableName;
    }

    @Override
    public void configureQuery(QueryBuilder query, QueryParams<LicenseSankeyParams> params) {
        LicenseSankeyParams p = params.getParams();
        String langName = params.getLangName();
        // Gender query
        query.appendSubQuery("genderAgeQuery", qb -> qb
            .append("SELECT ")
            .append("t.gender, ")
            .append("CASE WHEN GROUPING(t.year) = 1 THEN NULL ELSE t.year END AS year, ")
            .append("SUM(t.quantity) AS total_quantity, ")
            .append("gc.name_").append(langName).append(" AS gender_name, ")
            .append("gc.id AS gender_id, ")
            .append("CASE WHEN GROUPING(t.year) = 1 THEN NULL ELSE lac.name_").append(langName).append(" END AS age_name, ")
            .append("CASE WHEN GROUPING(t.year) = 1 THEN NULL ELSE lac.id END AS age_id, ")
            .append("CASE ")
            .append(" WHEN t.gender = 1 AND GROUPING(t.year) = 1 THEN 2 ")
            .append(" WHEN t.gender = 1 AND GROUPING(t.year) = 0 THEN 3 ")
            .append(" WHEN t.gender = 2 AND GROUPING(t.year) = 1 THEN 1 ")
            .append(" WHEN t.gender = 2 AND GROUPING(t.year) = 0 THEN 2 ")
            .append(" WHEN t.gender = 3 AND GROUPING(t.year) = 1 THEN 6 ")
            .append(" WHEN t.gender = 3 AND GROUPING(t.year) = 0 THEN 7 ")
            .append(" ELSE 5 ")
            .append("END AS sort_order ")
            .append("FROM [dbo].[licenses] t ")
            .append("LEFT JOIN [CL].[cl_gender] gc ON t.gender = gc.id ")
            .append("LEFT JOIN [CL].[cl_licenses_age] lac ON t.age = lac.id ")
            .append("WHERE 1=1 ")
            .addFilter("t.year", p.getYear(), "=")
            .addFilter("t.type", (params.getParams().getTable() ? "1":"2"), "=")
            .addFilter(new QueryBuilder.InFilter("t.gender", List.of(1, 2, 3), "IN")) // Updated to include 3
            .append("GROUP BY GROUPING SETS ( ")
            .append(" (t.gender, gc.name_").append(langName).append(", gc.id), ")
            .append(" (t.gender, t.year, t.age, gc.name_").append(langName).append(", gc.id, lac.name_").append(langName).append(", lac.id) ")
            .append(") ")
            .append("ORDER BY sort_order, gender desc, year, age_id;\n") //append("ORDER BY gender desc, sort_order, year, age_id;\n")
        );

    }

    @Override
    public List<String> getAttributes() {
        return List.of("gender", "age");
    }

    @Override
    public List<String> getGroupBy() {
        return List.of("gender", "age");
    }

    @Override
    public boolean isCombinedQuery() {
        return true;
    }

    @Override
    public TableQueryStrategy<LicenseSankeyParams> setTableName(String tableName) {
        this.tableName = tableName;
        return this;
    }
}
