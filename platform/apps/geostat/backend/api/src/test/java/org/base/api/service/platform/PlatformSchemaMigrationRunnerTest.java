package org.base.api.service.platform;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Recorded migrations are never re-executed; new ones run once and are recorded; changed ones fail closed. */
class PlatformSchemaMigrationRunnerTest {
    private final JdbcTemplate control = mock(JdbcTemplate.class);
    private final JdbcTemplate data = mock(JdbcTemplate.class);
    private final JdbcTemplate archive = mock(JdbcTemplate.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);

    private PlatformSchemaMigrationRunner runner() {
        return new PlatformSchemaMigrationRunner(control, data, archive, true, events);
    }

    private static String checksum(String resource) {
        try {
            byte[] text = new ClassPathResource(resource).getInputStream().readAllBytes();
            String decoded = new String(text, StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(decoded.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception error) {
            throw new IllegalStateException(error);
        }
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void recordedMigrationsAreNotReExecuted() throws Exception {
        when(control.query(startsWith("SELECT checksum FROM platform.schema_migration"), any(ResultSetExtractor.class), anyString()))
                .thenAnswer(call -> checksum(call.getArgument(2)));
        runner().run(null);
        verify(control, never()).execute(any(org.springframework.jdbc.core.StatementCallback.class));
        verify(data, never()).execute(any(org.springframework.jdbc.core.StatementCallback.class));
        verify(archive, never()).execute(any(org.springframework.jdbc.core.StatementCallback.class));
        verify(control, never()).update(startsWith("INSERT INTO platform.schema_migration"), any(Object[].class));
        verify(events).publishEvent(any(PlatformSchemaReadyEvent.class));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void newMigrationsRunOnceAndAreRecorded() throws Exception {
        String pending = "db/platform/095_access_package_malware_admission.sql"; // a Data Plane migration
        when(control.query(startsWith("SELECT checksum FROM platform.schema_migration"), any(ResultSetExtractor.class), anyString()))
                .thenAnswer(call -> pending.equals(call.getArgument(2)) ? null : checksum(call.getArgument(2)));
        runner().run(null);
        verify(data, times(1)).execute(any(org.springframework.jdbc.core.StatementCallback.class));
        verify(control, never()).execute(any(org.springframework.jdbc.core.StatementCallback.class));
        verify(control).update(startsWith("INSERT INTO platform.schema_migration"), eq(pending), eq(checksum(pending)));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void changedAppliedMigrationFailsClosed() {
        when(control.query(startsWith("SELECT checksum FROM platform.schema_migration"), any(ResultSetExtractor.class), anyString()))
                .thenReturn("0".repeat(64));
        assertThrows(IllegalStateException.class, () -> runner().run(null));
        verify(events, never()).publishEvent(any());
        verify(control, atLeastOnce()).query(anyString(), any(ResultSetExtractor.class), anyString());
    }
}
