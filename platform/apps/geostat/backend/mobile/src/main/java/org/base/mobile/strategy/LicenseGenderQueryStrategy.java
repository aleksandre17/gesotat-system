package org.base.mobile.strategy;

import org.base.mobile.params.LicenseDualParams;
import org.base.mobile.params.QueryParams;
import org.base.mobile.params.LicenseGenderParams;
import org.base.core.model.ClassificationTableType;
import org.base.core.service.QueryBuilder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Strategy for license-gender endpoint query.
 */
@Component("licenseGenderStrategy")
public class LicenseGenderQueryStrategy implements TableQueryStrategy<LicenseGenderParams> {
    private  String tableName;

    public LicenseGenderQueryStrategy() {
        //this.tableName = tableName;
    }

    @Override
    public String getTableName() {
        return tableName;
    }

    @Override
    public void configureQuery(QueryBuilder query, QueryParams<LicenseGenderParams> params) {
        String langName = params.getLangName();

        ClassificationTableType cl = ClassificationTableType.fromFilter("gender");

        query.append("SELECT SUM(t.quantity) AS quantity, t.year, t.gender, ")
                .append("gc.name_").append(langName).append(" AS gender_name, gc.id AS gender_id ")
                .append("FROM ").append(tableName).append(" t ")
                .append("LEFT JOIN "+cl.getTableName()+" gc ON t.gender = gc.id ")
                .append("WHERE t.type = " +(params.getParams().getTable() ? "1":"2"))
                .addGroupBy("t.year", "t.gender", "gc.name_" + langName, "gc.id")
                .addOrderBy("t.year", "ASC");
    }

    @Override
    public List<String> getAttributes() {
        return List.of("year", "gender");
    }

    @Override
    public List<String> getGroupBy() {
        return List.of("year", "gender");
    }

    @Override
    public boolean isCombinedQuery() {
        return false;
    }

    @Override
    public TableQueryStrategy<LicenseGenderParams> setTableName(String tableName) {
        this.tableName = tableName;
        return this;
    }
}
