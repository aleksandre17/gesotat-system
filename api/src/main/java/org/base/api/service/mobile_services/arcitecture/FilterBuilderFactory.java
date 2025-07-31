package org.base.api.service.mobile_services.arcitecture;

import org.base.api.service.mobile_services.params.RaceParams;
import org.base.core.service.QueryBuilder;

import java.util.function.Function;

public class FilterBuilderFactory {
    /**
     * Creates a FilterBuilder from a FilterRule, supporting two parameters:
     * params (P) and overrideCondition (RuleExpression<P>).
     *
     * @param <P> the parameter type (e.g., CompareParams)
     * @param rule the FilterRule to wrap
     * @return a FilterBuilder that applies the rule's createFilter method
     */
    public static <P> Function<P, QueryBuilder.Filter> fromRule(FilterRule<P, ?> rule) {
        return params -> rule.createFilter(params, null);
    }

    /**
     * Creates a FilterBuilder that supports two parameters (params and overrideCondition).
     *
     * @param <P> the parameter type (e.g., CompareParams)
     * @param rule the FilterRule to wrap
     * @return a FilterBuilder applying the rule's createFilter method
     */
    public static <P> FilterBuilder<P> fromRuleWithCondition(FilterRule<P, ?> rule) {
        return (params, overrideCondition) -> rule.createFilter(params, overrideCondition);
    }

    public static Object fromRule(Function<RaceParams, QueryBuilder.Filter> raceParamsFilterFunction) {
        return raceParamsFilterFunction;
    }
}
