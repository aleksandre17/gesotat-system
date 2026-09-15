package org.base.mobile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AllArgsConstructor;
import org.base.mobile.TableConfigText;
import org.base.mobile.repository.GlobalRepository;
import org.base.mobile.arcitecture.SelectorBuilder;
import org.base.mobile.dto.text.SelectorDTO;
import org.base.mobile.dto.text.SelectorValue;
import org.base.mobile.params.QueryParams;
import org.base.mobile.params.text.*;
import org.base.mobile.strategy.TableQueryStrategy;
import org.base.mobile.strategy.text.RegionalAnalysisQueryStrategy;
import org.base.core.exeption.extend.ApiException;
import org.base.core.service.QueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static org.base.mobile.TableConfigText.DEFAULT_TABLE_NAME;

@AllArgsConstructor
@Service
public class MobileServiceText {

    @Qualifier("secondaryJdbcTemplate")
    @Autowired
    private JdbcTemplate jdbcTemplate;
    private GlobalRepository globalRepository;
    private TableQueryStrategyFactory strategyFactory;
    private LanguageService languageService;
    private SelectorBuilder selectorBuilder;
    private ObjectMapper objectMapper;

    public String getQuarter(String key) {
        return switch (key) {
            case "1" -> "I";
            case "2" -> "II";
            case "3" -> "III";
            case "4" -> "IV";
            default -> key;
        };
    }

    @Transactional(readOnly = true)
    public List<SelectorDTO> getRatings(String tableName, RatingsParams params) {
        try {
            if (tableName == null) {
                tableName = DEFAULT_TABLE_NAME;
            }
            TableConfigText.TableMetadata tableConfig = TableConfigText.TABLES.getOrDefault(tableName, TableConfigText.TABLES.get(DEFAULT_TABLE_NAME));
            params.setTableName(tableName);

            QueryParams<RatingsParams> queryParams = new QueryParams<>("", params);
            TableQueryStrategy<RatingsParams> strategy = strategyFactory.getStrategy("ratings-text", tableName, RatingsParams.class);

            QueryBuilder query = new QueryBuilder();
            strategy.setTableName(tableName);
            strategy.configureQuery(query, queryParams);

            // Execute query
            List<Map<String, Object>> results = globalRepository.executeAreaCurrencyQuery(query, tableName);

            // Transform results
            boolean isMainTreeMap = Objects.equals(tableName, DEFAULT_TABLE_NAME);
            List<SelectorDTO> selectors = new ArrayList<>();
            Map<String, List<Map<String, Object>>> grouped = results.stream()
                    .collect(Collectors.groupingBy(
                            row -> (String) row.get("selector_type"),
                            Collectors.toList()
                    ));

            // Year selector
            List<Map<Object, Object>> yearRows = (List<Map<Object, Object>>) (Object) grouped.getOrDefault("year", List.of());

            // Reverse the year rows (newest year first)
            List<Map<Object, Object>> reversedYearRows = new ArrayList<>(yearRows);
            Collections.reverse(reversedYearRows);

            Integer defaultYear = params.getYear() != null ? params.getYear() :
                    (Integer) yearRows.stream()
                            .findFirst()
                            .map(row -> row.get("default_code"))
                            .orElse(TableConfigText.DEFAULT_YEAR);

            SelectorDTO yearSelector = new SelectorDTO(
                    "selector",
                    languageService.getTranslation(params.getLang(), tableConfig.columns().get("year").translationKey()),
                    new SelectorValue(defaultYear, defaultYear),
                    reversedYearRows.stream()
                            .map(row -> new SelectorValue(
                                    Integer.valueOf((String) row.get("name")),
                                    Integer.valueOf((String) row.get("code"))
                            ))
                            .collect(Collectors.toList())
            );

            selectors.add(yearSelector);

            // Quarter selector (for MainTreeMap)
            if (isMainTreeMap) {
                List<Map<String, Object>> quarterRows = grouped.getOrDefault("quarter", List.of());
                List<SelectorValue> quarterValues = quarterRows.stream()
                        .map(row -> new SelectorValue(
                                getQuarter(String.valueOf(row.get("name"))),
                                languageService.getQuarter((String) row.get("code"), params.getLang())
                        ))
                        .collect(Collectors.toList());
                quarterValues.add(0, new SelectorValue(languageService.getMain("main.All", params.getLang()), "99")); // Add "all" option
                SelectorDTO quarterSelector = new SelectorDTO(
                        "selector",
                        languageService.getTranslation(params.getLang(), tableConfig.columns().get("quarter").translationKey()),
                        new SelectorValue(languageService.getMain("main.All", params.getLang()), "99"),
                        quarterValues
                );
                selectors.add(quarterSelector);
            }

            return selectors;
        } catch (Exception e) {
            throw new ApiException("Failed to retrieve ratings data", String.valueOf(e));
        }
    }


