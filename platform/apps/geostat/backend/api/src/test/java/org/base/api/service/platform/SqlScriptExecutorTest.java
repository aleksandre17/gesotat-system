package org.base.api.service.platform;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.AbstractDataSource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SqlScriptExecutorTest {
    private final Statement statement = mock(Statement.class);

    private JdbcTemplate jdbc() throws SQLException {
        Connection connection = mock(Connection.class);
        when(connection.createStatement()).thenReturn(statement);
        return new JdbcTemplate(new AbstractDataSource() {
            @Override public Connection getConnection() { return connection; }
            @Override public Connection getConnection(String username, String password) { return connection; }
        });
    }

    @Test
    void failureOfALaterStatementFailsTheScript() throws Exception {
        when(statement.execute("first; second")).thenReturn(false);
        when(statement.getUpdateCount()).thenReturn(4);
        when(statement.getMoreResults()).thenThrow(new SQLException("ALTER TABLE ALTER COLUMN failed", "S0001", 4922));

        assertThrows(DataAccessException.class, () -> SqlScriptExecutor.execute(jdbc(), "first; second"));
    }

    @Test
    void everyResultIsDrainedUntilTheScriptEnds() throws Exception {
        ResultSet rows = mock(ResultSet.class);
        when(statement.execute("select; update")).thenReturn(true);
        when(statement.getResultSet()).thenReturn(rows);
        when(statement.getMoreResults()).thenReturn(false);
        when(statement.getUpdateCount()).thenReturn(1, -1);

        assertDoesNotThrow(() -> SqlScriptExecutor.execute(jdbc(), "select; update"));
        verify(rows).close();
    }
}
