package org.base.mobile.arcitecture;

import jakarta.servlet.FilterConfig;
import org.base.mobile.params.CommonParams;
import org.base.mobile.params.CompareParams;
import org.base.mobile.params.RaceParams;
import org.base.core.service.QueryBuilder;

import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

@FunctionalInterface
public interface FilterBuilder<P> extends BiFunction<P, RuleExpression<P>, QueryBuilder.Filter> {
    QueryBuilder.Filter apply(P params, RuleExpression<P> overrideCondition);


    static FilterBuilder<CompareParams> yearFilter() {
        return (params, overrideCondition) -> {
            if (overrideCondition != null && !overrideCondition.evaluate(params)) {
                return null;
            }
            String year = params.getYearOfProduction1(); // Adapt to CompareParams
            return year != null ? new QueryBuilder.ColumnFilter("t.year_of_production", year, "=", "?") : null;
        };
    }

//    static FilterBuilder<CompareParams> quarterFilter(FilterConfig config) {
//        Objects.requireNonNull(config, "FilterConfig cannot be null");
//        return (params, overrideCondition) -> {
//            if (overrideCondition != null && !overrideCondition.evaluate(params)) {
//                return null;
//            }
//            // Assuming no getQuarter() in CompareParams; placeholder logic
//            String quarter = null; // Replace with actual parameter if available
//            if (quarter == null || quarter.equals(config.getAllQuartersValue())) {
//                List<String> defaultQuarters = config.getDefaultQuarters();
//                if (defaultQuarters != null && !defaultQuarters.isEmpty()) {
//                    return new QueryBuilder.InFilter("t.quarter", defaultQuarters, "IN");
//                }
//                return null;
//            }
//            return new QueryBuilder.ExpressionFilter("CAST(t.quarter AS NVARCHAR)", quarter, "=", "?");
//        };
//    }
}
