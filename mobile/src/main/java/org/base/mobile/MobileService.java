package org.base.mobile;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import org.base.mobile.repository.GlobalRepository;
import org.base.mobile.dto.*;
import org.base.mobile.params.*;
import org.base.mobile.strategy.*;
import org.base.core.exeption.extend.ApiException;
import org.base.core.model.ClassificationTableType;
import org.base.core.service.QueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
public class MobileService {

    @Qualifier("secondaryJdbcTemplate")
    @Autowired
    private JdbcTemplate jdbcTemplate;
    private GlobalRepository globalRepository;
    private TableQueryStrategyFactory strategyFactory;
    private LanguageService languageService;


    /**
     * Retrieves sliders data grouped by page.
     */
    //@Cacheable(value = "slidersDataCache", key = "#tableName + '-' + #langName + '-' + #period + '-' + #title")
    @Transactional(readOnly = true)
    public List<SlidersDataDTO> getSlidersData(
            String tableName,
            @NotBlank String langName,
            @NotBlank String period,
            @NotBlank String title) {
        SlidersDataParams params = new SlidersDataParams(langName, period, title);
        QueryParams<SlidersDataParams> queryParams = new QueryParams<>(langName, params);
        TableQueryStrategy<SlidersDataParams> strategy = strategyFactory.getStrategy(
                "sliders-data", tableName, SlidersDataParams.class);

        QueryBuilder query = new QueryBuilder();
        strategy.configureQuery(query, queryParams);

        List<Map<String, Object>> results;
        try {
            results = globalRepository.executeAreaCurrencyQuery(query, tableName);
        } catch (Exception e) {
            //logger.error("Failed to execute sliders data query for table {}: {}", tableName, e.getMessage());
            throw new RuntimeException("Internal Server Error", e);
        }

        Map<String, SlidersDataDTO> groupedData = new HashMap<>();
        for (Map<String, Object> row : results) {
            Integer page = (Integer) row.get("page");
            String pageTitle = (String) row.get("title");
            String name = (String) row.get("name");
            String unit = (String) row.get("unit");
            String periodValue = (String) row.get("period");
            float value = row.get("value") instanceof Number ?
                    ((Number) row.get("value")).floatValue() : 0.0f;

            // Fix: consistently use the string key
            String key = String.valueOf(page);

            groupedData.computeIfAbsent(key, k -> new SlidersDataDTO(pageTitle, new ArrayList<>()));
            groupedData.get(key).getData().add(new SlidersDataDTO.DataItem(name, unit, periodValue, value));
        }

        return new ArrayList<>(groupedData.values());
    }

