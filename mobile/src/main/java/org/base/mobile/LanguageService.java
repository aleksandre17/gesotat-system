package org.base.mobile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.base.mobile.dto.text.SelectorValue;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides language translations for the application.
 */
@Service
public class LanguageService {
    private final Map<String, Map<String, String>> translations;
    private final ObjectMapper objectMapper;

    public LanguageService() {
        translations = new HashMap<>();
        // Example translations (equivalent to enTranslations/kaTranslations)
        translations.put("en", Map.of("main.other", "Others",
                                       "main.title", "title_en",
                                       "main.period", "period_en",
                                       "main.langName", "name_en",
                                        "tons", "Tons",
                                       "currency_gel_th", "Thousand GEL",
                                       "currency_usd_th", "Thousand US dollars"));
        translations.put("ka", Map.of("main.other", "დანარჩენი",
                                       "main.title", "title_ka",
                                       "main.period", "period_ka",
                                       "main.langName", "name_ka",
                                       "tons", "ტონა",
                                       "currency_gel_th", "ათასი ლარი",
                                       "currency_usd_th", "ათასი აშშ დოლარი"));

        objectMapper = new ObjectMapper();
    }

    /**
     * Retrieves translations for the given language.
     * @param lang Language key ("en" or "ka").
     * @return Map of translations.
     */
    public Map<String, String> getTranslations(String lang) {
        return translations.getOrDefault(lang, translations.get("ka"));
    }

    /**
     * Retrieves column names for the given language.
     * @param lang Language key ("en" or "ka").
     * @return Map with column names (langName, period, title).
     */
    public Map<String, String> getColumnNames(String lang) {
        Map<String, String> columns = new HashMap<>();
        if ("en".equals(lang)) {
            columns.put("year", "Year");
            columns.put("quarter", "Quarter");
            columns.put("langName", "name_en");
            columns.put("period", "period_en");
            columns.put("title", "title_en");
            columns.put("other", "Others");
            columns.put("currency_gel_th", "Thousand GEL");
            columns.put("currency_usd_th", "Thousand US dollars");
            columns.put("cars", "Vehicle");
            columns.put("all", "all");
        } else {
            columns.put("year", "წელი");
            columns.put("quarter", "კვარტალი");
            columns.put("langName", "name_ka");
            columns.put("period", "period_ka");
            columns.put("title", "title_ka");
            columns.put("other", "დანარჩენი");
            columns.put("currency_gel_th", "ათასი ლარი");
            columns.put("currency_usd_th", "ათასი აშშ დოლარი");
            columns.put("cars", "ავტომობილები");
            columns.put("all", "ყველა");
        }
        return columns;
    }

    public String getTranslation(String langKey, String key) {
        return getColumnNames(langKey).getOrDefault(key, "Others");
    }

    public String getTranslation(String langKey, String key, String defaultValue) {
        return translations.getOrDefault(langKey, Map.of()).getOrDefault(key, defaultValue);
    }

    public JsonNode getTopFive(String lang) {
        ArrayNode topFive = objectMapper.createArrayNode();
        if ("ka".equalsIgnoreCase(lang)) {
            // Georgian translations
            topFive.add(createTopFiveNode("სულ", "99"));
            topFive.add(createTopFiveNode("მსუბუქი", "1"));
            topFive.add(createTopFiveNode("სამგზავრო", "4"));
        } else {
            // Default to English
            topFive.add(createTopFiveNode("All", 99));
            topFive.add(createTopFiveNode("Passenger Cars", 1));
            topFive.add(createTopFiveNode("Buses and Minibus", 4));
        }
        return topFive;
    }
    public JsonNode getSortingItems(String lang) {
        ArrayNode sortingItems = objectMapper.createArrayNode();
        if ("ka".equalsIgnoreCase(lang)) {
            sortingItems.add(createNode("A-Z", "ascModel"));
            sortingItems.add(createNode("Z-A", "descModel"));
            sortingItems.add(createNode("ზრდადობა", "ascQuantity"));
            sortingItems.add(createNode("კლებადობა", "descQuantity"));
        } else {
            sortingItems.add(createNode("A-Z", "ascModel"));
            sortingItems.add(createNode("Z-A", "descModel"));
            sortingItems.add(createNode("Ascending", "ascQuantity"));
            sortingItems.add(createNode("Descending", "descQuantity"));
        }
        return sortingItems;
    }

    public JsonNode getDefaultSort(String lang) {
        return getSortingItems(lang).get(3);
    }