    public JsonNode getTopFive(String lang) {
        try {
            if (lang != null && !lang.matches("^[a-zA-Z]{2}$")) {
                throw new IllegalArgumentException("Invalid language code");
            }
            return languageService.getTopFive(lang);
        } catch (Exception e) {
            throw new ApiException("Failed to retrieve top five data", String.valueOf(e));
        }
    }

    private JsonNode createNode(Object name, Object code) {
        ObjectNode node = objectMapper.createObjectNode();
        if (name instanceof String) {
            node.put("name", (String) name);
        } else {
            node.put("name", (Integer) name);
        }
        if (code instanceof String) {
            node.put("code", (String) code);
        } else {
            node.put("code", (Integer) code);
        }
        return node;
    }

    @Transactional(readOnly = true)
    public JsonNode getFullRatings(String tableName, String lang) {
        try {
            //String cacheKey = "full_ratings:" + tableName + ":" + (lang != null ? lang : "en");
            //JsonNode cached = redisTemplate.opsForValue().get(cacheKey);
            //if (cached != null) {
                //return cached;
           // }

            if (tableName == null) {
                tableName = DEFAULT_TABLE_NAME;
            }
            TableConfigText.TableMetadata tableConfig = TableConfigText.TABLES.getOrDefault(tableName, TableConfigText.TABLES.get(DEFAULT_TABLE_NAME));

            FullRatingsParams params = new FullRatingsParams();
            params.setTableName(tableName);
            params.setLang(lang);

            QueryParams<FullRatingsParams> queryParams = new QueryParams<>("", params);
            TableQueryStrategy<FullRatingsParams> strategy = strategyFactory.getStrategy("ratings-full-text", tableName, FullRatingsParams.class);

            QueryBuilder query = new QueryBuilder();
            strategy.setTableName(tableName);
            strategy.configureQuery(query, queryParams);

            List<Map<String, Object>> results = globalRepository.executeAreaCurrencyQuery(query, tableName);

            ArrayNode response = objectMapper.createArrayNode();

            // Year selector
            ObjectNode yearSelector = objectMapper.createObjectNode();
            yearSelector.put("type", "selector");
            yearSelector.put("placeholder", languageService.getMain("main.year", lang));
            ArrayNode yearValues = objectMapper.createArrayNode();
            String defaultYear = results.isEmpty() ? String.valueOf(TableConfigText.DEFAULT_YEAR) : (String) results.get(0).get("default_code");
            yearSelector.set("default", createNode(defaultYear, defaultYear));
            for (Map<String, Object> row : results) {
                yearValues.add(createNode(Integer.valueOf((String) row.get("name")), Integer.valueOf((String) row.get("code"))));
            }
            yearSelector.set("selectValues", yearValues);
            response.add(yearSelector);

            // Sort selector
            ObjectNode sortSelector = objectMapper.createObjectNode();
            sortSelector.put("type", "selector");
            sortSelector.put("placeholder", languageService.getMain("main.sort", lang));
            sortSelector.set("default", languageService.getDefaultSort(lang));
            sortSelector.set("selectValues", languageService.getSortingItems(lang));
            response.add(sortSelector);

            // Search
            ObjectNode search = objectMapper.createObjectNode();
            search.put("type", "search");
            search.put("placeholder", languageService.getMain("main.search", lang));
            response.add(search);

            // Top five
            response.add(languageService.getTopFive(lang));

            //redisTemplate.opsForValue().set(cacheKey, response, Duration.ofHours(1));
            return response;
        } catch (Exception e) {
            throw new ApiException("Failed to retrieve full ratings data", String.valueOf(e));
        }
    }

