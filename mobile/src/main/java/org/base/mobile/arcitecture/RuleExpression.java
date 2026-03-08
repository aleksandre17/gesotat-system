package org.base.mobile.arcitecture;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public interface RuleExpression<P> {
    boolean evaluate(P params);

    default RuleExpression<P> and(RuleExpression<P> other) {
        return new AndExpression<>(List.of(this, other));
    }

    default RuleExpression<P> or(RuleExpression<P> other) {
        return new OrExpression<>(List.of(this, other));
    }

    static <P> RuleExpression<P> fromPredicate(Predicate<P> predicate) {
        return predicate::test;
    }

    static <P> RuleExpression<P> alwaysTrue() {
        return params -> true;
    }
}

class AndExpression<P> implements RuleExpression<P> {
    private final List<RuleExpression<P>> expressions;

    AndExpression(List<RuleExpression<P>> expressions) {
        this.expressions = new ArrayList<>(expressions);
    }

    @Override
    public boolean evaluate(P params) {
        return expressions.stream().allMatch(expr -> expr.evaluate(params));
    }
}

class OrExpression<P> implements RuleExpression<P> {
    private final List<RuleExpression<P>> expressions;

    OrExpression(List<RuleExpression<P>> expressions) {
        this.expressions = new ArrayList<>(expressions);
    }

    @Override
    public boolean evaluate(P params) {
        return expressions.stream().anyMatch(expr -> expr.evaluate(params));
    }
}
