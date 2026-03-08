package org.base.mobile.strategy.text;

import org.base.mobile.TableConfigText;
import org.base.mobile.params.QueryParams;
import org.base.mobile.params.text.LicensesParams;
import org.base.mobile.strategy.TableQueryStrategy;
import org.base.core.service.QueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("licensesStrategy")
public class LicensesQueryStrategy implements TableQueryStrategy<LicensesParams> {
    private static final Logger LOGGER = LoggerFactory.getLogger(LicensesQueryStrategy.class);
    private static final String DEFAULT_TABLE_NAME = TableConfigText.DEFAULT_LICENSES_TABLE_NAME;
    private String tableName;


    public LicensesQueryStrategy(JdbcTemplate jdbcTemplate) {
        this.tableName = DEFAULT_TABLE_NAME;

    }

    @Override
    public String getTableName() {
        return tableName;
    }

    @Override
    public TableQueryStrategy<LicensesParams> setTableName(String tableName) {
        this.tableName = sanitizeTableName(tableName);
        TableConfigText.getTableMetadata(this.tableName);
        return this;
    }

    @Override
    public void configureQuery(QueryBuilder query, QueryParams<LicensesParams> params) {
        if (params == null || params.getParams() == null) {
            throw new IllegalArgumentException("Query parameters cannot be null");
        }
        LicensesParams p = params.getParams();
        String queryType = params.getQueryType();

        if ("years".equals(queryType)) {
            if (!TableConfigText.isValidColumn(tableName, "year")) {
                throw new IllegalArgumentException("Invalid column 'year' for table: " + tableName);
            }
            query.append("SELECT DISTINCT CAST(t.year AS VARCHAR) AS name, CAST(t.year AS INT) AS code ")
                    .append("FROM ").append(tableName).append(" t ");
            if (!DEFAULT_TABLE_NAME.equals(tableName)) {
                query.addFilter("t.type", "1", "=");
                query.addFilter("t.year", TableConfigText.DEFAULT_YEAR, "!=");
            }
            query.append("ORDER BY t.year DESC");
        } else {
            throw new IllegalArgumentException("Unknown query type: " + queryType);
        }

        LOGGER.debug("Configured licenses query for table: {}, type: {}", tableName, queryType);
    }

    private String sanitizeTableName(String tableName) {
        return tableName != null ? tableName.replaceAll("[^a-zA-Z0-9_\\[\\].]", "") : DEFAULT_TABLE_NAME;
    }

    @Override
    public List<String> getAttributes() {
        return List.of("year");
    }

    @Override
    public List<String> getGroupBy() {
        return List.of();
    }

    @Override
    public boolean isCombinedQuery() {
        return false;
    }
}