    //    @Cacheable(
    //            value = "fullRaitingCache",
    //            key = "#tableName + '-' + (#params.year() != null ? #params.year() : 'max') + '-' + " +
    //                    "(#params.sort() != null ? #params.sort() : 'default') + '-' + " +
    //                    "(#params.search() != null ? #params.search() : 'none') + '-' + " +
    //                    "(#params.transport() != null ? #params.transport() : 'none') + '-' + #params.page()"
    //    )
    @Transactional(readOnly = true)
    public PaginatedResponse<FullRaitingDTO.Item> getFullRaiting(
            String tableName, @Valid FullRaitingParams params) {
        if (!"[dbo].[eoyes]".equals(tableName) && !"[dbo].[auto_main]".equals(tableName)) {
            throw new IllegalArgumentException("Invalid table name: " + tableName);
        }

        Integer resolvedYear = resolveYear(tableName, params.getYear());

        int limit = 10;
        int effectivePage = params.getPage() != null && params.getPage() > 0 ? params.getPage() : 1;

        FullRaitingParams resolvedParams = new FullRaitingParams(
                resolvedYear, params.getTransport(), params.getSort(), params.getSearch(), params.getPage()
        );
        QueryParams<FullRaitingParams> queryParams = new QueryParams<>("", resolvedParams);

        FullRaitingQueryStrategy strategy = (FullRaitingQueryStrategy) strategyFactory.getStrategy(
                "full-raiting", tableName, FullRaitingParams.class);

        // Main query
        QueryBuilder query = new QueryBuilder();
        strategy.configureQuery(query, queryParams);

        // Count query
        QueryBuilder countQuery = strategy.configureCountQuery(queryParams);

        // Execute queries
        List<Map<String, Object>> results;
        long totalCount;
        try {
            results = globalRepository.executeAreaCurrencyQuery(query, tableName);
            totalCount = globalRepository.executeCountQuery(countQuery, tableName);
        } catch (Exception e) {
            //logger.error("Failed to execute full raiting query for table {}: {}", tableName, e.getMessage(), e);
            throw new ApiException("Failed to retrieve data from the database"+e.getCause() + e.getCause(), "Query execution failed", e);
        }

        // Calculate pagination
        int totalPages = (int) Math.ceil((double) totalCount / limit);
        if (effectivePage > totalPages && totalPages > 0) {
            throw new IllegalArgumentException("Invalid Page");
        }

        // Map results
        List<FullRaitingDTO.Item> parsedResult = results.stream()
                .map(row -> new FullRaitingDTO.Item(
                        row.get("brand") + " " + row.get("model"),
                        row.get("value") instanceof Number ? ((Number) row.get("value")).intValue() : 0
                ))
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                parsedResult,
                effectivePage,
                totalPages,
                limit + "/" + totalCount
        );
    }

    /**
     * Retrieves top 5 brand-model combinations by quantity.
     */
    //@Cacheable(value = "topFiveCache", key = "#tableName + '-' + (#year != null ? #year : 'max') + '-' + (#quarter != null ? #quarter : 'all') + '-' + (#transport != null ? #transport : 'none')")
    @Transactional(readOnly = true)
    public List<TopFiveDTO> getTopFive(
            String tableName, Integer year, String quarter, String transport) {
        validateTableAndLang(tableName, "main_auto");

        Integer resolvedYear = resolveYear(tableName, year);
        TopFiveParams params = new TopFiveParams(resolvedYear, quarter, transport);
        QueryParams<TopFiveParams> queryParams = new QueryParams<>("", params); // langName not used
        TableQueryStrategy<TopFiveParams> strategy = strategyFactory.getStrategy(
                "top-five", tableName, TopFiveParams.class);

        QueryBuilder query = new QueryBuilder();
        strategy.configureQuery(query, queryParams);

        List<Map<String, Object>> results;
        try {
            results = globalRepository.executeAreaCurrencyQuery(query, tableName);
        } catch (Exception e) {
            //logger.error("Failed to execute top five query for table {}: {}", tableName, e.getMessage());
            throw new RuntimeException("Failed to retrieve data from the database"+e.getCause() + e.getCause(), e);
        }

        List<TopFiveDTO> parsedResult = new ArrayList<>();
        for (Map<String, Object> row : results) {
            String brand = (String) row.get("brand");
            String model = (String) row.get("model");
            int value = row.get("value") instanceof Number ?
                    ((Number) row.get("value")).intValue() : 0;
            parsedResult.add(new TopFiveDTO(brand + " " + model, value));
        }

        return parsedResult;
    }


    //    @Cacheable(
    //            value = "filtersCache",
    //            key = "#tableName + '-' + (#params.year() != null ? #params.year() : 'max') + '-' + " +
    //                    "(#params.quarter() != null ? #params.quarter() : 'all') + '-' + " +
    //                    "#params.filter() + '-' + (#params.transport() != null ? #params.transport() : 'none') + '-' + " +
    //                    "#params.langName()"
    //    )
    @Transactional(readOnly = true)
    public FiltersDTO getFilters(String tableName, @Valid FiltersParams params) {
        if (!"[dbo].[eoyes]".equals(tableName) && !"[dbo].[auto_main]".equals(tableName)) {
            throw new IllegalArgumentException("Invalid table name: " + tableName);
        }

        Integer resolvedYear = resolveYear(tableName, params.getYear());
        FiltersParams resolvedParams = new FiltersParams(
                resolvedYear, params.getQuarter(), params.getFilter(), params.getTransport(), params.getLangName()
        );
        QueryParams<FiltersParams> queryParams = new QueryParams<>("", resolvedParams);

        FiltersQueryStrategy strategy = (FiltersQueryStrategy) strategyFactory.getStrategy(
                "filters", tableName, FiltersParams.class);

        // Find top model
        QueryBuilder topModelQuery = strategy.configureTopModelQuery(queryParams);
        Optional<Map<String, Object>> topModelResult = globalRepository.executeTopModelQuery(topModelQuery, tableName);
        if (topModelResult.isEmpty()) {
            throw new TopModelNotFoundException();
        }
        String topModel = (String) topModelResult.get().get("model");

        // Main query
        QueryBuilder query = new QueryBuilder();
        query.mergeParameters(List.of(topModel)); // Add topModel as first parameter
        strategy.configureQuery(query, queryParams);

        // Execute main query
        List<Map<String, Object>> results;
        try {
            results = globalRepository.executeAreaCurrencyQuery(query, tableName);
        } catch (Exception e) {
            //logger.error("Failed to execute filters query for table {}: {}", tableName, e.getMessage(), e);
            throw new ApiException("couldnt retrive data", "Query execution failed", e);
        }

        // Process results
        String filter = params.getFilter().isEmpty() ? "year_of_production" : params.getFilter();
        List<List<Object>> data = new ArrayList<>();
        List<String> hexCodes = params.getFilter().equals("color") ? new ArrayList<>() : null;

        for (Map<String, Object> row : results) {
            String itemName = params.getFilter().isEmpty() || !List.of("fuel", "color", "body", "engine").contains(params.getFilter())
                    ? String.valueOf(row.get(filter))
                    : (String) row.get(params.getLangName());
            int totalQuantity = row.get("totalQuantity") instanceof Number
                    ? ((Number) row.get("totalQuantity")).intValue() : 0;
            data.add(List.of(itemName, totalQuantity));

            if (params.getFilter().equals("color") && row.get("hex_code") != null) {
                hexCodes.add((String) row.get("hex_code"));
            }
        }

        return new FiltersDTO(data, hexCodes);
    }

    public static class NoDataFoundException extends RuntimeException {
        public NoDataFoundException(String message) {
            super(message);
        }
    }


    //    @Cacheable(
    //            value = "sankeyCache",
    //            key = "#tableName + '-' + (#params.year() != null ? #params.year() : 'max') + '-' + " +
    //                    "(#params.quarter() != null ? #params.quarter() : 'all') + '-' + " +
    //                    "#params.filter() + '-' + #params.langName()"
    //    )
    @Transactional(readOnly = true)
    public List<SankeyDTO> getSankey(String tableName, @Valid SankeyParams params) {
        if (!"[dbo].[eoyes]".equals(tableName) && !"[dbo].[auto_main]".equals(tableName)) {
            throw new IllegalArgumentException("Invalid table name: " + tableName);
        }

        Integer resolvedYear = resolveYear(tableName, params.getYear());
        SankeyParams resolvedParams = new SankeyParams(
                resolvedYear, params.getQuarter(), params.getFilter(), params.getLangName()
        );
        QueryParams<SankeyParams> queryParams = new QueryParams<>("", resolvedParams);

        SankeyQueryStrategy strategy = (SankeyQueryStrategy) strategyFactory.getStrategy(
                "sankey", tableName, SankeyParams.class);

        QueryBuilder query = new QueryBuilder();
        strategy.configureQuery(query, queryParams);

        List<Map<String, Object>> results;
        try {
            results = globalRepository.executeAreaCurrencyQuery(query, tableName);
        } catch (Exception e) {
            //logger.error("Failed to execute sankey query for table {}: {}", tableName, e.getMessage(), e);
            throw new ApiException("Failed to retrieve data from the database"+e.getCause(), "Query execution failed", e);
        }

        return results.stream()
                .map(row -> new SankeyDTO(
                        (String) row.get("from_node"),
                        (String) row.get("to_node"),
                        row.get("value") instanceof Number ? ((Number) row.get("value")).longValue() : 0L,
                        (String) row.get("id")
                ))
                .collect(Collectors.toList());
    }




    //    @Cacheable(
    //            value = "treemapCache",
    //            key = "#tableName + '-' + (#params.year() != null ? #params.year() : 'max') + '-' + " +
    //                    "(#params.quarter() != null ? #params.quarter() : 'max') + '-' + " +
    //                    "#params.otherTranslation()"
    //    )
    @Transactional(readOnly = true)
    public TreemapDTO getTreemap(String tableName, @Valid TreemapParams params) {
        if (!"[dbo].[eoyes]".equals(tableName) && !"[dbo].[auto_main]".equals(tableName)) {
            throw new IllegalArgumentException("Invalid table name: " + tableName);
        }

        Integer resolvedYear = resolveYear(tableName, params.getYear());
        String resolvedQuarter = resolveQuarter(tableName, resolvedYear, params.getQuarter());
        params.setOtherTranslation(languageService.getTranslation(params.getOtherTranslation(), "other"));
        TreemapParams resolvedParams = new TreemapParams(resolvedYear, resolvedQuarter, params.getOtherTranslation());
        QueryParams<TreemapParams> queryParams = new QueryParams<>("", resolvedParams);

        TreemapQueryStrategy strategy = (TreemapQueryStrategy) strategyFactory.getStrategy("treemap", tableName, TreemapParams.class);
        String otherLabel = languageService.getTranslation(params.getOtherTranslation(), "other");

        // Main query for brands and models
        QueryBuilder query = new QueryBuilder();
        strategy.configureQuery(query, queryParams);

        List<Map<String, Object>> results = globalRepository.executeAreaCurrencyQuery(query, tableName);

        // Process results
        Map<String, Map<String, Integer>> resultMap = new LinkedHashMap<>();

        for (Map<String, Object> row : results) {
            String brand = (String) row.get("brand");
            String model = (String) row.get("model");
            int quantity = row.get("totalQuantity") instanceof Number ? ((Number) row.get("totalQuantity")).intValue() : 0;

            resultMap.computeIfAbsent(brand, k -> new LinkedHashMap<>()).put(model, quantity);
        }

        // Query for "Other" brands
        List<String> topBrands = resultMap.keySet().stream().filter(b -> !b.equals(otherLabel)).toList();
        QueryBuilder otherBrandsQuery = strategy.configureOtherBrandsQuery(topBrands, resolvedParams);
        long otherBrandsTotal = globalRepository.executeSumQuery(otherBrandsQuery, tableName);

        if (otherBrandsTotal > 0) {
            resultMap.put(otherLabel, Map.of(otherLabel, (int) otherBrandsTotal));
        }

        return new TreemapDTO(resultMap);
    }

    //    /**
    //     * Retrieves treemap data for the given parameters.
    //     * @return Nested map matching the Node.js output format.
    //     */


    //    @Cacheable(
    //            value = "colorsCache",
    //            key = "#tableName + '-' + (#params.year() != null ? #params.year() : 'max') + '-' + " +
    //                    "(#params.quarter() != null ? #params.quarter() : 'all') + '-' + " +
    //                    "#params.langName() + '-' + #params.otherTranslation()"
    //    )
    @Transactional(readOnly = true)
    public List<ColorsDTO> getColors(String tableName, @Valid ColorsParams params) {
        if (!"[dbo].[eoyes]".equals(tableName) && !"[dbo].[auto_main]".equals(tableName)) {
            throw new IllegalArgumentException("Invalid table name: " + tableName);
        }

        Integer resolvedYear = resolveYear(tableName, params.getYear());
        ColorsParams resolvedParams = new ColorsParams(
                resolvedYear, params.getQuarter(), params.getLangName(), params.getOtherTranslation()
        );
        QueryParams<ColorsParams> queryParams = new QueryParams<>("", resolvedParams);

        ColorsQueryStrategy strategy = (ColorsQueryStrategy) strategyFactory.getStrategy(
                "colors", tableName, ColorsParams.class);

        QueryBuilder query = new QueryBuilder();
        strategy.configureQuery(query, queryParams);

        List<Map<String, Object>> results;
        try {
            results = globalRepository.executeAreaCurrencyQuery(query, tableName);
        } catch (Exception e) {
            //logger.error("Failed to execute colors query for table {}: {}", tableName, e.getMessage(), e);
            throw new ApiException("Failed to retrieve data from the database"+e.getCause(), "Query execution failed", e);
        }

        return results.stream()
                .filter(row -> row.get("name") != null)
                .map(row -> new ColorsDTO(
                        (String) row.get("name"),
                        row.get("value") instanceof Number ? ((Number) row.get("value")).intValue() : 0,
                        (String) row.get("hex")
                ))
                .collect(Collectors.toList());
    }

    //    /**
    //     * Retrieves color data for the given parameters.
    //     * @return List of color data matching the Node.js output format.
    //     */



    /**
     * Retrieves fuel data for the given parameters.
     * @return List of fuel data matching the Node.js output format.
     */
    //@Cacheable(value = "fuelsCache", key = "#year + '-' + #quarter + '-' + #langName + '-' + #tableName")
    @Transactional(readOnly = true)
    public List<FuelDTO> getFuels(Integer year, String quarter, String langName, String tableName) {
        Comparator<FuelDTO> sorter = (a, b) -> {
            String othersEn = languageService.getTranslation("en", "other");
            String othersKa = languageService.getTranslation("ka", "other");
            if (a.getName().equals(othersEn) || a.getName().equals(othersKa)) return 1;
            if (b.getName().equals(othersEn) || b.getName().equals(othersKa)) return -1;
            return b.getValue() - a.getValue();
        };

        return classificationService.getClassificationData(
                year,
                quarter,
                langName,
                tableName,
                ClassificationTableType.FUEL,
                "fuel",
                (results, ln) -> results.stream()
                        .map(row -> new FuelDTO(
                                (String) row.get("item_name"),
                                ((Number) row.get("value")).intValue()
                        ))
                        .collect(Collectors.toList()),
                sorter
        );
    }
    private static final int BODY_LIMIT = 11;

    /**
     * Retrieves body type data for the given parameters.
     * @return List of wrapped body data matching the Node.js output format.
     */
    //@Cacheable(value = "bodyCache", key = "#year + '-' + #quarter + '-' + #langName + '-' + #tableName")
    @Transactional(readOnly = true)
    public List<TResponseDTO> getBody(Integer year, String quarter, String langName, String tableName) {
    //        if (!tableConfig.getAllowedTables().contains(tableName)) {
    //            throw new IllegalArgumentException("Invalid or unsupported table name");
    //        }
    //        if (!tableConfig.getAllowedLangKeys().contains(langName.replace("name_", ""))) {
    //            throw new IllegalArgumentException("Invalid or unsupported language name");
    //        }

        // Define sorter
        Comparator<TDTO> sorter = (a, b) -> {
            String othersEn = languageService.getTranslation("en", "main.other");
            String othersKa = languageService.getTranslation("ka", "main.other");
            if (a.getName().equals(othersEn) || a.getName().equals(othersKa)) return 1;
            if (b.getName().equals(othersEn) || b.getName().equals(othersKa)) return -1;
            return b.getValue() - a.getValue();
        };

        String langKey = langName.replace("name_", "");
        String otherTranslation = languageService.getTranslation(langKey, "main.other");

        // Fetch and process data
        List<TDTO> processedData = classificationService.getClassificationData(
                year,
                quarter,
                langName,
                tableName,
                ClassificationTableType.BODY,
                "body",
                (results, ln) -> {
                    List<TDTO> bodyData = results.stream()
                            .map(row -> new TDTO(
                                    (String) row.get("item_name"),
                                    ((Number) row.get("value")).intValue()
                            ))
                            .toList();
                    if (bodyData.size() > BODY_LIMIT) {
                        List<TDTO> limitedData = new ArrayList<>(bodyData.subList(0, BODY_LIMIT));
                        int othersTotal = bodyData.subList(BODY_LIMIT, bodyData.size())
                                .stream()
                                .mapToInt(TDTO::getValue)
                                .sum();
                        limitedData.add(new TDTO(otherTranslation, othersTotal));
                        return limitedData;
                    }
                    return bodyData;
                },
                sorter
        );

        // Wrap in response structure
        return List.of(new TResponseDTO("name", processedData));
    }


    @Autowired
    private ClassificationService classificationService;
    private static final List<Integer> DEFAULT_QUARTERS = List.of(1, 2, 3, 4);


    /**
     * Retrieves engine data for the given parameters.
     * @return List of engine data matching the Node.js output format.
     */
    //@Cacheable(value = "enginesCache", key = "#year + '-' + #quarter + '-' + #langName + '-' + #tableName")
    @Transactional(readOnly = true)
    public List<EngineDTO> getEngines(Integer year, String quarter, String langName, String tableName) {
        Comparator<EngineDTO> sorter = (a, b) -> {
            String unknownEn = languageService.getTranslation("en", "main.unknown", "Unknown");
            String unknownKa = languageService.getTranslation("ka", "main.unknown", "დაუდგენელი");
            if (a.getName().equals(unknownEn) || a.getName().equals(unknownKa)) return 1;
            if (b.getName().equals(unknownEn) || b.getName().equals(unknownKa)) return -1;
            return b.getValue() - a.getValue();
        };

        return classificationService.getClassificationData(
                year,
                quarter,
                langName,
                tableName,
                ClassificationTableType.ENGINE,
                "engine",
                (results, ln) -> results.stream()
                        .map(row -> new EngineDTO(
                                (String) row.get("item_name"),
                                ((Number) row.get("value")).intValue()
                        ))
                        .collect(Collectors.toList()),
                sorter
        );
    }


    /**
     * Retrieves vehicle age data for the given parameters.
     * @return List of wrapped vehicle age data matching the Node.js output format.
     */
    //@Cacheable(value = "vehicleAgeCache", key = "#year + '-' + #quarter + '-' + #langName + '-' + #tableName")
    @Transactional(readOnly = true)
    public List<WrappedResponseDTO<VehicleAgeDTO>> getVehicleAge(Integer year, String quarter, String langName, String tableName) {
        List<VehicleAgeDTO> data = classificationService.getClassificationData(
                year,
                quarter,
                langName,
                tableName,
                ClassificationTableType.VEHICLE_AGE,
                "age",
                (results, ln) -> results.stream()
                        .map(row -> new VehicleAgeDTO(
                                (String) row.get("item_name"),
                                ((Number) row.get("value")).intValue()
                        ))
                        .collect(Collectors.toList()),
                null // No custom sorting required
        );

        return List.of(new WrappedResponseDTO<>("name", data));
    }


    //@Cacheable(value = "raceCache", key = "#tableName")
    @Transactional(readOnly = true)
    public Map<String, List<Map<String, Object>>> getRace(String tableName, RaceParams params) {
//        if (!"[dbo][eoyes]".equals(tableName)) {
//            throw new IllegalArgumentException("Invalid table name: " + tableName);
//        }

        params.setTableName(tableName);
        QueryParams<RaceParams> queryParams = new QueryParams<>("", params);
        RaceQueryStrategy strategy = (RaceQueryStrategy) strategyFactory.getStrategy("race", tableName, RaceParams.class);

        QueryBuilder query = new QueryBuilder();
        strategy.configureQuery(query, queryParams);

        // Execute query
        List<Map<String, Object>> results = globalRepository.executeAreaCurrencyQuery(query, tableName);

        // Transform results into desired JSON format
        return results.stream()
                .collect(Collectors.groupingBy(
                        row -> String.valueOf(((Number) row.get("year")).intValue()), // Convert to integer string
                        TreeMap::new, // Use TreeMap for sorted keys
                        Collectors.mapping(row -> Map.of(
                                "name", row.get("name"),
                                "value", row.get("value")
                        ), Collectors.toList())
                ));
    }

    private static final int BRAND_LIMIT = 20;
    /**
     * Retrieves brand data grouped by year for the given table.
     * @return Map of year to list of brand data matching the Node.js output format.
     */

    /**
     * Retrieves dual vehicle data for the given type.
     * @return List of vehicle data matching the Node.js output format.
     */