    @Transactional(readOnly = true)
    public JsonNode getVehicleQuantity(String tableName, String lang) {
        try {
            //String cacheKey = "vehicle_quantity:" + tableName + ":" + (lang != null ? lang : "en");
            //JsonNode cached = redisTemplate.opsForValue().get(cacheKey);
            //if (cached != null) {
                //return cached;
            //}

            if (tableName == null) {
                tableName = TableConfigText.DEFAULT_VEHICLES_TABLE_NAME;
            }
            TableConfigText.TableMetadata tableConfig = TableConfigText.TABLES.getOrDefault(tableName, TableConfigText.TABLES.get(TableConfigText.DEFAULT_VEHICLES_TABLE_NAME));

            VehicleQuantityParams params = new VehicleQuantityParams();
            params.setTableName(tableName);
            params.setLang(lang);

            QueryParams<VehicleQuantityParams> queryParams = new QueryParams<>("", params);
            TableQueryStrategy<VehicleQuantityParams> strategy = strategyFactory.getStrategy("vehicle-quantity-text", tableName, VehicleQuantityParams.class);

            QueryBuilder query = new QueryBuilder();
            strategy.setTableName(tableName);
            strategy.configureQuery(query, queryParams);

            List<Map<String, Object>> results = globalRepository.executeAreaCurrencyQuery(query, tableName);

            ArrayNode response = objectMapper.createArrayNode();
            ObjectNode typesSelector = objectMapper.createObjectNode();
            typesSelector.put("type", "selector");
            typesSelector.put("placeholder", "");
            ArrayNode selectValues = objectMapper.createArrayNode();
            for (Map<String, Object> row : results) {
                selectValues.add(createNode((String) row.get("name"), row.get("code")));
            }
            typesSelector.set("selectValues", selectValues);
            response.add(typesSelector);

            //redisTemplate.opsForValue().set(cacheKey, response, Duration.ofHours(1));
            return response;
        } catch (Exception e) {
            throw new ApiException("Failed to retrieve vehicle quantity data", String.valueOf(e));
        }
    }

    @Transactional(readOnly = true)
    public JsonNode getStackedArea(String lang) {
        try {
            //String cacheKey = "stacked_area:" + (lang != null ? lang : "en");
            //JsonNode cached = redisTemplate.opsForValue().get(cacheKey);
            //if (cached != null) {
                //return cached;
            //}


            ObjectNode typeSelector = objectMapper.createObjectNode();
            typeSelector.put("type", "checkboxes");
            typeSelector.put("placeholder", languageService.getMain("main.indicator", lang));
            typeSelector.set("default", languageService.getDefaultStackedAreaIndicator(lang));
            typeSelector.set("selectValues", languageService.getStackedAreaIndicators(lang));

            //redisTemplate.opsForValue().set(cacheKey, response, Duration.ofHours(1));
            return typeSelector;
        } catch (Exception e) {
            throw new ApiException("Failed to retrieve stacked area data", String.valueOf(e));
        }
    }

    @Transactional(readOnly = true)
    public JsonNode getExportImport(String tableName, String lang, String type) {
        try {
//            String cacheKey = "export_import:" + tableName + ":" + (lang != null ? lang : "en") + ":" + type;
//            JsonNode cached = redisTemplate.opsForValue().get(cacheKey);
//            if (cached != null) {
//                return cached;
//            }
//
//            if (tableName == null) {
//                tableName = "[dbo].[FuelCl]";
//            }

            ArrayNode response = objectMapper.createArrayNode();

            // Export/Import selector
            ObjectNode exportImportSelector = objectMapper.createObjectNode();
            exportImportSelector.put("type", "selector");
            exportImportSelector.put("placeholder", "");
            exportImportSelector.set("default", languageService.getDefaultExportImport(lang));
            exportImportSelector.set("selectValues", languageService.getExportImport(lang));
            response.add(exportImportSelector);

            // Type selector
            ObjectNode typeSelector = objectMapper.createObjectNode();
            typeSelector.put("type", "selector");
            typeSelector.put("placeholder", "");
            typeSelector.set("default", languageService.getDefaultTypeSort(lang));
            typeSelector.set("selectValues", languageService.getTypeSort(lang));
            response.add(typeSelector);

            // Fuel selector (conditional)
            if ("1".equals(type)) {
                ExportImportParams params = new ExportImportParams();
                params.setTableName(tableName);
                params.setLang(lang);
                params.setType(type);

                QueryParams<ExportImportParams> queryParams = new QueryParams<>("", params);
                TableQueryStrategy<ExportImportParams> strategy = strategyFactory.getStrategy("export-Import-text", tableName, ExportImportParams.class);

                QueryBuilder query = new QueryBuilder();
                strategy.setTableName(tableName);
                strategy.configureQuery(query, queryParams);

                List<Map<String, Object>> results = globalRepository.executeAreaCurrencyQuery(query, tableName);

                ObjectNode fuelSelector = objectMapper.createObjectNode();
                fuelSelector.put("type", "selector");
                fuelSelector.put("placeholder", languageService.getMain("main.fuel_type", lang));
                ArrayNode fuelValues = objectMapper.createArrayNode();
                for (Map<String, Object> row : results) {
                    fuelValues.add(createNode((String) row.get("name"), row.get("code")));
                }
                fuelSelector.set("selectValues", fuelValues);
                response.add(fuelSelector);

                // Vehicle selector
                ObjectNode vehicleSelector = objectMapper.createObjectNode();
                vehicleSelector.put("type", "selector");
                vehicleSelector.put("placeholder", "");
                vehicleSelector.set("selectValues", languageService.getVehicleSort(lang));
                response.add(vehicleSelector);
            }

            // Currency switch
            ObjectNode currencySelector = objectMapper.createObjectNode();
            currencySelector.put("type", "switch");
            currencySelector.put("placeholder", "");
            currencySelector.set("selectValues", languageService.getCurrencySort(lang));
            response.add(currencySelector);

            //redisTemplate.opsForValue().set(cacheKey, response, Duration.ofHours(1));
            return response;
        } catch (Exception e) {
            throw new ApiException("Failed to retrieve export-import data", String.valueOf(e));
        }
    }

