package org.base.core.service;

import lombok.Getter;
import org.base.core.model.ClassificationTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class QueryBuilder {

    private static final Logger log = LoggerFactory.getLogger(QueryBuilder.class);

    private final StringBuilder sql;
    private final List<Object> params;
    private final List<String> groupBy;
    private final List<String> orderBy;
    private final List<String> cteClauses;
    private final List<String> unionQueries;
    @Getter
    private final Map<String, QueryBuilder> subQueries;
    private String paginationClause;
    private boolean hasSelectStarted = false;
    private boolean hasTopClause = false;
    private final ExpressionValidator expressionValidator;

    public QueryBuilder(ExpressionValidator expressionValidator) {
        this.sql = new StringBuilder();
        this.params = new ArrayList<>();
        this.groupBy = new ArrayList<>();
        this.orderBy = new ArrayList<>();
        this.cteClauses = new ArrayList<>();
        this.unionQueries = new ArrayList<>();
        this.subQueries = new LinkedHashMap<>();
        this.paginationClause = null;
        this.expressionValidator = Objects.requireNonNull(expressionValidator, "Expression validator cannot be null");
    }

    public QueryBuilder() {
        this(new DefaultExpressionValidator());
    }

    public void clear() {
        sql.setLength(0);
    }

    /**
     * Appends an SQL fragment to the main query.
     *
     * @param sqlPart The SQL fragment to append.
     * @return This QueryBuilder for chaining.
     * @throws IllegalArgumentException if sqlPart is null or empty.
     */
    public QueryBuilder append(String sqlPart, boolean withSpace) {
        if (sqlPart == null) { //|| sqlPart.trim().isEmpty()
            throw new IllegalArgumentException("SQL part cannot be null or empty");
        }
        if (withSpace) {
            sqlPart = " " + sqlPart + " ";
        }
        sql.append(sqlPart);
        return this;
    }

    public QueryBuilder append(String sqlPart) {
        return append(sqlPart, false);
    }

    public QueryBuilder addSelect(String column, String alias) {
        return addSelect(column, alias, false);
    }

    public QueryBuilder addTop(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("TOP limit must be positive");
        }
        if (hasSelectStarted) {
            int selectIndex = sql.indexOf("SELECT ");
            if (selectIndex >= 0) {
                sql.insert(selectIndex + 7, "TOP " + limit + " ");
            } else {
                throw new IllegalStateException("Cannot add TOP clause without SELECT");
            }
        } else {
            sql.append("SELECT TOP ").append(limit).append(" ");
            hasTopClause = true;
        }
        return this;
    }

    /**
     * Adds a column or expression to the SELECT clause with an optional alias.
     *
     * @param column The column or expression (e.g., "t.year", "SUM(t.quantity)", "CAST(t.quarter AS NVARCHAR)").
     * @param alias  Optional alias for the column (e.g., "total_quantity", "quarter"). Can be null.
     * @return This QueryBuilder for chaining.
     * @throws IllegalArgumentException if column is null or empty.
     */
    public QueryBuilder addSelect(String column, String alias, boolean isLiteral) {
        if (column == null || column.trim().isEmpty()) {
            throw new IllegalArgumentException("Column, expression, or literal cannot be null or empty");
        }
        if (!hasSelectStarted) {
            if (!hasTopClause) {
                sql.append("SELECT ");
            }
            hasSelectStarted = true;
        } else {
            sql.append(", ");
        }
        String sanitizedColumn;
        if (isLiteral) {
            sanitizedColumn = column;
        } else if (column.equals("*")) {
            sanitizedColumn = "*";
        } else if (column.contains("(") || column.contains(" ") || column.contains(".")) {
            sanitizedColumn = column.replaceAll("[;]", "");
        } else {
            sanitizedColumn = sanitizeColumn(column);
        }
        sql.append(sanitizedColumn);
        if (alias != null && !alias.trim().isEmpty()) {
            sql.append(" AS ").append(sanitizeColumn(alias));
        }
        return this;
    }


    /**
     * Adds a filter condition to the query.
     * @param column Column name (e.g., "year", "quarter").
     * @param value Filter value (e.g., 2023, "1").
     * @param operator Placeholder for the value (e.g., "?").
     * @param valueWrapper Optional list of quarters for "quarter" column when value is null or "99".
     * @return This QueryBuilder for chaining.
     */
    public QueryBuilder addFilter(String column, Object value, String operator, String valueWrapper, List<?> defaultValues) {
        if (column == null || column.trim().isEmpty()) {
            throw new IllegalArgumentException("Column name cannot be null or empty");
        }
        if (operator == null || operator.trim().isEmpty()) {
            throw new IllegalArgumentException("Operator cannot be null or empty");
        }
        return addFilter(new ColumnFilter(column, value, operator, valueWrapper));
    }

    public QueryBuilder addFilter(String column, Object value, String operator) {
        return addFilter(column, value, operator, null, null);
    }

    public QueryBuilder addFilter(Filter filter) {
        if (filter != null) {
            filter.apply(this);
        }
        //        if (filter != null) {
        //            appendCondition(filter.getSql(this), filter.getParams());
        //        }
        return this;
    }


