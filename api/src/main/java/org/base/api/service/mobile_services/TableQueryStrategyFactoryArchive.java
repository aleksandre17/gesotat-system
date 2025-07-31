package org.base.api.service.mobile_services;

import org.base.api.service.mobile_services.params.*;
import org.base.api.service.mobile_services.strategy.*;
import org.base.core.service.QueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.util.List;

//@Component
public class TableQueryStrategyFactoryArchive {
    @Autowired
    private ApplicationContext context;

    @SuppressWarnings("unchecked")
    public <T> TableQueryStrategy<T> getStrategy(String endpoint, String tableName, Class<T> paramType) {
        String beanName = switch (endpoint) {
            case "top-five" -> "topFiveStrategy";
            case "sliders-data" -> "slidersDataStrategy";
            case "regional-map" -> "regionalMapStrategy";
            case "regional-bar" -> "regionalBarStrategy";
            case "regional-quantity" -> "regionalQuantityStrategy";
            case "equity" -> "equityStrategy";
            case "fuel-currency" -> "fuelCurrencyStrategy";
            case "fuel-quantity" -> "fuelQuantityStrategy";
            case "fuel-av-price" -> "fuelAvPriceStrategy";
            case "fuel-column" -> "fuelColumnStrategy";
            case "fuel-line" -> "fuelLineStrategy";
            case "full-raiting" -> "fullRaitingStrategy";
            case "filters" -> "filtersStrategy";
            case "road-length" -> "roadLengthStrategy";
            case "accidents-main" -> "accidentsMainStrategy";
            case "accidents-gender" -> "accidentsGenderStrategy";
            case "license-sankey" -> "licenseSankeyStrategy";
            case "license-gender" -> "licenseGenderStrategy";
            case "license-age" -> "licenseAgeStrategy";
            case "license-dual" -> "licenseDualStrategy";
            case "compare-line" -> "compareLineStrategy";
            case "trade" -> "tradeStrategy";
            case "sankey" -> "sankeyStrategy";
            case "quantity" -> "quantityStrategy";
            //case "quantity" -> getTradeStrategyBeanName(tableName); //, "area-currency"
            case "area-currency" -> "areaCurrencyStrategy";
            case "treemap" -> "treemapStrategy";
            case "colors" -> "colorsStrategy";
            case "race" -> "raceStrategy";
            case "dual" -> "dualStrategy";
            case "stacked" -> "stackedStrategy";

            default -> throw new IllegalArgumentException("Unknown endpoint: " + endpoint);
        };

        if (beanName.equals("regionalMapStrategy") || beanName.equals("regionalBarStrategy") ||
                beanName.equals("regionalQuantityStrategy") || beanName.equals("equityStrategy") ||
                beanName.equals("fuelCurrencyStrategy") || beanName.equals("fuelQuantityStrategy") ||
                beanName.equals("fuelAvPriceStrategy") || beanName.equals("fuelColumnStrategy") ||
                beanName.equals("fuelLineStrategy") || beanName.equals("roadLengthStrategy") ||
                beanName.equals("accidentsMainStrategy") || beanName.equals("accidentsGenderStrategy") ||
                beanName.equals("licenseSankeyStrategy") || beanName.equals("licenseGenderStrategy") ||
                beanName.equals("licenseAgeStrategy") || beanName.equals("licenseDualStrategy") ||
                beanName.equals("topFiveStrategy") || beanName.equals("slidersDataStrategy") ||
                beanName.equals("fullRaitingStrategy") || beanName.equals("filtersStrategy") || beanName.equals("sankeyStrategy")
                ||beanName.equals("treemapStrategy") || beanName.equals("colorsStrategy") || beanName.equals("raceStrategy") || beanName.equals("dualStrategy")
                || beanName.equals("stackedStrategy") || beanName.equals("areaCurrencyStrategy") || beanName.equals("quantityStrategy")) {
            return (TableQueryStrategy<T>) new DynamicStrategy(beanName, tableName);
        }
        return context.getBean(beanName, TableQueryStrategy.class);
    }

    /**
     * Maps table name to trade strategy bean name.
     */
    private String getTradeStrategyBeanName(String tableName) {
        return switch (tableName) {
            case "vehicle_imp_exp" -> "vehicleImpExpStrategy";
            case "others_imp_exp" -> "othersImpExpStrategy";
            default -> "combinedImpExpStrategy";
        };
    }

    /**
     * Dynamic strategy for endpoints with table-specific configurations.
     */
    private static class DynamicStrategy<T> implements TableQueryStrategy<T> {
        private final String strategyName;
        private final String tableName;

        DynamicStrategy(String strategyName, String tableName) {
            this.strategyName = strategyName;
            this.tableName = tableName;
        }

        @Override
        public String getTableName() {
            return tableName;
        }