    private JsonNode createTopFiveNode(String name, Object code) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("name", name);
        if (code instanceof String) {
            node.put("code", (String) code);
        } else {
            node.put("code", (Integer) code);
        }
        return node;
    }

    public String getMain(String key, String lang) {
        if ("ka".equalsIgnoreCase(lang)) {
            return switch (key) {
                case "currency_usd_th"-> "Thousand US dollars";
                case "currency_gel_th"-> "Thousand GEL";
                case "main.year" -> "წელი";
                case "main.sort" -> "სორტირება";
                case "main.search" -> "ძებნა";
                case "main.indicator" -> "მაჩვენებელი";
                case "main.fuel_type" -> "საწვავის ტიპი";
                case "main.quarter" -> "კვარტალი";
                case "main.brand" -> "მარკა";
                case "main.model" -> "მოდელი";
                case "main.year_of_prod" -> "გამოშვების წელი";
                case "main.region" -> "რეგიონი";
                case "main.All" -> "ყველა";
                default -> key;
            };
        }
        return switch (key) {
            case "currency_usd_th"-> "ათასი აშშ დოლარი";
            case "currency_gel_th"-> "ათასი ლარი";
            case "main.year" -> "Year";
            case "main.sort" -> "Sort";
            case "main.search" -> "Search";
            case "main.indicator" -> "Indicator";
            case "main.fuel_type" -> "Fuel Type";
            case "main.quarter" -> "Quarter";
            case "main.brand" -> "Brand";
            case "main.model" -> "Model";
            case "main.year_of_prod" -> "Year of Production";
            case "main.region" -> "Region";
            case "main.All" -> "All";
            default -> key;
        };
    }

    public SelectorValue getAll(String lang) {
        return new SelectorValue("ka".equalsIgnoreCase(lang) ? "ყველა" : "All", null);
    }

    public String getQuarter(String key, String lang) {
        if ("ka".equalsIgnoreCase(lang)) {
            return switch (key) {
                case "1" -> "I";
                case "2" -> "II";
                case "3" -> "III";
                case "4" -> "IV";
                default -> key;
            };
        }
        return switch (key) {
            case "1" -> "I";
            case "2" -> "II";
            case "3" -> "III";
            case "4" -> "IV";
            default -> key;
        };
    }

    public JsonNode getStackedAreaIndicators(String lang) {
        ArrayNode indicators = objectMapper.createArrayNode();
        if ("ka".equalsIgnoreCase(lang)) {
            indicators.add(createNode("მარკა", "brand"));
            indicators.add(createNode("მოდელი", "model"));
            indicators.add(createNode("გამოშვების წელი", "year_of_production"));
            indicators.add(createNode("საწვავის ტიპი", "fuel"));
            indicators.add(createNode("ძარის ტიპი", "body"));
            indicators.add(createNode("ძრავის მოცულობა", "engine"));
            indicators.add(createNode("ფერი", "color"));
        } else {
            indicators.add(createNode("Brand", "brand"));
            indicators.add(createNode("Model", "model"));
            indicators.add(createNode("Year Of Production", "year_of_production"));
            indicators.add(createNode("Fuel Type", "fuel"));
            indicators.add(createNode("Body Type", "body"));
            indicators.add(createNode("Engine", "engine"));
            indicators.add(createNode("Color", "color"));
        }
        return indicators;
    }

    public JsonNode getDefaultStackedAreaIndicator(String lang) {
        return getStackedAreaIndicators(lang).get(0);
    }

    public JsonNode getExportImport(String lang) {
        ArrayNode exportImport = objectMapper.createArrayNode();
        if ("ka".equalsIgnoreCase(lang)) {
            exportImport.add(createNode("ექსპორტი", "E"));
            exportImport.add(createNode("იმპორტი", "I"));
        } else {
            exportImport.add(createNode("Export", "E"));
            exportImport.add(createNode("Import", "I"));
        }
        return exportImport;
    }

    public JsonNode getDefaultExportImport(String lang) {
        return getExportImport(lang).get(1);
    }

    public JsonNode getVehicleSort(String lang) {
        String[] vehicles = "ka".equalsIgnoreCase(lang) ?
                new String[]{"მეორადი", "ახალი"} :
                new String[]{"Used", "New"};
        return mapSortArray(vehicles);
    }

    public JsonNode getTypeSort(String lang) {
        String[] types = "ka".equalsIgnoreCase(lang) ?
                new String[]{"მსუბუქი", "დანარჩენი", "ყველა"} :
                new String[]{"Passenger Cars", "Others", "All"};
        return mapSortArray(types);
    }

    public JsonNode getDefaultTypeSort(String lang) {
        return getTypeSort(lang).get(2);
    }

    public JsonNode getCurrencySort(String lang) {
        String[] currencies = new String[]{"GEL1000", "USD1000"};
        ArrayNode result = objectMapper.createArrayNode();
        for (String currency : currencies) {
            result.add(createNode(currency, currency));
        }
        return result;
    }

    private JsonNode mapSortArray(String[] array) {
        ArrayNode result = objectMapper.createArrayNode();
        for (int i = 0; i < array.length; i++) {
            result.add(createNode(array[i], i + 1));
        }
        return result;
    }

    JsonNode createNode(String name, Object code) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("name", name);
        if (code instanceof String) {
            node.put("code", (String) code);
        } else {
            node.put("code", (Integer) code);
        }
        return node;
    }
}
