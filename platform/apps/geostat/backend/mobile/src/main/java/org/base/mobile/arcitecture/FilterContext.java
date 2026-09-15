package org.base.mobile.arcitecture;

import org.base.mobile.params.RaceParams;
import org.base.core.service.QueryBuilder;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FilterContext<P> {
    private final Map<String, List<FilterRule<P, ?>>> tableFilterRules;

    private FilterContext(Map<String, List<FilterRule<P, ?>>> tableFilterRules) {
        this.tableFilterRules = Objects.requireNonNull(tableFilterRules, "Table filter rules cannot be null");
    }

    public void applyFilters(QueryBuilder query, String tableName, P params) {
        applyFilters(query, tableName, params, null);
    }

    public void applyFilters(QueryBuilder query, String tableName, P params, RuleExpression<P> overrideCondition) {
        List<FilterRule<P, ?>> rules = tableFilterRules.getOrDefault(tableName, List.of());
        for (FilterRule<P, ?> rule : rules) {
            QueryBuilder.Filter filter = rule.createFilter(params, overrideCondition);
            if (filter != null) {
                query.addFilter(filter);
            }
        }
    }

    public void applyDescriptorFilters(QueryBuilder query, String tableName, P params, ParameterDescriptor<P> descriptor, RuleExpression<P> overrideCondition) {
        if (descriptor.isValid(params)) {
            for (Map.Entry<String, Function<P, String>> entry : descriptor.getParameterMappings().entrySet()) {
                String column = entry.getKey();
                String value = entry.getValue().apply(params);
                if (value != null) {
                    FilterRule<P, String> rule = new FilterRule.Builder<P, String>("dynamic_" + column, column, p -> entry.getValue().apply(p))
                            .withPredicateCondition(p -> entry.getValue().apply(p) != null)
                            .build();
                    FilterBuilder<P> builder = FilterBuilderFactory.fromRuleWithCondition(rule);
                    QueryBuilder.Filter filter = builder.apply(params, overrideCondition);
                    if (filter != null) {
                        query.addFilter(filter);
                    }
                }
            }
        }
    }

    public List<Function<P, QueryBuilder.Filter>> getFilterBuilders(String tableName) {
        return tableFilterRules.getOrDefault(tableName, List.of())
                .stream()
                .map(FilterBuilderFactory::fromRule)
                .collect(Collectors.toList());
    }

    public FilterContext<P> withAdditionalRule(String tableName, FilterRule<P, ?> rule) {
        Map<String, List<FilterRule<P, ?>>> newFilterRules = new HashMap<>();
        for (Map.Entry<String, List<FilterRule<P, ?>>> entry : tableFilterRules.entrySet()) {
            newFilterRules.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        newFilterRules.computeIfAbsent(tableName, k -> new ArrayList<>()).add(rule);
        return new FilterContext<>(newFilterRules);
    }

    public static class Builder<P> {
        private final Map<String, List<FilterRule<P, ?>>> tableFilterRules;

        public Builder() {
            this.tableFilterRules = new HashMap<>();
        }

        public Builder<P> addRule(String tableName, FilterRule<P, ?> rule) {
            tableFilterRules.computeIfAbsent(tableName, k -> new ArrayList<>()).add(rule);
            return this;
        }

        public Builder<P> addRules(String tableName, List<FilterRule<P, ?>> rules) {
            tableFilterRules.computeIfAbsent(tableName, k -> new ArrayList<>()).addAll(rules);
            return this;
        }

        public FilterContext<P> build() {
            return new FilterContext<>(tableFilterRules);
        }
    }
}
