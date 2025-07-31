package org.base.api.service.mobile_services;

import org.base.core.model.ClassificationTable;
import org.base.core.model.ClassificationTableType;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class FilterResolver {
    private static final Map<String, ClassificationTableType> FILTER_TO_CL_TABLE = Map.of(
            "fuel", ClassificationTableType.FUEL,
            "color", ClassificationTableType.COLOR,
            "body", ClassificationTableType.BODY,
            "engine", ClassificationTableType.ENGINE
    );

    private static final Set<String> VALID_FILTERS = Set.of(
            "fuel", "color", "body", "engine", "year_of_production", "model", "brand"
    );

    public FilterConfig resolve(String filter) {
        String resolvedFilter = (filter == null || filter.isEmpty() || !VALID_FILTERS.contains(filter)) ? "brand" : filter;
        ClassificationTableType clTable = FILTER_TO_CL_TABLE.get(resolvedFilter);
        boolean needsJoin = clTable != null;
        boolean hasHex = clTable != null && clTable.hasHexCode();
        String column = needsJoin ? clTable.getKeyColumn() : resolvedFilter;

        return new FilterConfig(resolvedFilter, column, clTable, needsJoin, hasHex);
    }

    public static class FilterConfig {
        private final String filter;
        private final String column;
        private final ClassificationTable classificationTable;
        private final boolean needsJoin;
        private final boolean hasHex;

        public FilterConfig(String filter, String column, ClassificationTable classificationTable,
                            boolean needsJoin, boolean hasHex) {
            this.filter = filter;
            this.column = column;
            this.classificationTable = classificationTable;
            this.needsJoin = needsJoin;
            this.hasHex = hasHex;
        }

        public String getFilter() { return filter; }
        public String getColumn() { return column; }
        public ClassificationTable getClassificationTable() { return classificationTable; }
        public boolean needsJoin() { return needsJoin; }
        public boolean hasHex() { return hasHex; }
    }
}