    public static String cleanQuarter(Object value) {
        if (value == null) return null;

        try {
            double d = Double.parseDouble(String.valueOf(value));
            if (d == (int) d) {
                return String.valueOf((int) d); // მაგ.: 1.0 → 1
            } else {
                return String.valueOf(d);       // მაგ.: 1.5 → 1.5
            }
        } catch (NumberFormatException e) {
            return String.valueOf(value);       // fallback: აბრუნებს როგორც არის
        }
    }

    @Transactional(readOnly = true)
    public JsonNode getRegionalAnalysis(String tableName, String lang, String brand, String year) {
        try {
            //            String cacheKey = "regional_analysis:" + tableName + ":" + (lang != null ? lang : "en") + ":" +
            //                    (brand != null ? brand : "") + ":" + (year != null ? year : "");
            //            JsonNode cached = redisTemplate.opsForValue().get(cacheKey);
            //            if (cached != null) {
            //                return cached;
            //            }

            if (tableName == null) {
                tableName = DEFAULT_TABLE_NAME;
            }
            TableConfigText.TableMetadata tableConfig = TableConfigText.TABLES.getOrDefault(tableName, TableConfigText.TABLES.get(DEFAULT_TABLE_NAME));

            RegionalAnalysisParams params = new RegionalAnalysisParams();
            params.setTableName(tableName);
            params.setLang(lang);
            params.setBrand(brand);
            params.setYear(year);

            TableQueryStrategy<RegionalAnalysisParams> strategy = strategyFactory.getStrategy("regional-analysis-text", tableName, RegionalAnalysisParams.class);
            strategy.setTableName(tableName);

            // Determine year if not provided
            if (year == null) {
                params.setYear(((RegionalAnalysisQueryStrategy) strategy).getMaxYear());
            }

            ArrayNode response = objectMapper.createArrayNode();

            // Year selector
            QueryBuilder yearQuery = new QueryBuilder();
            strategy.configureQuery(yearQuery, new QueryParams<>("years", params));
            List<Map<String, Object>> years = globalRepository.executeAreaCurrencyQuery(yearQuery, tableName);
            ObjectNode yearSelector = objectMapper.createObjectNode();
            yearSelector.put("type", "selector");
            yearSelector.put("placeholder", languageService.getMain("main.year", lang));
            ArrayNode yearValues = objectMapper.createArrayNode();
            for (Map<String, Object> row : years) {
                String yearValue = (String) row.get("year");
                yearValues.add(createNode((Object) yearValue, yearValue));
            }
            yearSelector.set("selectValues", yearValues);
            response.add(yearSelector);

            // Quarter selector (only for auto_main)
            if ("[dbo].[auto_main]".equals(tableName)) {
                QueryBuilder quarterQuery = new QueryBuilder();
                strategy.configureQuery(quarterQuery, new QueryParams<>("quarters", params));
                List<Map<String, Object>> quarters = globalRepository.executeAreaCurrencyQuery(quarterQuery, tableName);
                ObjectNode quarterSelector = objectMapper.createObjectNode();
                quarterSelector.put("type", "selector");
                quarterSelector.put("placeholder", languageService.getMain("main.quarter", lang));
                ArrayNode quarterValues = objectMapper.createArrayNode();
                quarterValues.add(languageService.createNode((String) languageService.getAll(lang).getName(), null));
                for (Map<String, Object> row : quarters) {
                    String quarter = cleanQuarter(row.get("quarter"));
                    quarterValues.add(createNode(languageService.getQuarter(quarter, lang), row.get("quarter")));
                }
                quarterSelector.set("selectValues", quarterValues);
                response.add(quarterSelector);
            }

            // Brand selector
            QueryBuilder brandQuery = new QueryBuilder();
            strategy.configureQuery(brandQuery, new QueryParams<>("brands", params));
            List<Map<String, Object>> brands = globalRepository.executeAreaCurrencyQuery(brandQuery, tableName);
            ObjectNode brandSelector = objectMapper.createObjectNode();
            brandSelector.put("type", "selector");
            brandSelector.put("placeholder", languageService.getMain("main.brand", lang));
            ArrayNode brandValues = objectMapper.createArrayNode();
            for (Map<String, Object> row : brands) {
                String brandValue = (String) row.get("brand");
                brandValues.add(createNode((Object) brandValue, brandValue));
            }
            brandSelector.set("selectValues", brandValues);
            response.add(brandSelector);

            // Year of production selector
            QueryBuilder yearOfProdQuery = new QueryBuilder();
            strategy.configureQuery(yearOfProdQuery, new QueryParams<>("yearOfProduction", params));
            List<Map<String, Object>> yearOfProds = globalRepository.executeAreaCurrencyQuery(yearOfProdQuery, tableName);
            ObjectNode yearOfProdSelector = objectMapper.createObjectNode();
            yearOfProdSelector.put("type", "selector");
            yearOfProdSelector.put("placeholder", languageService.getMain("main.year_of_prod", lang));
            ArrayNode yearOfProdValues = objectMapper.createArrayNode();
            for (Map<String, Object> row : yearOfProds) {
                String yearValue = (String) row.get("year");
                yearOfProdValues.add(createNode((Object) yearValue, yearValue));
            }
            yearOfProdSelector.set("selectValues", yearOfProdValues);
            response.add(yearOfProdSelector);

            //redisTemplate.opsForValue().set(cacheKey, response, Duration.ofHours(1));
            return response;
        } catch (Exception e) {
            throw new ApiException("Failed to retrieve regional analysis data", String.valueOf(e));
        }
    }


