package org.base.api.service.mobile_services;

import org.apache.catalina.core.ApplicationContext;
import org.base.api.repository.mobile.GlobalRepository;
import org.base.api.service.mobile_services.strategy.TableSelectionStrategy;
import org.base.core.model.ClassificationTable;
import org.base.core.service.QueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Generic service for classification data (e.g., fuels, engines).
 */
@Service
public class ClassificationService {
    @Autowired
    private GlobalRepository autoRepository;
    @Autowired
    private TableConfig tableConfig;
    @Autowired
    private LanguageService languageService;

    @Autowired
    private org.springframework.context.ApplicationContext context;

    /**
     * Retrieves classification data for a given column and table.
     * @param year Year filter (optional).
     * @param quarter Quarter filter (optional).
     * @param langName Language column (e.g., name_en).
     * @param tableName Table name.
     * @param clTable Classification table (e.g., engine_cl).
     * @param column Column to aggregate (e.g., engine).
     * @param sorter Custom sorter for results.
     * @return List of classification DTOs.
     */
    @Transactional(readOnly = true)
    public <T> List<T> getClassificationData(
            Integer year,
            String quarter,
            String langName,
            String tableName,
            ClassificationTable clTable,
            String column,
            BiFunction<List<Map<String, Object>>, String, List<T>> mapper,
            Comparator<T> sorter) {
//        if (!tableConfig.getAllowedTables().contains(tableName)) {
//            throw new IllegalArgumentException("Invalid or unsupported table name");
//        }
//        if (!tableConfig.getAllowedLangKeys().contains(langName.replace("name_", ""))) {
//            throw new IllegalArgumentException("Invalid or unsupported language name");
//        }

        // Resolve strategy
        TableSelectionStrategy strategy = getStrategy(tableName);
        Integer finalYear = strategy.determineYear(autoRepository, year).orElseThrow(() -> new IllegalStateException("No data available"));
        List<Integer> quarters = strategy.determineQuarters(quarter);

        // Build query
        QueryBuilder query = new QueryBuilder()
                .append("SELECT t.").append(column).append(", ")
                .append(clTable.getAlias()).append(".").append(langName)
                .append(" AS item_name, SUM(t.quantity) AS value ")
                .append("FROM ").append(tableName).append(" t ");
        query.addJoin(clTable, langName, "t")
                .append(" WHERE 1=1")
                .addFilter("year", finalYear, "=")
                .addFilter(new QueryBuilder.InFilter("t.quarter", quarters))
                .append(" GROUP BY t.").append(column).append(", ")
                .append(clTable.getAlias()).append(".").append(langName)
                .append(" ORDER BY value DESC");

        // Execute query
        List<Map<String, Object>> results = autoRepository.executeQuery(query);

        // Map and sort results
        List<T> data = mapper.apply(results, langName);
        if (sorter != null) {
            data.sort(sorter);
        }

        return data;
    }

    private TableSelectionStrategy getStrategy(String tableName) {
        if (MAIN_TABLE.equals(tableName) || ENGINE_TABLE.equals(tableName)) {
            return context.getBean("eoyesTabelStrategy", TableSelectionStrategy.class).setTableName(tableName);
        }
        // Add other strategies as needed
        throw new IllegalArgumentException("No strategy defined for table: " + tableName);
    }


    private static final String MAIN_TABLE = "[dbo].[eoyes]";
    private static final String ENGINE_TABLE = "[dbo].[auto_main]";
}
