package org.base.api.service.mobile_services.strategy;

import org.base.api.service.mobile_services.params.QueryParams;
import org.base.api.service.mobile_services.params.LicenseAgeParams;
import org.base.core.model.ClassificationTableType;
import org.base.core.service.QueryBuilder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Strategy for license-age endpoint query.
 */
@Component("licenseAgeStrategy")
public class LicenseAgeQueryStrategy implements TableQueryStrategy<LicenseAgeParams> {
    private  String tableName;

    public LicenseAgeQueryStrategy() {}

    @Override
    public String getTableName() {
        return tableName;
    }

    @Override
    public void configureQuery(QueryBuilder query, QueryParams<LicenseAgeParams> params) {
        String langName = params.getLangName();
        ClassificationTableType cl = ClassificationTableType.fromFilter("LICENSE_AGE");

        query.append("SELECT SUM(t.quantity) AS quantity, t.year, t.age, ")
                .append("lac.name_").append(langName).append(" AS age_name, lac.id AS age_id ")
                .append("FROM ").append(tableName).append(" t ")
                .append("LEFT JOIN "+cl.getTableName()+" lac ON t.age = lac.id ")
                .append("WHERE t.type = " +(params.getParams().getTable() ? "1":"2"))
                .addGroupBy("t.year", "t.age", "lac.name_" + langName, "lac.id")
                .addOrderBy("t.year", "ASC");
    }

    @Override
    public List<String> getAttributes() {
        return List.of("year", "age");
    }

    @Override
    public List<String> getGroupBy() {
        return List.of("year", "age");
    }

    @Override
    public boolean isCombinedQuery() {
        return false;
    }

    @Override
    public TableQueryStrategy<LicenseAgeParams> setTableName(String tableName) {
        this.tableName = tableName;
        return this;
    }
}
