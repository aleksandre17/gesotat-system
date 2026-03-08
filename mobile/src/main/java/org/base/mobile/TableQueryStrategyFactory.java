package org.base.mobile;

import org.base.mobile.strategy.*;
import org.base.mobile.strategy.text.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Component
public class TableQueryStrategyFactory {
    @Autowired
    private ApplicationContext context;

    private final Map<String, Class<? extends TableQueryStrategy<?>>> strategyRegistry = new HashMap<>();

    @PostConstruct
    public void init() {
        // Register strategies by endpoint
        strategyRegistry.put("top-five", TopFiveQueryStrategy.class);
        strategyRegistry.put("sliders-data", SlidersDataQueryStrategy.class);
        strategyRegistry.put("regional-map", RegionalMapQueryStrategy.class);
        strategyRegistry.put("regional-bar", RegionalBarQueryStrategy.class);
        strategyRegistry.put("regional-quantity", RegionalQuantityQueryStrategy.class);
        strategyRegistry.put("equity", EquityQueryStrategy.class);
        strategyRegistry.put("fuel-currency", FuelCurrencyQueryStrategy.class);
        strategyRegistry.put("fuel-quantity", FuelQuantityQueryStrategy.class);
        strategyRegistry.put("fuel-av-price", FuelAvPriceQueryStrategy.class);
        strategyRegistry.put("fuel-column", FuelColumnQueryStrategy.class);
        strategyRegistry.put("fuel-line", FuelLineQueryStrategy.class);
        strategyRegistry.put("full-raiting", FullRaitingQueryStrategy.class);
        strategyRegistry.put("filters", FiltersQueryStrategyImpl.class);
        strategyRegistry.put("road-length", RoadLengthQueryStrategy.class);
        strategyRegistry.put("accidents-main", AccidentsMainQueryStrategy.class);
        strategyRegistry.put("accidents-gender", AccidentsGenderQueryStrategy.class);
        strategyRegistry.put("license-sankey", LicenseSankeyQueryStrategy.class);
        strategyRegistry.put("license-gender", LicenseGenderQueryStrategy.class);
        strategyRegistry.put("license-age", LicenseAgeQueryStrategy.class);
        strategyRegistry.put("license-dual", LicenseDualQueryStrategy.class);
        strategyRegistry.put("compare-line", CompareLineQueryStrategy.class);
        strategyRegistry.put("trade", TradeQueryStrategy.class);
        strategyRegistry.put("sankey", SankeyQueryStrategy.class);
        strategyRegistry.put("area-currency", AreaCurrencyQueryStrategy.class);
        strategyRegistry.put("treemap", TreemapQueryStrategy.class);
        strategyRegistry.put("colors", ColorsQueryStrategy.class);
        strategyRegistry.put("race", RaceQueryStrategy.class);
        strategyRegistry.put("dual", DualQueryStrategy.class);
        strategyRegistry.put("stacked", StackedQueryStrategy.class);

        strategyRegistry.put("ratings-text", RatingsQueryStrategy.class);
        strategyRegistry.put("ratings-full-text", FullRatingsQueryStrategy.class);
        strategyRegistry.put("vehicle-quantity-text", VehicleQuantityQueryStrategy.class);
        strategyRegistry.put("export-Import-text", ExportImportQueryStrategy.class);
        strategyRegistry.put("regional-analysis-text", RegionalAnalysisQueryStrategy.class);
        strategyRegistry.put("compare-text", CompareQueryStrategy.class);
        strategyRegistry.put("fuel-text", FuelQueryStrategy.class);
        strategyRegistry.put("road-text", RoadQueryStrategy.class);
        strategyRegistry.put("accidents-text", AccidentsQueryStrategy.class);
        strategyRegistry.put("licenses-text", LicensesQueryStrategy.class);
    }

    @SuppressWarnings("unchecked")
    public <T> TableQueryStrategy<T> getStrategy(String endpoint, String tableName, Class<T> paramType) {
        Class<? extends TableQueryStrategy<?>> strategyClass = strategyRegistry.get(endpoint);
        if (strategyClass == null) {
            throw new IllegalArgumentException("Unknown endpoint: " + endpoint);
        }

        TableQueryStrategy<T> strategy = (TableQueryStrategy<T>) context.getBean(strategyClass);
        return strategy.setTableName(tableName);
    }

    /**
     * Maps table name to trade strategy bean name (for legacy support).
     */
    private String getTradeStrategyBeanName(String tableName) {
        return switch (tableName) {
            case "vehicle_imp_exp" -> "vehicleImpExpStrategy";
            case "others_imp_exp" -> "othersImpExpStrategy";
            default -> "combinedImpExpStrategy";
        };
    }
}