//    public QueryBuilder addFilter(String column, Object value, String placeholder, List<Integer> defaultQuarters) {
//        if (column == null || column.trim().isEmpty()) {
//            throw new IllegalArgumentException("Column name cannot be null or empty");
//        }
//        if (placeholder == null || !placeholder.equals("?")) {
//            throw new IllegalArgumentException("Placeholder must be '?'");
//        }
//        String sanitizedColumn = sanitizeColumn(column);
//        if (value != null && !value.equals("99")) {
//            sql.append(" AND ").append(sanitizeColumn(column)).append(" = ?");
//            params.add(value);
//        } else if (column.equals("quarter") && (value == null || value.equals("99"))) {
//            List<Integer> quarters = defaultQuarters != null && !defaultQuarters.isEmpty()
//                    ? defaultQuarters
//                    : List.of(1, 2, 3, 4);
//            sql.append(" AND t.quarter IN (")
//                    .append(quarters.stream().map(String::valueOf).collect(Collectors.joining(", ")))
//                    .append(")");
//        }
//        return this;
//    }


    /**
     * Adds a LEFT JOIN to a classification table.
     *
     * @param clTable        The classification table configuration.
     * @param langName       Language name for column selection.
     * @param mainTableAlias Alias of the main table.
     * @return This QueryBuilder for chaining.
     */
    public QueryBuilder addJoin(ClassificationTable clTable, String langName, String mainTableAlias) {
        if (clTable != null && mainTableAlias != null && langName != null) {
            sql.append(" LEFT JOIN ").append(sanitizeColumn(clTable.getTableName())).append(" ")
                    .append(sanitizeColumn(clTable.getAlias())).append(" ON ")
                    .append(sanitizeColumn(mainTableAlias)).append(".").append(sanitizeColumn(clTable.getKeyColumn()))
                    .append(" = ").append(sanitizeColumn(clTable.getAlias())).append(".").append("id"); //sanitizeColumn(clTable.getKeyColumn());
        }
        return this;
    }