    @Transactional(readOnly = true)
    public JsonNode getCompare(String tableName, String lang, String brand1, String brand2, String model1, String model2, String yearOfProd1, String yearOfProd2) {
        try {
            //            String cacheKey = "compare:" + tableName + ":" + (lang != null ? lang : "en") + ":" +
            //                    (brand1 != null ? brand1 : "ford") + ":" + (brand2 != null ? brand2 : "toyota") + ":" +
            //                    (model1 != null ? model1 : "") + ":" + (model2 != null ? model2 : "") + ":" +
            //                    (yearOfProd1 != null ? yearOfProd1 : "") + ":" + (yearOfProd2 != null ? yearOfProd2 : "");
            //            JsonNode cached = redisTemplate.opsForValue().get(cacheKey);
            //            if (cached != null) {
            //                return cached;
            //            }

            if (tableName == null) {
                tableName = DEFAULT_TABLE_NAME;
            }
            TableConfigText.TableMetadata tableConfig = TableConfigText.TABLES.getOrDefault(tableName, TableConfigText.TABLES.get(DEFAULT_TABLE_NAME));

            CompareParams params = new CompareParams();
            params.setTableName(tableName);
            params.setLang(lang);
            params.setBrand1(brand1 != null && !brand1.isEmpty() ? brand1 : "ford");
            params.setBrand2(brand2 != null && !brand2.isEmpty() ? brand2 : "toyota");
            params.setModel1(model1);
            params.setModel2(model2);
            params.setYearOfProd1(yearOfProd1);
            params.setYearOfProd2(yearOfProd2);

            TableQueryStrategy<CompareParams> strategy = strategyFactory.getStrategy("compare-text", tableName, CompareParams.class);
            strategy.setTableName(tableName);

            ArrayNode response = objectMapper.createArrayNode();

            // Brand selector
            QueryBuilder brandQuery = new QueryBuilder();
            strategy.configureQuery(brandQuery, new QueryParams<>("brands", params));
            List<Map<String, Object>> brands = globalRepository.executeAreaCurrencyQuery(brandQuery, tableName);
            ObjectNode brandSelector = objectMapper.createObjectNode();
            brandSelector.put("type", "comparison");
            brandSelector.put("placeholder", languageService.getMain("main.brand", lang));
            ArrayNode brandValues1 = objectMapper.createArrayNode();
            ArrayNode brandValues2 = objectMapper.createArrayNode();
            for (Map<String, Object> row : brands) {
                String brandValue = (String) row.get("brand");
                brandValues1.add(createNode(brandValue, brandValue));
                brandValues2.add(createNode(brandValue, brandValue));
            }
            brandSelector.set("selectValues1", brandValues1);
            brandSelector.set("selectValues2", brandValues2);
            response.add(brandSelector);

            // Model selector
            QueryBuilder modelQuery1 = new QueryBuilder();
            strategy.configureQuery(modelQuery1, new QueryParams<>("models1", params));
            List<Map<String, Object>> models1 = globalRepository.executeAreaCurrencyQuery(modelQuery1, tableName);

            QueryBuilder modelQuery2 = new QueryBuilder();
            strategy.configureQuery(modelQuery2, new QueryParams<>("models2", params));
            List<Map<String, Object>> models2 = globalRepository.executeAreaCurrencyQuery(modelQuery2, tableName);

            ObjectNode modelSelector = objectMapper.createObjectNode();
            modelSelector.put("type", "comparison");
            modelSelector.put("placeholder", languageService.getMain("main.model", lang));
            ArrayNode modelValues1 = objectMapper.createArrayNode();
            ArrayNode modelValues2 = objectMapper.createArrayNode();
            for (Map<String, Object> row : models1) {
                String modelValue = (String) row.get("model");
                modelValues1.add(createNode(modelValue, modelValue));
            }
            for (Map<String, Object> row : models2) {
                String modelValue = (String) row.get("model");
                modelValues2.add(createNode(modelValue, modelValue));
            }
            modelSelector.set("selectValues1", modelValues1);
            modelSelector.set("selectValues2", modelValues2);
            response.add(modelSelector);

            // Year of production selector
            QueryBuilder yearOfProdQuery1 = new QueryBuilder();
            strategy.configureQuery(yearOfProdQuery1, new QueryParams<>("yearOfProduction1", params));
            List<Map<String, Object>> yearOfProds1 = globalRepository.executeAreaCurrencyQuery(yearOfProdQuery1, tableName);

            QueryBuilder yearOfProdQuery2 = new QueryBuilder();
            strategy.configureQuery(yearOfProdQuery2, new QueryParams<>("yearOfProduction2", params));
            List<Map<String, Object>> yearOfProds2 = globalRepository.executeAreaCurrencyQuery(yearOfProdQuery2, tableName);

            ObjectNode yearOfProdSelector = objectMapper.createObjectNode();
            yearOfProdSelector.put("type", "comparison");
            yearOfProdSelector.put("placeholder", languageService.getMain("main.year_of_prod", lang));
            ArrayNode yearOfProdValues1 = objectMapper.createArrayNode();
            ArrayNode yearOfProdValues2 = objectMapper.createArrayNode();
            for (Map<String, Object> row : yearOfProds1) {
                String yearValue = (String) row.get("year");
                yearOfProdValues1.add(createNode(yearValue, yearValue));
            }
            for (Map<String, Object> row : yearOfProds2) {
                String yearValue = (String) row.get("year");
                yearOfProdValues2.add(createNode(yearValue, yearValue));
            }
            yearOfProdSelector.set("selectValues1", yearOfProdValues1);
            yearOfProdSelector.set("selectValues2", yearOfProdValues2);
            response.add(yearOfProdSelector);

            //redisTemplate.opsForValue().set(cacheKey, response, Duration.ofHours(1));
            return response;
        } catch (Exception e) {
            throw new ApiException("Failed to retrieve compare data", String.valueOf(e));
        }
    }

