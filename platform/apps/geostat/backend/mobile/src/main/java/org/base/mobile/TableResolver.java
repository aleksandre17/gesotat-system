package org.base.mobile;

import org.springframework.stereotype.Component;

/**
 * Resolves the SQL Server table name from a boolean parameter.
 * <p>
 * Centralizes the repeated {@code table ? "[dbo].[auto_main]" : "[dbo].[eoyes]"} pattern
 * used across ~15 controller endpoints.
 */
@Component
public class TableResolver {

    private static final String TABLE_EOYES = "[dbo].[eoyes]";
    private static final String TABLE_AUTO_MAIN = "[dbo].[auto_main]";

    /**
     * @param useAutoMain if {@code true}, returns auto_main; otherwise eoyes.
     *                    {@code null} is treated as {@code false}.
     */
    public String resolve(Boolean useAutoMain) {
        return Boolean.TRUE.equals(useAutoMain) ? TABLE_AUTO_MAIN : TABLE_EOYES;
    }
}

