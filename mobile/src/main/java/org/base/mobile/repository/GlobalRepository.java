package org.base.mobile.repository;

import org.base.core.exeption.extend.ApiException;
import org.base.core.runner.TreeBasePagesMigrationRunner;
import org.base.core.service.QueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class GlobalRepository {


    @Autowired
    @Qualifier("secondaryJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    private static final Logger log = LoggerFactory.getLogger(TreeBasePagesMigrationRunner.class);

    //    public Optional<Integer> getMaxYear(String tableName) {
    //        String sql = "SELECT MAX(year) FROM " + tableName;
    //        Integer maxYear = jdbcTemplate.queryForObject(sql, Integer.class);
    //        return Optional.ofNullable(maxYear);
    //    }

    /**
     * Retrieves the maximum year for a table.
     */
//    public Optional<Integer> getMaxYear(String tableName) {
//        String query = "SELECT MAX(year) AS maxYear FROM " + tableName;
//        log.debug("Executing maxYear query for table {}: SQL = {}", tableName, query);
//        try {
//            Map<String, Object> result = jdbcTemplate.queryForMap(query);
//            return Optional.ofNullable(result.get("maxYear")).map(Number.class::cast).map(Number::intValue);
//        } catch (Exception e) {
//            log.error("Failed to retrieve maxYear for table {}: {}", tableName, e.getMessage());
//            return Optional.empty();
//        }
//    }

    public Optional<Map<String, Object>> executeMinMaxYearQuery(QueryBuilder query, String tableName) {
        try {
            //logger.debug("Executing min/max year query: {} with params: {}", query.getSql(), query.getParameters());
            List<Map<String, Object>> results = jdbcTemplate.queryForList(query.getSql(), query.getParameters().toArray());
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (Exception e) {
            //logger.error("Failed to execute min/max year query on table {}: {}", tableName, e.getMessage(), e);
            throw new ApiException("couldnt retrieve data", String.format("Min/max year query failed for table %s", tableName), e);
        }
    }

    //@Override
    public Optional<Integer> getMaxYear(String tableName) {
        try {
            String sql = String.format("SELECT MAX(year) AS maxYear FROM %s", tableName);
            //logger.debug("Executing max year query: {}", sql);
            Integer maxYear = jdbcTemplate.queryForObject(sql, Integer.class);
            return Optional.ofNullable(maxYear);
        } catch (Exception e) {
            //logger.error("Failed to retrieve max year for table {}: {}", tableName, e.getMessage(), e);
            throw new ApiException("Failed to retrieve max year",
                    String.format("Max year query failed for table %s", tableName), e);
        }
    }

    public Optional<Integer> getMaxQuarter(String tableName, Integer year) {
        QueryBuilder query = new QueryBuilder()
                .append("SELECT MAX(quarter) FROM ").append(tableName)
                .append(" WHERE 1=1")
                .addFilter("year", year, "?", null, null);
        Integer maxQuarter = jdbcTemplate.queryForObject(query.getSql(), query.getParams(), Integer.class);
        return Optional.ofNullable(maxQuarter);
    }

    public Optional<YearRange> getYearRange(String tableName) {
        String sql = "SELECT MIN(year) as min_year, MAX(year) as max_year FROM " + tableName;
        return jdbcTemplate.query(sql, rs -> {
            if (rs.next()) {
                return Optional.of(new YearRange(rs.getInt("min_year"), rs.getInt("max_year")));
            }
            return Optional.empty();
        });
    }

    /**
     * Retrieves the minimum year for a table with given filters.
     */
    public Optional<Integer> getMinYear(String tableName, Map<String, Object> queryMap) {
        StringBuilder query = new StringBuilder("SELECT MIN(year) AS minYear FROM ").append(tableName).append(" WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (queryMap != null) {
            queryMap.forEach((k, v) -> {
                query.append(" AND ").append(sanitizeColumn(k)).append(" = ?");
                params.add(v);
            });
        }
        log.debug("Executing minYear query for table {}: SQL = {}, Params = {}", tableName, query, params);
        try {
            Map<String, Object> result = jdbcTemplate.queryForMap(query.toString(), params.toArray());
            return Optional.ofNullable(result.get("minYear")).map(Number.class::cast).map(Number::intValue);
        } catch (Exception e) {
            log.error("Failed to retrieve minYear for table {}: {}", tableName, e.getMessage());
            return Optional.empty();
        }
    }
    /**
     * Executes a count query using the provided QueryBuilder.
     *
     * @param query      The QueryBuilder containing the SQL and parameters.
     * @param tableName  The table name for context (logged for debugging).
     * @return Count of rows matching the query.
     * @throws ApiException if the query execution fails.
     */
    public Optional<Map<String, Object>> executeTopModelQuery(QueryBuilder query, String tableName) {
        try {
            //logger.debug("Executing top model query: {} with params: {}", query.getSql(), query.getParameters());
            List<Map<String, Object>> results = jdbcTemplate.queryForList(
                    query.getSql(), query.getParameters().toArray());
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (Exception e) {
            //logger.error("Failed to execute top model query on table {}: {}", tableName, e.getMessage(), e);
            throw new ApiException("Failed to retrieve data from the database"+e.getCause(),
                    String.format("Top model query failed for table %s", tableName), e);
        }
    }

    /**
     * Executes a count query using the provided QueryBuilder.
     *
     * @param query      The QueryBuilder containing the SQL and parameters.
     * @param tableName  The table name for context (logged for debugging).
     * @return The count of rows matching the query.
     * @throws ApiException if the query execution fails.
     */
    public long executeCountQuery(QueryBuilder query, String tableName) {
        try {
            //logger.debug("Executing count query: {} with params: {}", query.getQuery(), query.getParameters());
            Long count = jdbcTemplate.queryForObject(
                    query.getQuery(),
                    query.getParameters().toArray(),
                    Long.class
            );
            return count != null ? count : 0L;
        } catch (Exception e) {
            //logger.error("Failed to execute count query on table {}: {}", tableName, e.getMessage(), e);
            throw new ApiException("Failed to retrieve data from the database"+e.getCause(),
                    String.format("Count query failed for table %s", tableName), e);
        }
    }

    //    public Optional<Integer> getMinYear(String tableName, Map<String, Object> queryParams) {
    //        StringBuilder sql = new StringBuilder("SELECT MIN(year) AS min_year FROM ").append(tableName).append(" WHERE 1=1");
    //        Object[] params = new Object[queryParams.size()];
    //        int index = 0;
    //        for (Map.Entry<String, Object> entry : queryParams.entrySet()) {
    //            sql.append(" AND ").append(entry.getKey()).append(" = ?");
    //            params[index++] = entry.getValue();
    //        }
    //        return jdbcTemplate.query(sql.toString(), params, rs -> {
    //            if (rs.next()) {
    //                return Optional.of(rs.getInt("min_year"));
    //            }
    //            return Optional.empty();
    //        });
    //    }

    public List<Map<String, Object>> executeQuery(QueryBuilder queryBuilder) {
        try {
            return jdbcTemplate.queryForList(queryBuilder.getSql(), queryBuilder.getParams());
        } catch (Exception e) {
            throw new RuntimeException("Database query failed for table ", e);
        }
    }

    /**
     * Executes a single query with parameters.
     *
     * @param query      The QueryBuilder containing the SQL and parameters.
     * @param tableName  The table name for context (logged for debugging).
     * @return List of rows as maps of column names to values.
     * @throws IllegalArgumentException if query is invalid.
     */
    public List<Map<String, Object>> executeAreaCurrencyQuery(QueryBuilder query, String tableName) {
        validateQuery(query, tableName);
        String sql = query.getSql();
        Object[] params = query.getParams();
        log.debug("Executing query for table {}: SQL = {}, Params = {}", tableName, sql, params);
        try {
            return jdbcTemplate.queryForList(sql, params);
        } catch (Exception e) {
            log.error("Failed to execute query for table {}: {}", tableName, e.getMessage());
            throw new ApiException("Failed to retrieve data from the database"+e.getCause(), "Query execution failed", e);
        }
    }

    /**
     * Executes multiple sub-queries defined in the QueryBuilder.
     *
     * @param query      The QueryBuilder containing sub-queries.
     * @param tableName  The table name for context (logged for debugging).
     * @return Map of sub-query names to their results (list of row maps).
     * @throws IllegalArgumentException if sub-queries are invalid.
     */
    public Map<String, List<Map<String, Object>>> executeSubQueries(QueryBuilder query, String tableName) {
        if (query == null || query.getSubQueries().isEmpty()) {
            throw new IllegalArgumentException("No sub-queries provided for table " + tableName);
        }
        Map<String, List<Map<String, Object>>> results = new LinkedHashMap<>();
        for (Map.Entry<String, QueryBuilder> entry : query.getSubQueries().entrySet()) {
            String subQueryName = entry.getKey();
            QueryBuilder subQuery = entry.getValue();
            validateQuery(subQuery, tableName);
            String sql = subQuery.getSql();
            Object[] params = subQuery.getParams();
            log.debug("Executing sub-query '{}' for table {}: SQL = {}, Params = {}", subQueryName, tableName, sql, params);
            try {
                List<Map<String, Object>> subResult = jdbcTemplate.queryForList(sql, params);
                results.put(subQueryName, subResult);
            } catch (Exception e) {
                log.error("Failed to execute sub-query '{}' for table {}: {}", subQueryName, tableName, e.getMessage());
                throw new RuntimeException("Sub-query '" + subQueryName + "' failed for table " + tableName, e);
            }
        }
        return results;
    }


    public long executeSumQuery(QueryBuilder query, String tableName) {
        try {
            //logger.debug("Executing sum query: {} with params: {}", query.getSql(), query.getParameters());
            Long sum = jdbcTemplate.queryForObject(
                    query.getSql(),
                    query.getParameters().toArray(),
                    Long.class
            );
            return sum != null ? sum : 0L;
        } catch (Exception e) {
            //logger.error("Failed to execute sum query on table {}: {}", tableName, e.getMessage(), e);
            throw new ApiException("Failed to retrieve data from the database"+e.getCause(),
                    String.format("Sum query failed for table %s", tableName), e);
        }
    }

    /**
     * Validates a QueryBuilder before execution.
     */
    private void validateQuery(QueryBuilder query, String tableName) {
        if (query == null) {
            throw new IllegalArgumentException("QueryBuilder cannot be null for table " + tableName);
        }
        if (query.getSql().trim().isEmpty()) {
            throw new IllegalArgumentException("SQL query cannot be empty for table " + tableName);
        }
    }

    /**
     * Sanitizes column names to prevent SQL injection.
     */
    private String sanitizeColumn(String column) {
        if (column == null) return "";
        String sanitized = column.replaceAll("[^a-zA-Z0-9_.]", "");
        if (!sanitized.equals(column)) {
            log.warn("Sanitized column name from '{}' to '{}'", column, sanitized);
        }
        return sanitized;
    }

    public static class YearRange {
        private final int minYear;
        private final int maxYear;

        public YearRange(int minYear, int maxYear) {
            this.minYear = minYear;
            this.maxYear = maxYear;
        }

        public int getMinYear() { return minYear; }
        public int getMaxYear() { return maxYear; }
    }
}
