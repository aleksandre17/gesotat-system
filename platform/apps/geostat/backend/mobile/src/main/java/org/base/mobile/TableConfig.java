package org.base.mobile;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@ConfigurationProperties(prefix = "app.tables")
public class TableConfig {
    private final Set<String> allowedTables = Set.of(
            "main_auto",
            "main_tree_map",
            "secondary_tree_map",
            "vehicles1000",
            "vehicle_imp_exp",
            "others_imp_exp",
            "country_cl",
            "region_cl"
    );
    private final Set<String> allowedLangKeys = Set.of("en", "ka");

    public Set<String> getAllowedTables() {
        return allowedTables;
    }

    public Set<String> getAllowedLangKeys() {
        return allowedLangKeys;
    }
}
