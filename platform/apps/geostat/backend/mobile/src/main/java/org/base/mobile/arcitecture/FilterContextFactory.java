package org.base.mobile.arcitecture;

import org.base.mobile.params.*;
import org.base.mobile.params.text.RatingsParams;

import java.util.List;

public class FilterContextFactory {

    public static <P> FilterContext<P> createDefaultContext(List<FilterContextConfig<P>> configs) {
        FilterContext.Builder<P> builder = new FilterContext.Builder<>();
        for (FilterContextConfig<P> config : configs) {
            for (FilterRule<P, ?> rule : config.getRules()) {
                builder.addRule(config.getTableName(), rule);
            }
        }
        return builder.build();
    }

    public static class FilterContextConfig<P> {
        private final String tableName;
        private final List<FilterRule<P, ?>> rules;

        public FilterContextConfig(String tableName, List<FilterRule<P, ?>> rules) {
            this.tableName = tableName;
            this.rules = rules != null ? List.copyOf(rules) : List.of();
        }

        public String getTableName() {
            return tableName;
        }

        public List<FilterRule<P, ?>> getRules() {
            return rules;
        }
    }

    public static FilterContext<RaceParams> defaultRaceContext() {
        return defaultContext();
    }

    public static FilterContext<TreemapParams> defaultTreeMapContext() {
        return defaultContext();
    }

    public static FilterContext<ColorsParams> defaultColorMapContext() {
        return defaultContext();
    }

    public static FilterContext<CompareParams> defaultCommonContext() {
        return createDefaultContext(List.of());
    }

    public static FilterContext<RatingsParams> defaultCommonContextText() {
        return createDefaultContext(List.of());
    }

    private static <P extends CommonParams> FilterContext<P> defaultContext() {
        return createDefaultContext(List.of(
                new FilterContextConfig<>("[dbo].[eoyes]", List.of(
                        new FilterRule.Builder<P, Integer>(
                                "year", "t.year", CommonParams::getYear).build()
                )),
                new FilterContextConfig<>("[dbo].[auto_main]", List.of(
                        new FilterRule.Builder<P, Integer>(
                                "year", "t.year", CommonParams::getYear).build(),
                        new FilterRule.Builder<P, String>(
                                "quarter", "CAST(t.quarter AS NVARCHAR)", CommonParams::getQuarter)
                                .withSpecialValue("99")
                                .withDefaultValues(List.of("1", "2", "3", "4"))
                                .withOperator(" IN ")
                                .withPlaceholder("(?)")
                                .asExpression()
                                .build()
                ))
        ));
    }

}