    @Transactional(readOnly = true)
    public JsonNode getFuel(String tableName, String lang) {
        try {
            //            String cacheKey = "fuel:" + tableName + ":" + (lang != null ? lang : "en");
            //            JsonNode cached = redisTemplate.opsForValue().get(cacheKey);
            //            if (cached != null) {
            //                return cached;
            //            }
            //
            //            if (tableName == null) {
            //                tableName = "[dbo].[FuelCl]";
            //            }
            TableConfigText.TableMetadata tableConfig = TableConfigText.TABLES.getOrDefault(tableName, TableConfigText.TABLES.get("[CL].[cl_fuel]"));

            FuelParams params = new FuelParams();
            params.setTableName(tableName);
            params.setLang(lang != null ? lang : "en");

            TableQueryStrategy<FuelParams> strategy = strategyFactory.getStrategy("fuel-text", tableName, FuelParams.class);
            strategy.setTableName(tableName);

            ArrayNode response = objectMapper.createArrayNode();

            // Export/Import selector
            ObjectNode exportImportSelector = objectMapper.createObjectNode();
            exportImportSelector.put("type", "selector");
            exportImportSelector.put("placeholder", "");
            exportImportSelector.set("selectValues", languageService.getExportImport(lang));
            response.add(exportImportSelector);

            // Fuel selector
            QueryBuilder fuelQuery = new QueryBuilder();
            strategy.configureQuery(fuelQuery, new QueryParams<>("fuels", params));
            List<Map<String, Object>> fuels = globalRepository.executeAreaCurrencyQuery(fuelQuery, tableName);
            ObjectNode fuelSelector = objectMapper.createObjectNode();
            fuelSelector.put("type", "selector");
            fuelSelector.put("placeholder", languageService.getMain("main.fuel_type", lang));
            ArrayNode fuelValues = objectMapper.createArrayNode();
            for (Map<String, Object> row : fuels) {
                fuelValues.add(createNode((String) row.get("name"), row.get("code")));
            }
            fuelSelector.set("selectValues", fuelValues);
            response.add(fuelSelector);

            // Currency selector
            ObjectNode currencySelector = objectMapper.createObjectNode();
            currencySelector.put("type", "switch");
            currencySelector.put("placeholder", "");
            currencySelector.set("selectValues", languageService.getCurrencySort(lang));
            response.add(currencySelector);

            //redisTemplate.opsForValue().set(cacheKey, response, Duration.ofHours(1));
            return response;
        } catch (Exception e) {
            throw new ApiException("Failed to retrieve fuel data", String.valueOf(e));
        }
    }

