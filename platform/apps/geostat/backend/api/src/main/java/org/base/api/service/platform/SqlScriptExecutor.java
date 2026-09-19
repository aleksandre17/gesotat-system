package org.base.api.service.platform;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.StatementCallback;

import java.sql.ResultSet;

/**
 * Executes a multi-statement script and fails when <em>any</em> statement fails. A driver reports the error
 * of a later statement only while its results are read; executing the batch without draining them records
 * a script as applied although part of it never ran.
 */
public final class SqlScriptExecutor {
    private SqlScriptExecutor() {}

    public static void execute(JdbcTemplate jdbc, String script) {
        jdbc.execute((StatementCallback<Void>) statement -> {
            boolean hasResultSet = statement.execute(script);
            while (true) {
                if (hasResultSet) {
                    try (ResultSet results = statement.getResultSet()) {
                        while (results.next()) { /* drained so the next statement's outcome becomes visible */ }
                    }
                } else if (statement.getUpdateCount() == -1) {
                    return null;
                }
                hasResultSet = statement.getMoreResults();
            }
        });
    }
}