//    public QueryBuilder addJoin(ClassificationTable clTable, String langName, String mainTableAlias) {
//        if (clTable != null && mainTableAlias != null && langName != null) {
//            sql.append(" LEFT JOIN ").append(clTable.getTableName()).append(" ")
//                    .append(clTable.getAlias()).append(" ON ").append(sanitizeColumn(mainTableAlias)).append(".")
//                    .append(clTable.getKeyColumn()).append(" = ").append(clTable.getAlias()).append(".")
//                    .append(clTable.getKeyColumn());
//        }
//        return this;
//    }

    /**
     * Adds GROUP BY columns, preventing duplicates.
     */
    public QueryBuilder addGroupBy(String... columns) {
        if (columns != null) {
            groupBy.addAll(Arrays.stream(columns)
                    .filter(c -> c != null && !c.trim().isEmpty())
                    .map(this::sanitizeColumn)
                    .collect(Collectors.toSet()));
        }
        return this;
    }

    /**
     * Adds GROUP BY columns, preventing duplicates.
     */
    public QueryBuilder addGroupByExpression(String... columns) {
        if (columns != null) {
            groupBy.addAll(Arrays.stream(columns)
                    .filter(c -> c != null && !c.trim().isEmpty())
                    .collect(Collectors.toSet()));
        }
        return this;
    }

    /**
     * Adds an ORDER BY clause.
     */
    public QueryBuilder addOrderBy(String column, String direction) {
        if (column != null && !column.trim().isEmpty() && direction != null &&
                (direction.equalsIgnoreCase("ASC") || direction.equalsIgnoreCase("DESC"))) {
            orderBy.add(sanitizeColumn(column) + " " + direction.toUpperCase());
        }
        return this;
    }

    /**
     * Adds an ORDER BY clause for a complex expression (e.g., SUM(t.quantity)).
     */
    public QueryBuilder addOrderByExpression(String expression, String direction) {
        if (expression != null && !expression.trim().isEmpty() && direction != null &&
                (direction.equalsIgnoreCase("ASC") || direction.equalsIgnoreCase("DESC"))) {
            // Minimal sanitization to prevent injection while preserving expression
            String sanitizedExpression = expression.replaceAll("[;]", "");
            orderBy.add(sanitizedExpression + " " + direction.toUpperCase());
        }
        return this;
    }

    /**
     * Adds a rounded column to the SELECT clause.
     */
    public QueryBuilder addRoundedColumn(String column, int decimals, String alias) {
        if (column != null && alias != null && decimals >= 0) {
            sql.append(", ROUND(").append(sanitizeColumn(column)).append(", ").append(decimals)
                    .append(") AS ").append(sanitizeColumn(alias));
        }
        return this;
    }