    @Transactional(readOnly = true)
    public JsonNode getRoad(String tableName, String lang) {
        try {
            //            String cacheKey = "road:" + tableName + ":" + (lang != null ? lang : "en");
            //            JsonNode cached = redisTemplate.opsForValue().get(cacheKey);
            //            if (cached != null) {
            //                return cached;
            //            }
            //
            //            if (tableName == null) {
            //                tableName = "[dbo].[Road_length]";
            //            }
            TableConfigText.TableMetadata tableConfig = TableConfigText.TABLES.getOrDefault(tableName, TableConfigText.TABLES.get("[dbo].[road_length]"));

            RoadParams params = new RoadParams();
            params.setTableName(tableName);
            params.setLang(lang != null ? lang : "en");

            TableQueryStrategy<RoadParams> strategy = strategyFactory.getStrategy("road-text", tableName, RoadParams.class);
            strategy.setTableName(tableName);

            ArrayNode response = objectMapper.createArrayNode();

            // Year selector
            QueryBuilder yearQuery = new QueryBuilder();
            strategy.configureQuery(yearQuery, new QueryParams<>(lang, params, "years"));
            List<Map<String, Object>> years = globalRepository.executeAreaCurrencyQuery(yearQuery, tableName);
            ObjectNode yearSelector = objectMapper.createObjectNode();
            yearSelector.put("type", "selector");
            yearSelector.put("placeholder", languageService.getMain("main.year", lang));
            ArrayNode yearValues = objectMapper.createArrayNode();
            for (Map<String, Object> row : years) {
                String yearValue = (String) row.get("year");
                yearValues.add(createNode(yearValue, yearValue));
            }
            yearSelector.set("selectValues", yearValues);
            response.add(yearSelector);

            // Region selector
            QueryBuilder regionQuery = new QueryBuilder();
            strategy.configureQuery(regionQuery, new QueryParams<>(lang, params, "regions"));
            List<Map<String, Object>> regions = globalRepository.executeAreaCurrencyQuery(regionQuery, "[CL].[cl_region]");
            ObjectNode regionSelector = objectMapper.createObjectNode();
            regionSelector.put("type", "selector");
            regionSelector.put("placeholder", languageService.getMain("main.region", lang));
            ArrayNode regionValues = objectMapper.createArrayNode();
            for (Map<String, Object> row : regions) {
                regionValues.add(createNode((String) row.get("name"), row.get("code")));
            }
            regionSelector.set("selectValues", regionValues);
            response.add(regionSelector);

            //redisTemplate.opsForValue().set(cacheKey, response, Duration.ofHours(1));
            return response;
        } catch (Exception e) {
            throw new ApiException("Failed to retrieve road data", String.valueOf(e));
        }
    }