        @Override
        public void configureQuery(QueryBuilder query, QueryParams<T> params) {
            switch (strategyName) {
                case "regionalMapStrategy" ->
                        new RegionalMapQueryStrategy().setTableName(tableName).configureQuery((QueryBuilder) query, (QueryParams<RegionalMapParams>) params);
                case "regionalBarStrategy" ->
                        new RegionalBarQueryStrategy().setTableName(tableName).configureQuery((QueryBuilder) query, (QueryParams<RegionalBarParams>) params);
                case "regionalQuantityStrategy" ->
                        new RegionalQuantityQueryStrategy().setTableName(tableName).configureQuery((QueryBuilder) query, (QueryParams<RegionalQuantityParams>) params);
                case "equityStrategy" ->
                        new EquityQueryStrategy().setTableName(tableName).configureQuery(query, (QueryParams<EquityParams>) params);
                case "fuelCurrencyStrategy" ->
                        new FuelCurrencyQueryStrategy().setTableName(tableName).configureQuery(query, (QueryParams<FuelCurrencyParams>) params);
                case "fuelQuantityStrategy" ->
                        new FuelQuantityQueryStrategy().setTableName(tableName).configureQuery(query, (QueryParams<FuelQuantityParams>) params);
                case "fuelAvPriceStrategy" ->
                        new FuelAvPriceQueryStrategy().setTableName(tableName).configureQuery(query, (QueryParams<FuelAvPriceParams>) params);
                case "fuelColumnStrategy" ->
                        new FuelColumnQueryStrategy().setTableName(tableName).configureQuery(query, (QueryParams<FuelColumnParams>) params);
                case "fuelLineStrategy" ->
                        new FuelLineQueryStrategy().setTableName(tableName).configureQuery(query, (QueryParams<FuelLineParams>) params);
                case "roadLengthStrategy" ->
                        new RoadLengthQueryStrategy().setTableName(tableName).configureQuery(query, (QueryParams<RoadLengthParams>) params);

                case "accidentsMainStrategy" ->
                        new AccidentsMainQueryStrategy().setTableName(tableName).configureQuery(query, (QueryParams<AccidentsMainParams>) params);

                case "accidentsGenderStrategy" ->
                        new AccidentsGenderQueryStrategy().setTableName(tableName).configureQuery(query, (QueryParams<AccidentsGenderParams>) params);

                case "licenseSankeyStrategy" ->
                        new LicenseSankeyQueryStrategy().setTableName(tableName).configureQuery(query, (QueryParams<LicenseSankeyParams>) params);

                case "licenseGenderStrategy" ->
                        new LicenseGenderQueryStrategy().setTableName(tableName).configureQuery(query, (QueryParams<LicenseGenderParams>) params);
                case "licenseAgeStrategy" ->
                        new LicenseAgeQueryStrategy().setTableName(tableName).setTableName(tableName).configureQuery(query, (QueryParams<LicenseAgeParams>) params);
                case "licenseDualStrategy" ->
                        new LicenseDualQueryStrategy().setTableName(tableName).configureQuery(query, (QueryParams<LicenseDualParams>) params);

                case "topFiveStrategy" ->
                        new TopFiveQueryStrategy().setTableName(tableName).configureQuery(query, (QueryParams<TopFiveParams>) params);
                case "slidersDataStrategy" ->
                        new SlidersDataQueryStrategy().setTableName(tableName).configureQuery(query, (QueryParams<SlidersDataParams>) params);

                case "fullRaitingStrategy" ->
                        new FullRaitingQueryStrategy().setTableName(tableName).configureQuery(query, (QueryParams<FullRaitingParams>) params);
                case "filtersStrategy" ->
                        new FiltersQueryStrategy() {
                            @Override
                            public String getTableName() {
                                return "";
                            }

                            @Override
                            public void configureQuery(QueryBuilder query, QueryParams<FiltersParams> params) {

                            }

                            @Override
                            public List<String> getAttributes() {
                                return List.of();
                            }

                            @Override
                            public List<String> getGroupBy() {
                                return List.of();
                            }

                            @Override
                            public boolean isCombinedQuery() {
                                return false;
                            }

                            @Override
                            public TableQueryStrategy<FiltersParams> setTableName(String tableName) {
                                return null;
                            }

                            @Override
                            public QueryBuilder configureTopModelQuery(QueryParams<FiltersParams> params) {
                                return null;
                            }
                        }.setTableName(tableName).configureQuery(query, (QueryParams<FiltersParams>) params);

                case "sankeyStrategy" ->
                        new SankeyQueryStrategy().setTableName(tableName).configureQuery(query, (QueryParams<SankeyParams>) params);

                case "treemapStrategy" ->
                        new TreemapQueryStrategy().setTableName(tableName).configureQuery(query, (QueryParams<TreemapParams>) params);

                case "colorsStrategy" ->
                        new ColorsQueryStrategy().setTableName(tableName).configureQuery(query, (QueryParams<ColorsParams>) params);

                case "raceStrategy" ->
                        new RaceQueryStrategy().setTableName(tableName).configureQuery(query, (QueryParams<RaceParams>) params);


                case "dualStrategy" ->
                        new DualQueryStrategy().setTableName(tableName).configureQuery(query, (QueryParams<DualParams>) params);

                case "stackedStrategy" ->
                        new StackedQueryStrategy().setTableName(tableName).configureQuery(query, (QueryParams<StackedParams>) params);

                case "areaCurrencyStrategy" ->
                        new AreaCurrencyQueryStrategy().setTableName(tableName).configureQuery(query, (QueryParams<AreaQuantityOrCurrencyParams>) params);


//                case "quantityStrategy" ->
//                        new QuantityQueryStrategy().setTableName(tableName).configureQuery(query, (QueryParams<QuantityParams>) params);
                default -> throw new IllegalStateException("Unknown strategy: " + strategyName);
            }
        }

