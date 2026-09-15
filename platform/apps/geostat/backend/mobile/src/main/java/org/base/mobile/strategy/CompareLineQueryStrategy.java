package org.base.mobile.strategy;

import org.base.mobile.arcitecture.*;
import org.base.mobile.params.CompareParams;
import org.base.mobile.params.QueryParams;
import org.base.core.service.QueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component("compareLineStrategy")
public class CompareLineQueryStrategy implements TableQueryStrategy<CompareParams> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CompareLineQueryStrategy.class);
    private static final String TABLE_NAME = "[dbo].[auto_main]";
    private final FilterContext<CompareParams> baseContext;

    public CompareLineQueryStrategy() {
        this(FilterContextFactory.defaultCommonContext());
    }

    public CompareLineQueryStrategy(FilterContext<CompareParams> baseContext) {
        this.baseContext = Objects.requireNonNull(baseContext, "FilterContext cannot be null");
    }

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public TableQueryStrategy<CompareParams> setTableName(String tableName) {
        return this;
    }

    private String normalize(String value) {
        return (value == null || value.trim().isEmpty()) ? null : value.trim();
    }

    @Override
    public void configureQuery(QueryBuilder query, QueryParams<CompareParams> params) {
        if (params == null || params.getParams() == null) {
            throw new IllegalArgumentException("Query parameters cannot be null");
        }
        CompareParams p = params.getParams();

        // Validate parameters
        validateParams(p);

//        ParameterDescriptor<CompareParams> config1Descriptor = new ParameterDescriptor.Builder<CompareParams>("config1")
//                .addParameter("t.brand", CompareParams::getBrand1)
//                .addParameter("t.model", CompareParams::getModel1)
//                .addParameter("t.year_of_production", CompareParams::getYearOfProduction1)
//                .build();
//
//        // First query
//        QueryBuilder config1Query = new QueryBuilder();
//        config1Query.append("SELECT t.year, SUM(t.quantity) AS quantity, '").append(config1Descriptor.getConfigName()).append("' AS config ")
//                .append("FROM ").append(TABLE_NAME).append(" t ")
//                .append("WHERE t.year != 2026 ");
//        RuleExpression<CompareParams> config1Condition = p.getBrand1() != null
//                ? RuleExpression.fromPredicate(param -> !param.getBrand1().isEmpty())
//                : null;
//        baseContext.applyDescriptorFilters(config1Query, TABLE_NAME, p, config1Descriptor, null);
//        config1Query.addGroupBy("t.year");
//        addGroupBy(config1Query, p, true);

        // Define rules for first query (brand1, model1, year_of_production1)
        FilterContext<CompareParams> config1Context = baseContext;
        config1Context = config1Context.withAdditionalRule(TABLE_NAME,
                new FilterRule.Builder<CompareParams, String>("brand1", "t.brand", CompareParams::getBrand1)
                        .withPredicateCondition(param -> normalize(param.getBrand1()) != null && !normalize(param.getBrand1()).isEmpty())
                        .build());
        config1Context = config1Context.withAdditionalRule(TABLE_NAME,
                new FilterRule.Builder<CompareParams, String>("model1", "t.model", CompareParams::getModel1)
                        .withPredicateCondition(param -> normalize(param.getModel1()) != null && !normalize(param.getModel1()).isEmpty())
                        .build());
        config1Context = config1Context.withAdditionalRule(TABLE_NAME,
                new FilterRule.Builder<CompareParams, String>("year_of_production1", "t.year_of_production", CompareParams::getYearOfProduction1)
                        .withPredicateCondition(param ->normalize(param.getYearOfProduction1()) != null && !normalize(param.getYearOfProduction1()).isEmpty())
                        .build());

        // First query
        QueryBuilder config1Query = new QueryBuilder();
        config1Query.append("SELECT t.year, SUM(t.quantity) AS quantity, 'config1' AS config ")
                .append("FROM ").append(TABLE_NAME).append(" t ")
                .append("WHERE t.year != 2026 ");
        // Add runtime expression (example: brand1 must be non-empty)
        RuleExpression<CompareParams> config1Condition = p.getBrand1() != null
                ? RuleExpression.fromPredicate(param -> !param.getBrand1().isEmpty())
                : null;
        config1Context.applyFilters(config1Query, TABLE_NAME, p, config1Condition);
        config1Query.addGroupBy("t.year");
        addGroupBy(config1Query, p, true);

        // Define rules for second query (brand2, model2, year_of_production2)
        FilterContext<CompareParams> config2Context = baseContext;
        config2Context = config2Context.withAdditionalRule(TABLE_NAME,
                new FilterRule.Builder<CompareParams, String>("brand2", "t.brand", CompareParams::getBrand2)
                        .withPredicateCondition(param ->  normalize(param.getBrand2()) != null && !normalize(param.getBrand2()).isEmpty())
                        .build());
        config2Context = config2Context.withAdditionalRule(TABLE_NAME,
                new FilterRule.Builder<CompareParams, String>("model2", "t.model", CompareParams::getModel2)
                        .withPredicateCondition(param ->  normalize(param.getModel2()) != null && !normalize(param.getModel2()).isEmpty())
                        .build());
        config2Context = config2Context.withAdditionalRule(TABLE_NAME,
                new FilterRule.Builder<CompareParams, String>("year_of_production2", "t.year_of_production", CompareParams::getYearOfProduction2)
                        .withPredicateCondition(param -> normalize(param.getYearOfProduction2()) != null && !normalize(param.getYearOfProduction2()).isEmpty())
                        .build());

        // Second query
        QueryBuilder config2Query = new QueryBuilder();
        config2Query.append("SELECT t.year, SUM(t.quantity) AS quantity, 'config2' AS config ")
                .append("FROM ").append(TABLE_NAME).append(" t ")
                .append("WHERE t.year != 2026 ");
        // Add runtime expression (example: brand2 must be non-empty)
        RuleExpression<CompareParams> config2Condition = p.getBrand2() != null
                ? RuleExpression.fromPredicate(param -> !param.getBrand2().isEmpty())
                : null;
        config2Context.applyFilters(config2Query, TABLE_NAME, p, config2Condition);
        config2Query.addGroupBy("t.year");
        addGroupBy(config2Query, p, false);

        // Combine queries
        query.addUnionQuery(config1Query.getSql(), config1Query.getParameters());
        query.addUnionQuery(config2Query.getSql(), config2Query.getParameters());
        query.addOrderBy("t.year", "ASC");

        LOGGER.debug("Configured compare query for brand1: {}, model1: {}, yearOfProduction1: {}, brand2: {}, model2: {}, yearOfProduction2: {}, params: {}",
                p.getBrand1(), p.getModel1(), p.getYearOfProduction1(), p.getBrand2(), p.getModel2(), p.getYearOfProduction2(), query.getParameters());
    }

    private void validateParams(CompareParams p) {
        if (p.getBrand1() != null && !(p.getBrand1() instanceof String)) {
            throw new IllegalArgumentException("brand1 must be a String, got: " + p.getBrand1().getClass());
        }
        if (p.getBrand2() != null && !(p.getBrand2() instanceof String)) {
            throw new IllegalArgumentException("brand2 must be a String, got: " + p.getBrand2().getClass());
        }
        if (p.getModel1() != null && !(p.getModel1() instanceof String)) {
            throw new IllegalArgumentException("model1 must be a String, got: " + p.getModel1().getClass());
        }
        if (p.getModel2() != null && !(p.getModel2() instanceof String)) {
            throw new IllegalArgumentException("model2 must be a String, got: " + p.getModel2().getClass());
        }
        if (p.getYearOfProduction1() != null && !(p.getYearOfProduction1() instanceof String)) {
            throw new IllegalArgumentException("yearOfProduction1 must be a String, got: " + p.getYearOfProduction1().getClass());
        }
        if (p.getYearOfProduction2() != null && !(p.getYearOfProduction2() instanceof String)) {
            throw new IllegalArgumentException("yearOfProduction2 must be a String, got: " + p.getYearOfProduction2().getClass());
        }
    }

    private void addGroupBy(QueryBuilder query, CompareParams p, boolean isConfig1) {
        if (isConfig1) {
            if (p.getBrand1() != null && !p.getBrand1().isEmpty()) query.addGroupBy("t.brand");
            if (p.getModel1() != null && !p.getModel1().isEmpty()) query.addGroupBy("t.model");
            if (p.getYearOfProduction1() != null && !p.getYearOfProduction1().isEmpty()) query.addGroupBy("t.year_of_production");
        } else {
            if (p.getBrand2() != null && !p.getBrand2().isEmpty()) query.addGroupBy("t.brand");
            if (p.getModel2() != null && !p.getModel2().isEmpty()) query.addGroupBy("t.model");
            if (p.getYearOfProduction2() != null && !p.getYearOfProduction2().isEmpty()) query.addGroupBy("t.year_of_production");
        }
    }

    @Override
    public List<String> getAttributes() {
        return List.of("year", "quantity", "config");
    }

    @Override
    public List<String> getGroupBy() {
        return List.of("year");
    }

    @Override
    public boolean isCombinedQuery() {
        return true;
    }
}