    @Transactional(readOnly = true)
    public JsonNode getAccidents(String tableName, String lang) {
        try {
            //            String cacheKey = "accidents:" + tableName + ":" + (lang != null ? lang : "en");
            //            JsonNode cached = redisTemplate.opsForValue().get(cacheKey);
            //            if (cached != null) {
            //                return cached;
            //            }
            //
            //            if (tableName == null) {
            //                tableName = "[dbo].[AccidentsCl]";
            //            }

            AccidentsParams params = new AccidentsParams();
            params.setTableName(tableName);
            params.setLang(lang != null ? lang : "en");

            TableQueryStrategy<AccidentsParams> strategy = strategyFactory.getStrategy("accidents-text", tableName, AccidentsParams.class);
            strategy.setTableName(tableName);

            ArrayNode response = selectorBuilder.getObjectMapper().createArrayNode();

            QueryBuilder regionQuery = new QueryBuilder();
            strategy.configureQuery(regionQuery, new QueryParams<>(lang, params, "regions"));
            List<Map<String, Object>> regions = globalRepository.executeAreaCurrencyQuery(regionQuery, "[CL].[cl_region]");
            response.add(selectorBuilder.buildSelector("selector", languageService.getMain("main.region", lang), regions));

            QueryBuilder accidentQuery = new QueryBuilder();
            strategy.configureQuery(accidentQuery, new QueryParams<>(lang, params, "accidents"));
            List<Map<String, Object>> accidents = globalRepository.executeAreaCurrencyQuery(accidentQuery, tableName);
            response.add(selectorBuilder.buildSelector("selector", "", accidents));

            //redisTemplate.opsForValue().set(cacheKey, response, Duration.ofHours(1));
            return response;
        } catch (Exception e) {
            throw new ApiException("Failed to retrieve accidents data", String.valueOf(e));
        }
    }

    @Transactional(readOnly = true)
    public JsonNode getLicenses(String tableName, String lang, Integer year) {
        try {
            //            String cacheKey = "licenses:" + tableName + ":" + (lang != null ? lang : "en");
            //            JsonNode cached = redisTemplate.opsForValue().get(cacheKey);
            //            if (cached != null) {
            //                return cached;
            //            }

            if (tableName == null) {
                tableName = TableConfigText.DEFAULT_LICENSES_TABLE_NAME;
            }

            LicensesParams params = new LicensesParams();
            params.setTableName(tableName);
            params.setLang(lang != null ? lang : "en");
            params.setYear(year);

            TableQueryStrategy<LicensesParams> strategy = strategyFactory.getStrategy("licenses-text", tableName, LicensesParams.class);
            strategy.setTableName(tableName);

            ArrayNode response = selectorBuilder.getObjectMapper().createArrayNode();

            QueryBuilder yearQuery = new QueryBuilder();
            strategy.configureQuery(yearQuery, new QueryParams<>(lang, params, "years"));
            List<Map<String, Object>> years = globalRepository.executeAreaCurrencyQuery(yearQuery, tableName);
            response.add(selectorBuilder.buildSelector("selector", languageService.getMain("main.year", lang), years));

            //redisTemplate.opsForValue().set(cacheKey, response, Duration.ofHours(1));
            return response;
        } catch (Exception e) {
            throw new ApiException("Failed to retrieve licenses data", String.valueOf(e));
        }
    }

    private JsonNode createNode(String name, Object code) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("name", name);
        if (code instanceof String) {
            node.put("code", (String) code);
        } else if (code instanceof Double) {
            node.put("code", (Double) code);
        } else {
            node.put("code", (Integer) code);
        }
        return node;
    }
}