        @Override
        public List<String> getAttributes() {
            return switch (strategyName) {
                case "regionalMapStrategy" -> new RegionalMapQueryStrategy().setTableName(tableName).getAttributes();
                case "regionalBarStrategy" -> new RegionalBarQueryStrategy().setTableName(tableName).getAttributes();
                case "regionalQuantityStrategy" -> new RegionalQuantityQueryStrategy().setTableName(tableName).getAttributes();
                case "equityStrategy" -> new EquityQueryStrategy().setTableName(tableName).getGroupBy();
                case "fuelCurrencyStrategy" -> new FuelCurrencyQueryStrategy().setTableName(tableName).getAttributes();
                case "fuelQuantityStrategy" -> new FuelQuantityQueryStrategy().setTableName(tableName).getAttributes();
                case "fuelAvPriceStrategy" -> new FuelAvPriceQueryStrategy().setTableName(tableName).getAttributes();
                case "fuelColumnStrategy" -> new FuelColumnQueryStrategy().setTableName(tableName).getAttributes();
                case "fuelLineStrategy" -> new FuelLineQueryStrategy().setTableName(tableName).getAttributes();
                case "roadLengthStrategy" -> new RoadLengthQueryStrategy().setTableName(tableName).getAttributes();
                case "accidentsMainStrategy" -> new AccidentsMainQueryStrategy().setTableName(tableName).getAttributes();
                case "accidentsGenderStrategy" -> new AccidentsGenderQueryStrategy().setTableName(tableName).getAttributes();
                case "licenseSankeyStrategy" -> new LicenseSankeyQueryStrategy().setTableName(tableName).getAttributes();
                case "licenseGenderStrategy" -> new LicenseGenderQueryStrategy().setTableName(tableName).getAttributes();
                case "licenseAgeStrategy" -> new LicenseAgeQueryStrategy().setTableName(tableName).getAttributes();
                case "licenseDualStrategy" -> new LicenseDualQueryStrategy().setTableName(tableName).getAttributes();
                case "topFiveStrategy" -> new TopFiveQueryStrategy().setTableName(tableName).getAttributes();
                case "slidersDataStrategy" -> new SlidersDataQueryStrategy().setTableName(tableName).getAttributes();
                case "fullRaitingStrategy" -> new FullRaitingQueryStrategy().setTableName(tableName).getAttributes();
                case "filtersStrategy" -> new FiltersQueryStrategy() {
                    @Override
                    public String getTableName() {
                        return "";
                    }

                    @Override
                    public void configureQuery(QueryBuilder query, QueryParams<FiltersParams> params) {

                    }

                    @Override
                    public List<String> getAttributes() {
                        return List.of();
                    }

                    @Override
                    public List<String> getGroupBy() {
                        return List.of();
                    }

                    @Override
                    public boolean isCombinedQuery() {
                        return false;
                    }

                    @Override
                    public TableQueryStrategy<FiltersParams> setTableName(String tableName) {
                        return null;
                    }

                    @Override
                    public QueryBuilder configureTopModelQuery(QueryParams<FiltersParams> params) {
                        return null;
                    }
                }.setTableName(tableName).getAttributes();
                case "sankeyStrategy" -> new SankeyQueryStrategy().setTableName(tableName).getAttributes();
                case "treemapStrategy" -> new TreemapQueryStrategy().setTableName(tableName).getAttributes();
                case "colorsStrategy" -> new ColorsQueryStrategy().setTableName(tableName).getAttributes();
                case "raceStrategy" -> new RaceQueryStrategy().setTableName(tableName).getAttributes();
                case "dualStrategy" -> new DualQueryStrategy().setTableName(tableName).getAttributes();
                case "stackedStrategy" -> new StackedQueryStrategy().setTableName(tableName).getAttributes();
                case "areaCurrencyStrategy" -> new AreaCurrencyQueryStrategy().setTableName(tableName).getAttributes();

                default -> List.of();
            };
        }