//    @Cacheable(
//            value = "dualCache",
//            key = "#tableName + '-' + (#params.vType() != null ? #params.vType() : '0') + '-' + #params.langName()"
//    )
    @Transactional(readOnly = true)
    public List<DualDTO> getDual(String tableName, @Valid DualParams params) {
        if (!"[dbo].[vehicles1000]".equals(tableName)) {
            throw new IllegalArgumentException("Invalid table name: " + tableName);
        }

        QueryParams<DualParams> queryParams = new QueryParams<>("", params);
        DualQueryStrategy strategy = (DualQueryStrategy) strategyFactory.getStrategy("dual", tableName, DualParams.class);

        QueryBuilder query = new QueryBuilder();
        strategy.configureQuery(query, queryParams);

        List<Map<String, Object>> results = globalRepository.executeAreaCurrencyQuery(query, tableName);;

        return results.stream()
                .map(row -> new DualDTO(
                        row.get("data1") instanceof Number ? ((Number) row.get("data1")).doubleValue() : 0.0,
                        row.get("data2") instanceof Number ? ((Number) row.get("data2")).longValue() : 0L,
                        row.get("name") instanceof Number ? ((Number) row.get("name")).intValue() : 0
                ))
                .collect(Collectors.toList());
    }


    @Autowired
    private FilterResolver filterResolver;

    private static final int TOP_CATEGORIES_LIMIT = 6;
    private static final String OTHERS_HEX = "#8DA399";

    /**
     * Retrieves stacked data for the given filter.
     * @return Stacked data matching the Node.js output format.
     */

