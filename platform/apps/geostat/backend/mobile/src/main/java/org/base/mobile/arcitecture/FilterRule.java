package org.base.mobile.arcitecture;

import org.base.core.service.QueryBuilder;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class FilterRule<P, T> {
    private final String name;
    private final String expression;
    private final Function<P, T> valueExtractor;
    private final String operator;
    private final String placeholder;
    private final String specialValue;
    private final List<T> defaultValues;
    private final boolean isExpression;
    private final RuleExpression<P> baseCondition;

    private FilterRule(String name, String expression, Function<P, T> valueExtractor,
                       String operator, String placeholder, String specialValue,
                       List<T> defaultValues, boolean isExpression, RuleExpression<P> baseCondition) {
        this.name = name;
        this.expression = expression;
        this.valueExtractor = valueExtractor;
        this.operator = operator != null ? operator : "=";
        this.placeholder = placeholder != null ? placeholder : "?";
        this.specialValue = specialValue;
        this.defaultValues = defaultValues;
        this.isExpression = isExpression;
        this.baseCondition = baseCondition != null ? baseCondition : RuleExpression.alwaysTrue();
    }

    public String getName() {
        return name;
    }

    public QueryBuilder.Filter createFilter(P params, RuleExpression<P> overrideCondition) {
        RuleExpression<P> condition = overrideCondition != null ? baseCondition.and(overrideCondition) : baseCondition;
        if (!condition.evaluate(params)) {
            return null;
        }
        T value = valueExtractor.apply(params);
        if (value == null || (specialValue != null && specialValue.equals(value))) {
            if (defaultValues != null && !defaultValues.isEmpty()) {
                return new QueryBuilder.InFilter(expression, defaultValues, operator);
            }
            return null;
        }
        return isExpression
                ? new QueryBuilder.ExpressionFilter(expression, value, operator, placeholder)
                : new QueryBuilder.ColumnFilter(expression, value, operator, placeholder);
    }

    public static class Builder<P, T> {
        private final String name;
        private final String expression;
        private final Function<P, T> valueExtractor;
        private String operator;
        private String placeholder;
        private String specialValue;
        private List<T> defaultValues;
        private boolean isExpression;
        private RuleExpression<P> baseCondition;

        public Builder(String name, String expression, Function<P, T> valueExtractor) {
            this.name = name;
            this.expression = expression;
            this.valueExtractor = valueExtractor;
        }

        public Builder<P, T> withOperator(String operator) {
            this.operator = operator;
            return this;
        }

        public Builder<P, T> withPlaceholder(String placeholder) {
            this.placeholder = placeholder;
            return this;
        }

        public Builder<P, T> withSpecialValue(String specialValue) {
            this.specialValue = specialValue;
            return this;
        }

        public Builder<P, T> withDefaultValues(List<T> defaultValues) {
            this.defaultValues = defaultValues;
            return this;
        }

        public Builder<P, T> asExpression() {
            this.isExpression = true;
            return this;
        }

        public Builder<P, T> withCondition(RuleExpression<P> condition) {
            this.baseCondition = condition;
            return this;
        }

        public Builder<P, T> withPredicateCondition(Predicate<P> predicate) {
            this.baseCondition = RuleExpression.fromPredicate(predicate);
            return this;
        }

        public FilterRule<P, T> build() {
            return new FilterRule<>(name, expression, valueExtractor, operator,
                    placeholder, specialValue, defaultValues, isExpression, baseCondition);
        }
    }
}