        @Override
        public List<String> getGroupBy() {
            return switch (strategyName) {
                case "regionalMapStrategy" -> new RegionalMapQueryStrategy().setTableName(tableName).getGroupBy();
                case "regionalBarStrategy" -> new RegionalBarQueryStrategy().setTableName(tableName).getGroupBy();
                case "regionalQuantityStrategy" -> new RegionalQuantityQueryStrategy().setTableName(tableName).getGroupBy();
                case "equityStrategy" -> new EquityQueryStrategy().setTableName(tableName).getGroupBy();
                case "fuelCurrencyStrategy" -> new FuelCurrencyQueryStrategy().setTableName(tableName).getGroupBy();
                case "fuelQuantityStrategy" -> new FuelQuantityQueryStrategy().setTableName(tableName).getGroupBy();
                case "fuelAvPriceStrategy" -> new FuelAvPriceQueryStrategy().setTableName(tableName).getGroupBy();
                case "fuelColumnStrategy" -> new FuelColumnQueryStrategy().setTableName(tableName).getGroupBy();
                case "fuelLineStrategy" -> new FuelLineQueryStrategy().setTableName(tableName).getGroupBy();
                case "roadLengthStrategy" -> new RoadLengthQueryStrategy().setTableName(tableName).getGroupBy();
                case "accidentsMainStrategy" -> new AccidentsMainQueryStrategy().setTableName(tableName).getGroupBy();
                case "accidentsGenderStrategy" -> new AccidentsGenderQueryStrategy().setTableName(tableName).getGroupBy();
                case "licenseSankeyStrategy" -> new LicenseSankeyQueryStrategy().setTableName(tableName).getGroupBy();
                case "licenseGenderStrategy" -> new LicenseGenderQueryStrategy().setTableName(tableName).getGroupBy();
                case "licenseAgeStrategy" -> new LicenseAgeQueryStrategy().setTableName(tableName).getGroupBy();
                case "licenseDualStrategy" -> new LicenseDualQueryStrategy().setTableName(tableName).getGroupBy();
                case "topFiveStrategy" -> new TopFiveQueryStrategy().setTableName(tableName).getGroupBy();
                case "slidersDataStrategy" -> new SlidersDataQueryStrategy().setTableName(tableName).getGroupBy();
                case "fullRaitingStrategy" -> new FullRaitingQueryStrategy().setTableName(tableName).getGroupBy();
                case "filtersStrategy" -> new FiltersQueryStrategy() {
                    @Override
                    public String getTableName() {
                        return "";
                    }

                    @Override
                    public void configureQuery(QueryBuilder query, QueryParams<FiltersParams> params) {

                    }

                    @Override
                    public List<String> getAttributes() {
                        return List.of();
                    }

                    @Override
                    public List<String> getGroupBy() {
                        return List.of();
                    }

                    @Override
                    public boolean isCombinedQuery() {
                        return false;
                    }

                    @Override
                    public TableQueryStrategy<FiltersParams> setTableName(String tableName) {
                        return null;
                    }

                    @Override
                    public QueryBuilder configureTopModelQuery(QueryParams<FiltersParams> params) {
                        return null;
                    }
                }.setTableName(tableName).getGroupBy();
                case "sankeyStrategy" -> new SankeyQueryStrategy().setTableName(tableName).getGroupBy();
                case "treemapStrategy" -> new TreemapQueryStrategy().setTableName(tableName).getGroupBy();
                case "colorsStrategy" -> new ColorsQueryStrategy().setTableName(tableName).getGroupBy();
                //case "race" -> "raceStrategy";
                case "raceStrategy" -> new RaceQueryStrategy().setTableName(tableName).getGroupBy();
                case "dualStrategy" -> new DualQueryStrategy().setTableName(tableName).getGroupBy();
                case "stackedStrategy" -> new StackedQueryStrategy().setTableName(tableName).getGroupBy();
                case "areaCurrencyStrategy" -> new AreaCurrencyQueryStrategy().setTableName(tableName).getGroupBy();

                default -> List.of();
            };
        }

        @Override
        public boolean isCombinedQuery() {
            return switch (strategyName) {
                case "equityStrategy", "fuelCurrencyStrategy", "fuelQuantityStrategy",
                     "fuelAvPriceStrategy", "fuelLineStrategy", "licenseSankeyStrategy",
                     "licenseDualStrategy", "sankeyStrategy", "treemapStrategy", "colorsStrategy",
                     "raceStrategy", "stackedStrategy", "areaCurrencyStrategy", "quantityStrategy" -> true;
                default -> false;
            };
        }

        @Override
        public TableQueryStrategy<T> setTableName(String tableName) {
            return null;
        }

    }
}