//    public QueryBuilder addRoundedColumn(String column, int decimals, String alias) {
//        if (column != null && alias != null && decimals >= 0) {
//            sql.append(", ROUND(t.").append(sanitizeColumn(column)).append(", ").append(decimals)
//                    .append(") AS ").append(sanitizeColumn(alias));
//        }
//        return this;
//    }

    /**
     * Adds a Common Table Expression (CTE).
     */
    public QueryBuilder addCte(String cteName, String cteQuery) {
        if (cteName != null && cteQuery != null && !cteName.trim().isEmpty() && !cteQuery.trim().isEmpty()) {
            cteClauses.add(sanitizeColumn(cteName) + " AS (" + cteQuery + ")");
        }
        return this;
    }

    public QueryBuilder addCte(String cteName, QueryBuilder cteQuery) {
        if (cteName != null && cteQuery != null && !cteName.trim().isEmpty()) {
            cteClauses.add(sanitizeColumn(cteName) + " AS (" + cteQuery.getSql() + ")");
            mergeParameters(cteQuery.getParameters());
        }
        return this;
    }

    List<Object> flatten(Object input) {
        List<Object> result = new ArrayList<>();

        if (input instanceof List<?>) {
            for (Object item : (List<?>) input) {
                if (item instanceof List<?>) {
                    result.addAll(flatten(item)); // recurse
                } else if (item != null) {
                    result.add(item); // add non-null item
                }
            }
        } else {
            result.add(input); // base case
        }

        return result;
    }

    /**
     * Adds a UNION query with parameters.
     */
    public QueryBuilder addUnionQuery(String unionQuery, Object... unionParams) {
        if (unionQuery != null && !unionQuery.trim().isEmpty()) {
            unionQueries.add(unionQuery);
            if (unionParams != null) {
                Object flattenedParams = flatten(Arrays.asList(unionParams));
                mergeParameters((List<Object>) flattenedParams);
            }
        }
        return this;
    }

    /**
     * Adds a sub-query using a configurer function.
     */
    public QueryBuilder appendSubQuery(String name, Consumer<QueryBuilder> queryConfigurer) {
        if (name == null || name.trim().isEmpty() || queryConfigurer == null) {
            throw new IllegalArgumentException("Sub-query name and configurer cannot be null or empty");
        }
        QueryBuilder subQuery = new QueryBuilder();
        queryConfigurer.accept(subQuery);
        subQueries.put(name, subQuery);
        mergeParameters(subQuery.getParameters());
        return this;
    }

    /**
     * Adds a sub-query for multi-query endpoints.
     */
    public QueryBuilder addSubQuery(String name, QueryBuilder subQuery) {
        if (name != null && !name.trim().isEmpty() && subQuery != null) {
            subQueries.put(name, subQuery);
            mergeParameters(subQuery.getParameters());
        }
        return this;
    }

    /**
     * Merges parameters from another source, preserving order.
     */
    public QueryBuilder mergeParameters(List<Object> params) {
        if (params != null) {
            this.params.addAll(params);
        }
        return this;
    }

    /**
     * Builds the final SQL query, including CTEs, UNIONs, GROUP BY, and ORDER BY.
     *
     * @return The complete SQL string.
     */
    public String getSql() {
        StringBuilder finalSql = new StringBuilder();
        if (!cteClauses.isEmpty()) {
            finalSql.append("WITH ").append(String.join(", ", cteClauses)).append(" ");
        }
        if (!unionQueries.isEmpty()) {
            // If only one union query and sql is empty, use it directly
            if (unionQueries.size() == 1 && sql.length() == 0) {
                finalSql.append(unionQueries.get(0));
            } else {
                // Combine main sql with union queries
                if (sql.length() > 0) {
                    finalSql.append(sql);
                }
                if (!unionQueries.isEmpty()) {
                    if (sql.length() > 0) {
                        finalSql.append(" UNION ALL ");
                    }
                    finalSql.append(String.join(" UNION ALL ", unionQueries));
                }
            }
        } else {
            finalSql.append(sql);
        }
        if (!groupBy.isEmpty()) {
            finalSql.append(" GROUP BY ").append(String.join(", ", groupBy));
        }
        if (!orderBy.isEmpty()) {
            finalSql.append(" ORDER BY ").append(String.join(", ", orderBy));
        }
        if (paginationClause != null) {
            finalSql.append(" ").append(paginationClause);
        }
        String result = finalSql.toString();
        log.debug("Generated SQL: {}", result);
        return result;
    }

    /**
     * Sets pagination for MSSQL using OFFSET and FETCH.
     *
     * @param offset Number of rows to skip.
     * @param fetchRows Number of rows to fetch.
     * @return This QueryBuilder for chaining.
     */
    public QueryBuilder setPagination(int offset, int fetchRows) {
        if (offset < 0 || fetchRows <= 0) {
            throw new IllegalArgumentException("Offset must be non-negative and fetchRows must be positive");
        }
        this.paginationClause = String.format(" OFFSET %d ROWS FETCH NEXT %d ROWS ONLY", offset, fetchRows);
        return this;
    }

    public String getQuery() {
        return getSql();
    }

    /**
     * Sanitizes column names to prevent SQL injection.
     */
    public String sanitizeColumn(String input) {
        if (input == null) return "";
        // Allow alphanumeric, dots, underscores, square brackets, spaces, parentheses,
        // and specific SQL keywords (CAST, AS, NVARCHAR, VARCHAR, UPPER, LOWER)
        String allowedPattern = "[a-zA-Z0-9_.\\[\\]* ()]|\\b(CAST|AS|NVARCHAR|VARCHAR|UPPER|LOWER)\\b";
        StringBuilder sanitized = new StringBuilder();
        String[] parts = input.split("(?<=\\b)");
        for (String part : parts) {
            if (part.matches(allowedPattern)) {
                sanitized.append(part);
            } else {
                // Replace unsafe characters with empty string
                sanitized.append(part.replaceAll("[^a-zA-Z0-9_.\\[\\]* ()]", ""));
            }
        }
        // Block dangerous patterns (e.g., semicolons, comments)
        String result = sanitized.toString();
        if (Pattern.compile("[;]|--|(/\\*.*\\*/)").matcher(result).find()) {
            throw new IllegalArgumentException("Invalid SQL expression: " + input);
        }
        return result.trim();
    }