//    @Cacheable(
//            value = "stackedCache",
//            key = "#tableName + '-' + #params.filter() + '-' + #params.langName() + '-' + #params.otherTranslation()"
//    )
    @Transactional(readOnly = true)
    public StackedDTO getStacked(String tableName, @Valid StackedParams params) {
        StackedQueryStrategy strategy = (StackedQueryStrategy) strategyFactory.getStrategy("stacked", tableName, StackedParams.class);

        // Fetch year range
        YearRange yearRange = fetchYearRange(strategy);

        // Execute main query
        List<Map<String, Object>> results = executeStackedQuery(strategy, params, tableName);

        // Process and return results
        List<StackedDTO.CategoryData> categories = processStackedResults(results, yearRange);

        return new StackedDTO(yearRange.minYear(), categories);
    }

    private YearRange fetchYearRange(StackedQueryStrategy strategy) {
        QueryBuilder minMaxQuery = strategy.configureMinMaxYearQuery();
        Map<String, Object> minMaxResult = globalRepository.executeMinMaxYearQuery(minMaxQuery, "[dbo].[vehicles_imp_exp]")
                .orElseThrow(() -> new ApiException("couldnt retrieve data", "No year data available"));

        int minYear = extractInt(minMaxResult.get("minYear"));
        int maxYear = extractInt(minMaxResult.get("maxYear"));

        return new YearRange(minYear, maxYear);
    }

    private List<Map<String, Object>> executeStackedQuery(StackedQueryStrategy strategy, StackedParams params, String tableName) {
        QueryParams<StackedParams> queryParams = new QueryParams<>("", params);
        QueryBuilder query = new QueryBuilder();
        strategy.configureQuery(query, queryParams);

        return globalRepository.executeAreaCurrencyQuery(query, tableName);
    }

    private List<StackedDTO.CategoryData> processStackedResults(List<Map<String, Object>> results, YearRange yearRange) {
        if (results.isEmpty()) {
            return Collections.emptyList();
        }

        int numYears = yearRange.numYears();
        Map<String, CategoryBuilder> categoryBuilders = new LinkedHashMap<>();

        for (Map<String, Object> row : results) {
            String name = (String) row.get("name");
            if (name == null) continue;

            String hex = Objects.toString(row.get("hex"), "");
            int year = extractInt(row.get("year"));
            long quantity = extractLong(row.get("quantity"));

            int yearIndex = year - yearRange.minYear();
            if (yearIndex < 0 || yearIndex >= numYears) continue;

            categoryBuilders
                    .computeIfAbsent(name, k -> new CategoryBuilder(k, hex, numYears))
                    .setQuantity(yearIndex, quantity);
        }

        return categoryBuilders.values().stream()
                .map(CategoryBuilder::build)
                .toList();
    }

    private int extractInt(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private long extractLong(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private record YearRange(int minYear, int maxYear) {
        int numYears() {
            return maxYear - minYear + 1;
        }
    }

    private static class CategoryBuilder {
        private final String name;
        private final String hex;
        private final long[] data;

        CategoryBuilder(String name, String hex, int numYears) {
            this.name = name;
            this.hex = hex;
            this.data = new long[numYears];
        }

        void setQuantity(int index, long quantity) {
            data[index] = quantity;
        }

        StackedDTO.CategoryData build() {
            List<Long> dataList = new ArrayList<>(data.length);
            for (long value : data) {
                dataList.add(value);
            }
            return new StackedDTO.CategoryData(name, hex, dataList);
        }
    }



    /**
     * Retrieves area currency data based on query parameters.
     * @return Area currency data matching the Node.js output format.
     */


//    @Cacheable(
//            value = "areaCurrencyCache",
//            key = "#tableName + '-' + #params.eI() + '-' + #params.type() + '-' + #params.fuel() + '-' + #params.vehicle() + '-' + #params.currencyGel() + '-' + #params.langName() + '-' + #params.currencyName() + '-' + #params.selected()"
//    )
    @Transactional(readOnly = true)
    public QuantityOrCurrencyDTO getAreaCurrencyOrQuantity(String tableName, @Valid AreaQuantityOrCurrencyParams params) {
//        String primaryTable = "1".equals(params.getType()) || !"2".equals(params.getType()) ? "vehicle_imp_exp" : "others_imp_exp";
//        if (!primaryTable.equals(tableName)) {
//            throw new IllegalArgumentException("Invalid table name: " + tableName);
//        }

        // Fetch minYear
        AreaCurrencyQueryStrategy strategy = (AreaCurrencyQueryStrategy) strategyFactory.getStrategy("area-currency", tableName, AreaQuantityOrCurrencyParams.class);
        QueryBuilder minYearQuery = strategy.configureMinYearQuery(tableName);
        Map<String, Object> minYearResult = globalRepository.executeMinMaxYearQuery(minYearQuery, tableName).orElseThrow(() -> new ApiException("Failed to retrieve data from the database", "No year data available"));
        int minYear = minYearResult.get("minYear") != null ? ((Number) minYearResult.get("minYear")).intValue() : 0;
        if (minYear == 0) {
            throw new ApiException("Failed to retrieve data from the database", "Invalid min year");
        }

        // Build and execute the main query
        QueryParams<AreaQuantityOrCurrencyParams> queryParams = new QueryParams<>("", params);
        QueryBuilder query = new QueryBuilder();
        strategy.configureQuery(query, queryParams);

        List<Map<String, Object>> results = globalRepository.executeAreaCurrencyQuery(query, tableName);

        // Process results
        List<Long> data = results.stream()
                .map(row -> row.get("currency") instanceof Number ? ((Number) row.get("currency")).longValue() : 0L)
                .collect(Collectors.toList());

        String name;

        if (params.getSelected().equals("currency")) {
            if (params.isCurrency()) {
                name = languageService.getTranslation(params.getCurrencyName(), "currency_usd_th");
            } else {
                name = languageService.getTranslation(params.getLangName(), "currency_gel_th");
            }
        } else {
            name = languageService.getTranslation(params.getLangName(), "cars");
        }

        return new QuantityOrCurrencyDTO(name, minYear, data);
    }


    /**
     * Retrieves trade data by country based on query parameters.
     * @return Trade data matching the Node.js output format.
     */

//    @Cacheable(
//            value = "tradeCache",
//            key = "#tableName + '-' + #params.eI() + '-' + #params.type() + '-' + #params.fuel() + '-' + #params.vehicle() + '-' + #params.currency() + '-' + #params.selector() + '-' + #params.langName()"
//    )
//    @Cacheable(
//            value = "tradeCache",
//            key = "#tableName + '-' + #params.eI() + '-' + #params.type() + '-' + #params.fuel() + '-' + #params.vehicle() + '-' + #params.currency() + '-' + #params.selector() + '-' + #params.langName()"
//    )
//    @Transactional(readOnly = true)
    public TradeDTO getTrade(String tableName, @Valid AreaQuantityOrCurrencyParams params) {
//        String primaryTable = "1".equals(params.getType()) || !"2".equals(params.getType()) ? "vehicle_imp_exp" : "others_imp_exp";
//        if (!primaryTable.equals(tableName)) {
//            throw new IllegalArgumentException("Invalid table name: " + tableName);
//        }

        // Fetch minYear
        TradeQueryStrategy strategy = (TradeQueryStrategy) strategyFactory.getStrategy("trade", tableName, AreaQuantityOrCurrencyParams.class);
        QueryBuilder minYearQuery = strategy.configureMinYearQuery(tableName);
        Map<String, Object> minYearResult = globalRepository.executeMinMaxYearQuery(minYearQuery, tableName)
                .orElseThrow(() -> new ApiException("Failed to retrieve data from the database", "No year data available"));
        int minYear = minYearResult.get("minYear") != null ? ((Number) minYearResult.get("minYear")).intValue() : 0;
        if (minYear == 0) {
            throw new ApiException("Failed to retrieve data from the database", "Invalid min year");
        }

        // Build and execute main query
        QueryParams<AreaQuantityOrCurrencyParams> queryParams = new QueryParams<>("", params);
        QueryBuilder query = new QueryBuilder();
        strategy.configureQuery(query, queryParams);

        List<Map<String, Object>> results = globalRepository.executeAreaCurrencyQuery(query, tableName);

        // Process results
        Map<String, TradeDTO.CountryData> countryMap = new LinkedHashMap<>();
        for (Map<String, Object> row : results) {
            String name = row.get("name") != null ? (String) row.get("name") : "Unknown";
            int year = row.get("year") != null ? ((Number) row.get("year")).intValue() : minYear;
            long value = row.get("value") instanceof Number ? ((Number) row.get("value")).longValue() : 0L;

            if (year < minYear || year >= minYear + strategy.getYearRange()) continue;

            countryMap.computeIfAbsent(name, k -> new TradeDTO.CountryData(
                    k, new ArrayList<>(Collections.nCopies(strategy.getYearRange(), 0L))
            )).data().set(year - minYear, value);
        }

        // Get top 5 countries
        List<TradeDTO.CountryData> topCountries = countryMap.values().stream()
                .sorted(Comparator.comparingLong(c -> -c.data().stream().mapToLong(Long::longValue).sum()))
                .limit(strategy.topCountriesLimit)
                .collect(Collectors.toList());

        return new TradeDTO(minYear, topCountries);
    }




    private static final int MIN_YEAR_THRESHOLD = 2017;


    //@Cacheable(value = "compareLineCache", key = "#brand1 + '-' + #model1 + '-' + #yearOfProduction1 + '-' + #brand2 + '-' + #model2 + '-' + #yearOfProduction2 + '-' + #langName")
    @Transactional(readOnly = true)
    public CompareLineDTO getCompareLine(String brand1, String model1, String yearOfProduction1, String fuel1, String body1, String color1, String engine1,
                                         String brand2, String model2, String yearOfProduction2, String fuel2, String body2, String color2, String engine2,
                                         @Valid @NotBlank String langName) {
        //        if (!tableConfig.getAllowedTables().contains("main_auto")) {
        //            throw new IllegalArgumentException("Invalid or unsupported table name");
        //        }
        //        if (!tableConfig.getAllowedLangKeys().contains(langName.replace("name_", ""))) {
        //            throw new IllegalArgumentException("Invalid or unsupported language name");
        //        }

        // Resolve parameters
        validateTableAndLang("[dbo].[auto_main]", langName);
        String resolvedBrand1 = brand1 != null ? brand1 : "toyota";
        String resolvedBrand2 = brand2 != null ? brand2 : "ford";
        String name1 = brand1 != null ? brand1 : "Toyota";
        String name2 = brand2 != null ? brand2 : "Ford";
        if (model1 != null) name1 += " " + model1;
        if (model2 != null) name2 += " " + model2;

        CompareParams params = new CompareParams(resolvedBrand1, model1, yearOfProduction1, resolvedBrand2, model2, yearOfProduction2);
        QueryParams<CompareParams> queryParams = new QueryParams<>(langName, params);

        // Determine minYear
        Integer minYear1 = globalRepository.getMinYear("[dbo].[auto_main]", buildQueryMap(params, true)).orElse(0);
        Integer minYear2 = globalRepository.getMinYear("[dbo].[auto_main]", buildQueryMap(params, false)).orElse(0);
        int minYear = (minYear1 > 0 && minYear2 > 0) ? Math.min(minYear1, minYear2) : Math.max(minYear1, minYear2);
        minYear = Math.max(minYear, MIN_YEAR_THRESHOLD);

        // Build and execute query
        TableQueryStrategy<CompareParams> strategy = strategyFactory.getStrategy("compare-line", "main_auto", CompareParams.class);
        QueryBuilder query = new QueryBuilder();
        strategy.configureQuery(query, queryParams);
        List<Map<String, Object>> results = globalRepository.executeAreaCurrencyQuery(query, strategy.getTableName());

        // Process results
        int currentYear = Year.now().getValue();
        List<Integer> quantities1 = new ArrayList<>(Collections.nCopies(currentYear - minYear, 0));
        List<Integer> quantities2 = new ArrayList<>(Collections.nCopies(currentYear - minYear, 0));

        for (Map<String, Object> row : results) {
            int year = ((Number) row.get("year")).intValue();
            int quantity = row.get("quantity") instanceof Number ? ((Number) row.get("quantity")).intValue() : 0;
            String config = (String) row.get("config");
            int yearIndex = year - minYear;
            if (yearIndex >= 0 && yearIndex < quantities1.size()) {
                if ("config1".equals(config)) {
                    quantities1.set(yearIndex, quantity);
                } else if ("config2".equals(config)) {
                    quantities2.set(yearIndex, quantity);
                }
            }
        }

        List<CompareSeriesDTO> data = List.of(
                new CompareSeriesDTO(name1, quantities1),
                new CompareSeriesDTO(name2, quantities2)
        );

        return new CompareLineDTO(minYear, data);
    }

    private Map<String, Object> buildQueryMap(CompareParams params, boolean isConfig1) {
        Map<String, Object> queryMap = new HashMap<>();
        if (isConfig1) {
            if (params.getBrand1() != null) queryMap.put("brand", params.getBrand1());
            if (params.getModel1() != null) queryMap.put("model", params.getModel1());
            if (params.getYearOfProduction1() != null) queryMap.put("year_of_production", params.getYearOfProduction1());
        } else {
            if (params.getBrand2() != null) queryMap.put("brand", params.getBrand2());
            if (params.getModel2() != null) queryMap.put("model", params.getModel2());
            if (params.getYearOfProduction2() != null) queryMap.put("year_of_production", params.getYearOfProduction2());
        }
        return queryMap;
    }

    /**
     * Retrieves regional map data based on query parameters.
     * @return Regional data matching the Node.js output format.
     */
    //@Cacheable(value = "regionalMapCache", key = "#tableName + '-' + #year + '-' + #quarter + '-' + #brand + '-' + #yearOfProduction + '-' + #region + '-' + #langName")
    @Transactional(readOnly = true)
    public List<RegionalMapDTO> getRegionalMap(String tableName, Integer year, String quarter, String brand, String yearOfProduction, String region, String langName) {
        validateTableAndLang(tableName, langName);
        Integer resolvedYear = resolveYear(tableName, year);
        String resolvedQuarter = resolveQuarter(tableName, quarter);

        if (year == null) {
            Integer maxYear = globalRepository.getMaxYear(tableName).orElseThrow(() -> new IllegalStateException("No year data available"));
            resolvedYear = maxYear;
        }


        RegionalMapParams params = new RegionalMapParams(resolvedYear, resolvedQuarter, brand, yearOfProduction, region);
        QueryParams<RegionalMapParams> queryParams = new QueryParams<>(langName, params);
        //params.langName = langName;

        // Build and execute query
        TableQueryStrategy<RegionalMapParams> strategy = strategyFactory.getStrategy("regional-map", tableName, RegionalMapParams.class);

        QueryBuilder query = new QueryBuilder();
        strategy.configureQuery(query, queryParams);
        List<Map<String, Object>> results = globalRepository.executeAreaCurrencyQuery(query, tableName);

        // Map results to DTO
        return results.stream()
                .map(row -> {
                    RegionalMapDTO dto = new RegionalMapDTO();
                    dto.setRegion((String) row.get("region"));
                    if (brand != null) dto.setBrand((String) row.get("brand"));
                    if (resolvedQuarter != null && !resolvedQuarter.equals("99")) Optional.ofNullable((Double) row.get("quarter")).map(q -> String.valueOf((int) Math.ceil(q)));
                    if (yearOfProduction != null) dto.setYearOfProduction((String) row.get("year_of_production"));
                    dto.setQuantity(row.get("quantity") instanceof Number ? ((Number) row.get("quantity")).intValue() : 0);
                    dto.setRegionCl(new RegionClDTO((String) row.get("name"), String.valueOf(row.get("code"))));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private void validateTableAndLang(String tableName, String langName) {
//        if (!tableConfig.getAllowedTables().contains(tableName)) {
//            throw new IllegalArgumentException("Invalid or unsupported table name");
//        }
//        if (!tableConfig.getAllowedLangKeys().contains(langName.replace("name_", ""))) {
//            throw new IllegalArgumentException("Invalid or unsupported language name");
//        }
    }

    //private void validateTableAndLang(String tableName, String expectedPrefix) {
//        if (!tableName.startsWith(expectedPrefix) || !tableConfig.getAllowedTables().contains(tableName)) {
//            throw new IllegalArgumentException("Invalid table name: must start with " + expectedPrefix);
//        }
//        if (!tableConfig.getAllowedLangKeys().contains(langName.replace("name_", ""))) {
//            throw new IllegalArgumentException("Invalid or unsupported language name");
//        }
   // }

    private Integer resolveYear(String tableName, Integer year) {
        if (year == null) {
            Integer maxYear = Integer.valueOf(globalRepository.getMaxYear(tableName)
                    .orElseThrow(() -> new IllegalStateException("No year data available")));
            return maxYear;
        }
        return year;
    }

    private String resolveQuarter(String tableName, String quarter) {
        return tableName.equals("main_auto") && (quarter == null || "99".equals(quarter)) ? null : quarter;
    }

    private String resolveQuarter(String tableName, Integer year, String quarter) {
        if (quarter != null && !quarter.equals("99")) {
            return quarter;
        }
        if (Objects.equals(tableName, "[dbo].[auto_main]")) {
            if (quarter == null) {
                QueryBuilder maxQuarterQuery = new TreemapQueryStrategy().configureMaxQuarterQuery(year);
                Long maxQuarter = globalRepository.executeSumQuery(maxQuarterQuery, "main_auto");
                return maxQuarter != null ? String.valueOf(maxQuarter) : "1"; // Fallback to 1 if no data
            }
        }
        return null; // quarter="99" means all quarters
    }

    /**
     * Retrieves regional bar chart data.
     */
    //@Cacheable(value = "regionalBarCache", key = "#tableName + '-' + #year + '-' + #quarter + '-' + #brand + '-' + #yearOfProduction + '-' + #region + '-' + #langName")
    //@Transactional(readOnly = true)
    public List<RegionalBarDTO> getRegionalBar(String tableName, Integer year, String quarter,
                                               String brand, String yearOfProduction, String region,
                                               @Valid @NotBlank String langName) {
        validateTableAndLang(tableName, langName);
        Integer resolvedYear = resolveYear(tableName, year);
        String resolvedQuarter = resolveQuarter(tableName, quarter);

        RegionalBarParams params = new RegionalBarParams(resolvedYear, resolvedQuarter, brand, yearOfProduction, region);
        QueryParams<RegionalBarParams> queryParams = new QueryParams<>(langName, params);
        TableQueryStrategy<RegionalBarParams> strategy = strategyFactory.getStrategy(
                "regional-bar", tableName, RegionalBarParams.class);

        QueryBuilder query = new QueryBuilder();
        strategy.configureQuery(query, queryParams);
        List<Map<String, Object>> results = globalRepository.executeAreaCurrencyQuery(query, tableName);

        List<BarDataDTO> data = results.stream()
                .map(row -> new BarDataDTO(
                        row.get("model") != null ? (String) row.get("model") : (String) row.get("brand"),
                        row.get("value") instanceof Number ? ((Number) row.get("value")).intValue() : 0))
                .collect(Collectors.toList());

        return List.of(new RegionalBarDTO("name", data));
    }

    /**
     * Retrieves regional quantity data for a specified table and parameters.
     *
     * @return A list containing a single RegionalBarDTO with region name and year-value data.
     */
    //@Cacheable(value = "regionalQuantityCache", key = "#tableName + '-' + #brand + '-' + #yearOfProduction + '-' + #region + '-' + #langName")
    @Transactional(readOnly = true)
    public List<RegionalBarDTO> getRegionalQuantity(
            String tableName, String brand, String yearOfProduction, String region,
            @Valid @NotBlank String langName) {
        validateTableAndLang(tableName, langName);

        RegionalQuantityParams params = new RegionalQuantityParams(null, null, brand, yearOfProduction, region);
        QueryParams<RegionalQuantityParams> queryParams = new QueryParams<>(langName, params);
        TableQueryStrategy<RegionalQuantityParams> strategy = strategyFactory.getStrategy("regional-quantity", tableName, RegionalQuantityParams.class);

        QueryBuilder query = new QueryBuilder();
        strategy.configureQuery(query, queryParams);
        List<Map<String, Object>> results = globalRepository.executeAreaCurrencyQuery(query, tableName);

        String regionName = (region != null && !"1".equals(region) && !results.isEmpty())
                ? (String) results.get(0).get("region_name")
                : languageService.getTranslation(langName.replace("name_", ""), "main.geo");

        List<BarDataDTO> data = results.stream()
                .map(row -> new BarDataDTO(
                        String.valueOf(((Number) row.get("year")).intValue()),
                        row.get("value") instanceof Number ? ((Number) row.get("value")).intValue() : 0))
                .collect(Collectors.toList());

        return List.of(new RegionalBarDTO(regionName, data));
    }

    /**
     * Retrieves equity data, calculating quantity ratios over years.
     */
    //@Cacheable(value = "equityCache", key = "#tableName + '-' + #brand + '-' + #yearOfProduction + '-' + #region + '-' + #langName")
    @Transactional(readOnly = true)
    public EquityDTO getEquity(String tableName, String brand, String yearOfProduction, String region, @Valid @NotBlank String langName) {
        validateTableAndLang(tableName, langName);

        QueryParams<EquityParams> queryParams = new QueryParams<>(langName, new EquityParams(null, null, brand, yearOfProduction, region));
        TableQueryStrategy<EquityParams> strategy = strategyFactory.getStrategy("equity", tableName, EquityParams.class);

        QueryBuilder query = new QueryBuilder();
        strategy.configureQuery(query, queryParams);
        Map<String, List<Map<String, Object>>> results = globalRepository.executeSubQueries(query, tableName);

        // Extract min year
        int minYear = results.get("minYearQuery").stream()
                .findFirst()
                .map(row -> ((Number) row.get("minYear")).intValue())
                .orElse(MIN_YEAR_THRESHOLD);

        List<Long> quantities1 = results.get("query1").stream()
                .map(row -> row.get("quantity") instanceof Number ? ((Number) row.get("quantity")).longValue() : 0L)
                .toList();
        List<Long> quantities2 = results.get("query2").stream()
                .map(row -> row.get("quantity") instanceof Number ? ((Number) row.get("quantity")).longValue() : 0L)
                .toList();

        // Calculate ratios
        List<Integer> ratios = new ArrayList<>();
        for (int i = 0; i < Math.min(quantities1.size(), quantities2.size()); i++) {
            long q1 = quantities1.get(i);
            long q2 = quantities2.get(i);
            int ratio = q2 != 0 ? Math.round((q1 * 100f) / q2) : 0;
            ratios.add(ratio);
        }

        // Determine name
        String name = brand != null && !results.get("query1").isEmpty()
                ? (String) results.get("query1").get(0).get("brand")
                : languageService.getTranslation(langName.replace("name_", ""), "main.vehicle");

        List<EquitySeriesDTO> data = List.of(new EquitySeriesDTO(name, ratios));
        return new EquityDTO(minYear, data);
    }

    private static final int MAX_YEAR_FUEL = 2026;
    /**
     * Retrieves fuel currency data by year.
     */
    //@Cacheable(value = "fuelCurrencyCache", key = "#tableName + '-' + #e_i + '-' + #fuel + '-' + #currency + '-' + #langName")
    @Transactional(readOnly = true)
    public FuelCurrencyDTO getFuelCurrency(FuelCurrencyParams params, String tableName, String langName) {
        //validateTableAndLang(tableName, "fuel_imp");
        QueryParams<FuelCurrencyParams> queryParams = new QueryParams<>(langName, params);
        TableQueryStrategy<FuelCurrencyParams> strategy = strategyFactory.getStrategy("fuel-currency", tableName, FuelCurrencyParams.class);

        QueryBuilder query = new QueryBuilder();
        strategy.configureQuery(query, queryParams);
        Map<String, List<Map<String, Object>>> results = globalRepository.executeSubQueries(query, tableName);

        // Extract min year
        int minYear = results.get("minYearQuery").stream()
                .findFirst()
                .map(row -> ((Number) row.get("minYear")).intValue())
                .orElse(MIN_YEAR_THRESHOLD);

        // Extract currency data
        Map<Integer, Double> resultsMap = results.get("dataQuery").stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row.get("year")).intValue(),
                        row -> row.get("currency") instanceof Number ?
                                ((Number) row.get("currency")).doubleValue() : 0.0,
                        (v1, v2) -> v1));

        // Build data array
        int maxYear = globalRepository.getMaxYear(tableName).orElse(MAX_YEAR_FUEL);
        List<Double> data = new ArrayList<>();
        for (int year = minYear; year <= maxYear; year++) {
            data.add(resultsMap.getOrDefault(year, 0.0));
        }

        // Determine currency name
        String currencyName = params.getCurrency() ? languageService.getTranslation(langName, "currency_gel_th") : languageService.getTranslation(langName, "currency_usd_th");

        return new FuelCurrencyDTO(currencyName, minYear, data);
    }


    /**
     * Retrieves fuel quantity data by year or month.
     */
    //@Cacheable(value = "fuelQuantityCache", key = "#tableName + '-' + #e_i + '-' + #fuel + '-' + #anualOrMonthly + '-' + #langName")
    @Transactional(readOnly = true)
    public FuelQuantityDTO getFuelQuantity(FuelQuantityParams params, String tableName, @Valid @NotBlank String langName) {
        validateTableAndLang(tableName, "fuel_imp");

        QueryParams<FuelQuantityParams> queryParams = new QueryParams<>(langName, params);
        TableQueryStrategy<FuelQuantityParams> strategy = strategyFactory.getStrategy("fuel-quantity", tableName, FuelQuantityParams.class);

        QueryBuilder query = new QueryBuilder();
        strategy.configureQuery(query, queryParams);
        Map<String, List<Map<String, Object>>> results = globalRepository.executeSubQueries(query, tableName);;

        // Extract min year
        int minYear = results.get("minYearQuery").stream()
                .findFirst()
                .map(row -> ((Number) row.get("minYear")).intValue())
                .orElse(MIN_YEAR_THRESHOLD);

        // Determine grouping
        boolean isMonthly = params.getAnualOrMonthly();
        String groupBy = isMonthly ? "month" : "year";

        // Extract tone data
        Map<Integer, Double> resultsMap = results.get("dataQuery").stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row.get(groupBy)).intValue(),
                        row -> row.get("tone") instanceof Number ?
                                ((Number) row.get("tone")).doubleValue() : 0.0,
                        (v1, v2) -> v1)); // Keep first value if duplicate

        // Build data array
        List<Double> data = new ArrayList<>();
        if (isMonthly) {
            for (int month = 1; month <= 12; month++) {
                data.add(resultsMap.getOrDefault(month, 0.0));
            }
        } else {
            int maxYear = globalRepository.getMaxYear(tableName).orElse(MAX_YEAR_FUEL);
            for (int year = minYear; year <= maxYear; year++) {
                data.add(resultsMap.getOrDefault(year, 0.0));
            }
        }

        // Determine name
        String name = languageService.getTranslation(langName, "tons");

        return new FuelQuantityDTO(name, isMonthly ? 1 : minYear, data);
    }

    /**
     * Retrieves average fuel price data by year.
     */
    //@Cacheable(value = "fuelAvPriceCache", key = "#tableName + '-' + #fuel + '-' + #currency + '-' + #langName")
    @Transactional(readOnly = true)
    public FuelAvPriceDTO getFuelAvPrice(FuelAvPriceParams params, String tableName, @Valid @NotBlank String langName) {
        validateTableAndLang(tableName, "fuel_imp");
        Boolean isGel = params.getCurrency();

        QueryParams<FuelAvPriceParams> queryParams = new QueryParams<>(langName, params);
        TableQueryStrategy<FuelAvPriceParams> strategy = strategyFactory.getStrategy("fuel-av-price", tableName, FuelAvPriceParams.class);

        QueryBuilder query = new QueryBuilder();
        strategy.configureQuery(query, queryParams);
        Map<String, List<Map<String, Object>>> results = globalRepository.executeSubQueries(query, tableName);

        // Extract min year
        int minYear = results.get("minYearQuery").stream()
                .findFirst()
                .map(row -> ((Number) row.get("minYear")).intValue())
                .orElse(MIN_YEAR_THRESHOLD);

        // Extract currency and tone data
        Map<Integer, Double> resultsMap = results.get("dataQuery").stream()
                .filter(row -> row.get("tone") instanceof Number && ((Number) row.get("tone")).doubleValue() > 0)
                .collect(Collectors.toMap(
                        row -> ((Number) row.get("year")).intValue(),
                        row -> {
                            double currencyValue = row.get("currency") instanceof Number ?
                                    ((Number) row.get("currency")).doubleValue() : 0.0;
                            double toneValue = ((Number) row.get("tone")).doubleValue();
                            return Math.round((currencyValue / toneValue) * 10.0) / 10.0; // Round to 1 decimal
                        },
                        (v1, v2) -> v1)); // Keep first value if duplicate

        // Build data array
        List<Double> data = new ArrayList<>();
        int maxYear = globalRepository.getMaxYear(tableName).orElse(MAX_YEAR_FUEL);
        for (int year = minYear; year <= maxYear; year++) {
            data.add(resultsMap.getOrDefault(year, 0.0));
        }

        // Determine currency name
        String currencyName = isGel
                ? languageService.getTranslation(langName.replace("name_", ""), "main.currency_gel")
                : languageService.getTranslation(langName.replace("name_", ""), "main.currency_usd");

        return new FuelAvPriceDTO(currencyName, minYear, data);
    }

    /**
     * Retrieves fuel column data by year and country.
     */
    //@Cacheable(value = "fuelColumnCache", key = "#tableName + '-' + #e_i + '-' + #fuel + '-' + #currency + '-' + #langName")
    @Transactional(readOnly = true)
    public FuelColumnDTO getFuelColumn(FuelColumnParams params, String tableName, @Valid @NotBlank String langName) {
        //validateTableAndLang(tableName, "fuel_imp");
        Boolean isGel = params.getCurrency();

        QueryParams<FuelColumnParams> queryParams = new QueryParams<>(langName, params);
        TableQueryStrategy<FuelColumnParams> strategy = strategyFactory.getStrategy("fuel-column", tableName, FuelColumnParams.class);

        QueryBuilder query = new QueryBuilder();
        strategy.configureQuery(query, queryParams);
        List<Map<String, Object>> results = globalRepository.executeAreaCurrencyQuery(query, tableName);;

        // Extract unique years
        Set<Integer> yearsSet = results.stream().map(row -> ((Number) row.get("year")).intValue()).collect(Collectors.toSet());

        if (yearsSet.isEmpty()) {
            return new FuelColumnDTO(List.of(), List.of());
        }

        // Determine min and max years
        int minYear = yearsSet.stream().min(Integer::compare).orElse(MIN_YEAR_THRESHOLD);
        int maxYear = yearsSet.stream().max(Integer::compare).orElse(MAX_YEAR_FUEL);

        // Create categories
        List<Integer> categories = new ArrayList<>();
        for (int year = minYear; year <= maxYear; year++) {
            categories.add(year);
        }

        // Build series map
        Map<String, List<Double>> seriesMap = new HashMap<>();
        for (Map<String, Object> row : results) {
            String countryName = (String) row.get("country_name");
            if (countryName == null) continue; // Skip if no translation
            int year = ((Number) row.get("year")).intValue();
            double currencyValue = row.get("currency") instanceof Number ?
                    Math.round(((Number) row.get("currency")).doubleValue() * 10.0) / 10.0 : 0.0;

            seriesMap.computeIfAbsent(countryName, k -> new ArrayList<>(Collections.nCopies(categories.size(), 0.0)))
                    .set(categories.indexOf(year), currencyValue);
        }

        // Transform series map to series list
        List<FuelColumnDTO.SeriesDTO> series = seriesMap.entrySet().stream()
                .map(entry -> new FuelColumnDTO.SeriesDTO(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        return new FuelColumnDTO(categories, series);
    }


    /**
     * Retrieves fuel line data by fuel type and year.
     */
    //@Cacheable(value = "fuelLineCache", key = "#tableName + '-' + #langName")
    @Transactional(readOnly = true)
    public FuelLineDTO getFuelLine(String tableName, @Valid @NotBlank String langName) {
        //validateTableAndLang(tableName, "fuel_prices");
        FuelLineParams params = new FuelLineParams();
        QueryParams<FuelLineParams> queryParams = new QueryParams<>(langName, params);
        TableQueryStrategy<FuelLineParams> strategy = strategyFactory.getStrategy("fuel-line", tableName, FuelLineParams.class);

        QueryBuilder query = new QueryBuilder();
        strategy.configureQuery(query, queryParams);
        Map<String, List<Map<String, Object>>> results;
        try {
            results = globalRepository.executeSubQueries(query, tableName);
        } catch (Exception e) {
            //logger.error("Failed to execute fuel line query for table {}: {}", tableName, e.getMessage());
            throw new RuntimeException("Failed to retrieve data from the database"+e.getCause(), e);
        }

        // Extract min year
        int minYear = results.get("minYearQuery").stream()
                .findFirst()
                .map(row -> ((Number) row.get("minYear")).intValue())
                .orElse(MIN_YEAR_THRESHOLD);

        // Extract max year
        int maxYear = globalRepository.getMaxYear(tableName).orElse(MAX_YEAR_FUEL);

        // Build fuel data map
        Map<String, List<Double>> fuelDataMap = new HashMap<>();
        List<Integer> years = new ArrayList<>();
        for (int year = minYear; year <= maxYear; year++) {
            years.add(year);
        }

        for (Map<String, Object> row : results.get("dataQuery")) {
            String fuelName = (String) row.get("fuel_name");
            if (fuelName == null) continue; // Skip if no translation
            int year = ((Number) row.get("year")).intValue();
            double averagePrice = row.get("average_price") instanceof Number ?
                    Math.round(((Number) row.get("average_price")).doubleValue() * 10.0) / 10.0 : 0.0;

            fuelDataMap.computeIfAbsent(fuelName, k -> new ArrayList<>(Collections.nCopies(years.size(), 0.0)))
                    .set(years.indexOf(year), averagePrice);
        }

        // Transform fuel data map to series list
        List<FuelLineDTO.SeriesDTO> data = fuelDataMap.entrySet().stream()
                .map(entry -> new FuelLineDTO.SeriesDTO(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        return new FuelLineDTO(minYear, data);
    }

    /**
     * Retrieves road length data by year and region.
     */
    //@Cacheable(value = "roadLengthCache", key = "#tableName + '-' + #year + '-' + #region + '-' + #langName")
    @Transactional(readOnly = true)
    public List<RoadLengthDTO> getRoadLength(String tableName, Integer year, String region, @Valid @NotBlank String langName) {
        //validateTableAndLang(tableName, "road_length");

        Integer resolvedYear = resolveYear(tableName, year);
        String resolvedRegion = region != null ? region : "1";

        RoadLengthParams params = new RoadLengthParams(resolvedYear, resolvedRegion);
        QueryParams<RoadLengthParams> queryParams = new QueryParams<>(langName, params);
        TableQueryStrategy<RoadLengthParams> strategy = strategyFactory.getStrategy("road-length", tableName, RoadLengthParams.class);

        QueryBuilder query = new QueryBuilder();
        strategy.configureQuery(query, queryParams);
        List<Map<String, Object>> results = globalRepository.executeAreaCurrencyQuery(query, tableName);

        // Process results
        List<RoadLengthDTO> processedResult = results.stream()
                .filter(row -> row.get("name") != null)
                .map(row -> new RoadLengthDTO(
                        (String) row.get("name"),
                        row.get("length") instanceof Number ? ((Number) row.get("length")).doubleValue() : 0.0
                ))
                .collect(Collectors.toList());

        // Calculate total sum
        double allSum = processedResult.stream().mapToDouble(RoadLengthDTO::getValue).sum();
        double roundedSum = Math.round(allSum * 10.0) / 10.0;

        // Add "all" entry
        String allName = languageService.getTranslation(langName, "all");
        processedResult.add(0, new RoadLengthDTO(allName, roundedSum));

        return processedResult;
    }

    /**
     * Retrieves accident quantity data by region and accident type.
     */
    //@Cacheable(value = "accidentsMainCache", key = "#tableName + '-' + #region + '-' + #accidents + '-' + #langName")
    @Transactional(readOnly = true)
    public AccidentsMainDTO getAccidentsMain(
            String tableName, String region, String accidents,
            @Valid @NotBlank String langName) {
        //validateTableAndLang(tableName, "[dbo].[patrul_ssk_ask]");

        String resolvedRegion = region != null ? region : "1";
        String resolvedAccidents = accidents != null ? accidents : "3";

        AccidentsMainParams params = new AccidentsMainParams(resolvedRegion, resolvedAccidents);
        QueryParams<AccidentsMainParams> queryParams = new QueryParams<>(langName, params);
        TableQueryStrategy<AccidentsMainParams> strategy = strategyFactory.getStrategy("accidents-main", tableName, AccidentsMainParams.class);

        QueryBuilder query = new QueryBuilder();
        strategy.configureQuery(query, queryParams);
        List<Map<String, Object>> results = globalRepository.executeAreaCurrencyQuery(query, tableName);

        // Get min year
        int minYear = globalRepository.getMinYear(tableName, null).orElseThrow(() -> new IllegalStateException("No year data available"));

        // Extract quantities
        List<Integer> quantities = results.stream()
                .map(row -> row.get("quantity") instanceof Number ? ((Number) row.get("quantity")).intValue() : 0)
                .collect(Collectors.toList());
        // Get a translation for "Quantity"
        String quantityName = languageService.getTranslation(langName.replace("name_", ""), "main.quantity");

        // Build response
        List<AccidentsMainDTO.SeriesDTO> data = List.of(new AccidentsMainDTO.SeriesDTO(quantityName, quantities));
        return new AccidentsMainDTO(minYear, data);
    }

    /**
     * Retrieves accident quantity data by gender and accident type.
     */
    //@Cacheable(value = "accidentsGenderCache", key = "#tableName + '-' + #accidents + '-' + #langName")
    @Transactional(readOnly = true)
    public AccidentsGenderDTO getAccidentsGender(
            String tableName, String accidents,
            @Valid @NotBlank String langName) {
        validateTableAndLang(tableName, "patrul_age");

        if (accidents == null || "3".equals(accidents)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Data Not Found");
        }

        AccidentsGenderParams params = new AccidentsGenderParams(accidents);
        QueryParams<AccidentsGenderParams> queryParams = new QueryParams<>(langName, params);
        TableQueryStrategy<AccidentsGenderParams> strategy = strategyFactory.getStrategy("accidents-gender", tableName, AccidentsGenderParams.class);

        QueryBuilder query = new QueryBuilder();
        strategy.configureQuery(query, queryParams);
        List<Map<String, Object>> results;
        try {
            results = globalRepository.executeAreaCurrencyQuery(query, tableName);
        } catch (Exception e) {
            //logger.error("Failed to execute accidents gender query for table {}: {}", tableName, e.getMessage());
            throw new RuntimeException("Failed to retrieve data from the database"+e.getCause(), e);
        }

        // Get min year
        int minYear = globalRepository.getMinYear(tableName, null)
                .orElseThrow(() -> new IllegalStateException("No year data available"));

        // Process gender data
        Map<String, List<Integer>> genderData = new HashMap<>();
        results.forEach(row -> {
            String gender = (String) row.get("gender_name");
            if (gender == null) return;
            int quantity = row.get("quantity") instanceof Number ? ((Number) row.get("quantity")).intValue() : 0;

            genderData.computeIfAbsent(gender, k -> new ArrayList<>()).add(quantity);
        });

        // Build series
        List<AccidentsGenderDTO.SeriesDTO> series = genderData.entrySet().stream()
                .map(entry -> new AccidentsGenderDTO.SeriesDTO(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        return new AccidentsGenderDTO(minYear, series);
    }

    /**
     * Retrieves license data for Sankey diagram by gender and age.
     */
    //@Cacheable(value = "licenseSankeyCache", key = "#tableName + '-' + #year + '-' + #langName")
    @Transactional(readOnly = true)
    public List<LicenseSankeyDTO> getLicenseSankey(QueryParams<LicenseSankeyParams> queryParams, String tableName) {
        //validateTableAndLang(tableName, "license_data");

        Integer resolvedYear = resolveYear(tableName, queryParams.getParams().getYear());
        queryParams.getParams().setYear(resolvedYear);
        TableQueryStrategy<LicenseSankeyParams> strategy = strategyFactory.getStrategy("license-sankey", tableName, LicenseSankeyParams.class);

        List<LicenseSankeyDTO> results = new ArrayList<>();
        String allName = languageService.getTranslation(queryParams.getLangName(), "all");

        QueryBuilder genderQuery = new QueryBuilder();
        strategy.configureQuery(genderQuery, queryParams);
        Map<String, List<Map<String, Object>>> queryResults = globalRepository.executeSubQueries(genderQuery, tableName);

        ClassificationTableType clTable = ClassificationTableType.LICENSE_AGE;
        List<Map<String, Object>> genders = queryResults.getOrDefault("genderAgeQuery", List.of());

        for (Map<String, Object> genderRow : genders) {
            String genderName = genderRow.get("age_name") != null ? (String) genderRow.get("gender_name") : allName;
            String ageName = genderRow.get("age_name") != null ? String.valueOf(genderRow.get("age_name")) : String.valueOf(genderRow.get("gender_name"));
            int totalQuantity = genderRow.get("total_quantity") instanceof Number ? ((Number) genderRow.get("total_quantity")).intValue() : 0;

            results.add(new LicenseSankeyDTO(genderName, ageName, totalQuantity, genderName + "-" + ageName));
        }

        return results;
    }

    /**
     * Retrieves license data by gender and year.
     */
    //@Cacheable(value = "licenseGenderCache", key = "#tableName + '-' + #langName")
    @Transactional(readOnly = true)
    public List<LicenseGenderDTO> getLicenseGender(LicenseGenderParams params, @Valid @NotBlank String langName) {
        //validateTableAndLang(tableName, "license_data");

        QueryParams<LicenseGenderParams> queryParams = new QueryParams<>(langName, params);
        TableQueryStrategy<LicenseGenderParams> strategy = strategyFactory.getStrategy("license-gender", "[dbo].[licenses]", LicenseGenderParams.class);

        QueryBuilder query = new QueryBuilder();
        strategy.configureQuery(query, queryParams);

        List<Map<String, Object>> results = globalRepository.executeAreaCurrencyQuery(query, "[dbo].[licenses]");

        Map<String, LicenseGenderDTO> genderDataMap = new LinkedHashMap<>();

        for (Map<String, Object> row : results) {
            String genderName = (String) row.get("gender_name");
            if (genderName == null) continue;
            String year = String.valueOf(row.get("year"));
            int quantity = row.get("quantity") instanceof Number ? ((Number) row.get("quantity")).intValue() : 0;

            genderDataMap.computeIfAbsent(genderName, k -> new LicenseGenderDTO(k, new ArrayList<>())).getData().add(new LicenseGenderDTO.DataPoint(year, quantity));
        }

        return new ArrayList<>(genderDataMap.values());
    }

    /**
     * Retrieves license data by age and year.
     */
    //@Cacheable(value = "licenseAgeCache", key = "#tableName + '-' + #langName")
    @Transactional(readOnly = true)
    public LicenseAgeDTO getLicenseAge(Boolean tableName, @Valid @NotBlank String langName) {
        //validateTableAndLang(tableName, "license_data");

        LicenseAgeParams params = new LicenseAgeParams(tableName);
        QueryParams<LicenseAgeParams> queryParams = new QueryParams<>(langName, params);
        TableQueryStrategy<LicenseAgeParams> strategy = strategyFactory.getStrategy("license-age", "[dbo].[licenses]", LicenseAgeParams.class);

        QueryBuilder query = new QueryBuilder();
        strategy.configureQuery(query, queryParams);

        List<Map<String, Object>> results = globalRepository.executeAreaCurrencyQuery(query, "[dbo].[licenses]");;

        TreeSet<String> years = new TreeSet<>();
        Map<String, LicenseAgeDTO.Series> seriesMap = new LinkedHashMap<>();

        for (Map<String, Object> row : results) {
            String ageName = (String) row.get("age_name");
            if (ageName == null) continue;
            String year = String.valueOf(row.get("year"));
            int quantity = row.get("quantity") instanceof Number ? ((Number) row.get("quantity")).intValue() : 0;
            years.add(year);
            seriesMap.computeIfAbsent(ageName, k -> new LicenseAgeDTO.Series(k, new ArrayList<>()));

            LicenseAgeDTO.Series series = seriesMap.get(ageName);
            // Initialize data list to hold quantities for all years
            while (series.getData().size() < years.size()) {
                series.getData().add(0);
            }
            series.getData().set(years.headSet(year).size(), quantity);
        }

        // Ensure all series have data for all years
        List<String> categories = new ArrayList<>(years);
        for (LicenseAgeDTO.Series series : seriesMap.values()) {
            while (series.getData().size() < categories.size()) {
                series.getData().add(0);
            }
        }

        return new LicenseAgeDTO(categories, new ArrayList<>(seriesMap.values()));
    }

    /**
     * Retrieves license data from licenses_main and licenses_eoy by year.
     */
    //@Cacheable(value = "licenseDualCache", key = "#tableName")
    @Transactional(readOnly = true)
    public List<LicenseDualDTO> getLicenseDual(String tableName,  QueryParams<LicenseDualParams> queryParams) {

        TableQueryStrategy<LicenseDualParams> strategy = strategyFactory.getStrategy("license-dual", tableName, LicenseDualParams.class);

        QueryBuilder query = new QueryBuilder();
        strategy.configureQuery(query, queryParams);

        Map<String, List<Map<String, Object>>> results = globalRepository.executeSubQueries(query, tableName);

        List<Map<String, Object>> mainResults = results.getOrDefault("mainQuery", List.of());
        List<Map<String, Object>> eoyResults = results.getOrDefault("eoyQuery", List.of());

        // Map eoy results by year
        Map<String, Integer> eoyMap = new HashMap<>();
        for (Map<String, Object> row : eoyResults) {
            String year = row.get("name") instanceof Number ? ((Number) row.get("name")).intValue() + "" : null;
            int quantity = row.get("data1") instanceof Number ? ((Number) row.get("data1")).intValue() : 0;
            eoyMap.put(year, quantity);
        }

        // Combine results
        List<LicenseDualDTO> combinedData = new ArrayList<>();
        for (Map<String, Object> row : mainResults) {
            String year = row.get("name") instanceof Number ? ((Number) row.get("name")).intValue() + "" : null;
            int mainQuantity = row.get("data2") instanceof Number ? ((Number) row.get("data2")).intValue() : 0;
            int eoyQuantity = eoyMap.getOrDefault(year, 0);
            combinedData.add(new LicenseDualDTO(year, eoyQuantity, mainQuantity));
        }

        return combinedData;
    }
}