//    private String sanitizeColumn(String column) {
//        if (column == null) return "";
//        // Basic sanitization: allow alphanumeric, underscore, dot (for table.column)
//        String sanitized = column.replaceAll("[^a-zA-Z0-9_.]", "");
//        if (!sanitized.equals(column)) {
//            //logger.warn("Sanitized column name from '{}' to '{}'", column, sanitized);
//        }
//        return sanitized;
//    }

    /**
     * Returns the list of query parameters.
     */
    public List<Object> getParameters() {
        return params;
    }

    /**
     * Returns query parameters as an array.
     */
    public Object[] getParams() {
        return params.toArray();
    }

    void appendCondition(String condition, List<?> parameters) {
        sql.append(" AND ").append(condition);
        params.addAll(parameters);
    }

    public interface Filter {
        void apply(QueryBuilder builder);
        //String getSql(QueryBuilder builder);
        //List<Object> getParams();
    }

    public static class ColumnFilter implements Filter {
        private final String column;
        private final Object value;
        private final String operator;
        private final String placeholder;

        public ColumnFilter(String column, Object value, String operator, String placeholder) {
            this.column = column;;
            this.value = value;
            this.operator = operator;
            this.placeholder = placeholder != null ? placeholder : "?";
        }

        @Override
        public void apply(QueryBuilder builder) {
            if (value == null) {
                return;
            }
            String condition = String.format("%s %s %s", builder.expressionValidator.validate(column), operator, placeholder);
            List<Object> parameters = placeholder.contains("?") ? new ArrayList<>(List.of(value)) : new ArrayList<>();
            builder.appendCondition(condition, parameters);
        }

        //        @Override
        //        public String getSql(QueryBuilder builder) {
        //            return String.format("%s %s %s", builder.expressionValidator.validate(column), operator, placeholder);
        //        }
        //
        //        @Override
        //        public List<Object> getParams() {
        //            return value != null ? List.of(value) : List.of();
        //        }
    }

    public static class InFilter implements Filter {
        private final String expression;
        private final List<?> values;
        private final String operator;

        public InFilter(String expression, List<?> values, String operator) {
            this.expression = expression;
            this.values = values != null ? values : List.of();
            this.operator = operator != null ? operator : "IN";
        }

        public InFilter(String expression, List<?> values) {
            this(expression, values, "IN");
        }

        @Override
        public void apply(QueryBuilder builder) {
            if (values.isEmpty()) {
                return;
            }
            String placeholders = values.stream().map(v -> "?").collect(Collectors.joining(", "));
            String condition = String.format("%s %s (%s)", builder.expressionValidator.validate(expression), operator, placeholders);
            builder.appendCondition(condition, values);
        }

        //        @Override
        //        public String getSql(QueryBuilder builder) {
        //            String placeholders = values.stream().map(v -> "?").collect(Collectors.joining(", "));
        //            return String.format("%s %s (%s)", builder.expressionValidator.validate(expression), operator, placeholders);
        //        }
        //
        //        @Override
        //        public List<Object> getParams() {
        //            return List.of();
        //        }
    }

    public static class ExpressionFilter implements Filter {
        private final String expression;
        private final Object value;
        private final String operator;
        private final String placeholder;

        public ExpressionFilter(String expression, Object value, String operator, String placeholder) {
            this.expression = expression;
            this.value = value;
            this.operator = operator;
            this.placeholder = placeholder != null ? placeholder : "?";
        }

        @Override
        public void apply(QueryBuilder builder) {
            if (value == null) {
                return;
            }
            String condition = String.format("%s %s %s", builder.expressionValidator.validate(expression), operator, placeholder);
            List<Object> parameters = placeholder.contains("?") ? new ArrayList<>(List.of(value)) : new ArrayList<>();
            builder.appendCondition(condition, parameters);
        }

        //        @Override
        //        public String getSql(QueryBuilder builder) {
        //            return  String.format("%s %s %s", builder.expressionValidator.validate(expression), operator, placeholder);;
        //        }
        //
        //        @Override
        //        public List<Object> getParams() {
        //            return placeholder.contains("?") ? List.of(value) : List.of();
        //        }
    }

    public interface ExpressionValidator {
        String validate(String expression);
    }

    public static class DefaultExpressionValidator implements ExpressionValidator {
        private static final List<String> ALLOWED_PATTERNS = List.of(
                "CAST\\(t\\.[a-zA-Z0-9_]+ AS NVARCHAR\\)",
                "t\\.[a-zA-Z0-9_]+",
                "UPPER\\(t\\.[a-zA-Z0-9_]+\\)",
                "[a-zA-Z0-9_]+"
        );

        @Override
        public String validate(String expression) {
            if (expression == null || expression.trim().isEmpty()) {
                throw new IllegalArgumentException("Filter expression cannot be null or empty");
            }
            for (String pattern : ALLOWED_PATTERNS) {
                if (expression.matches(pattern)) {
                    return expression;
                }
            }
            throw new IllegalArgumentException("Invalid filter expression: " + expression);
        }
    }
}
