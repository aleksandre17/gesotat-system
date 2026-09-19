package org.base.api.service.platform;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import org.base.api.security.tenancy.TenantAccessGuards;

class PlatformSnapshotPreparationServiceTest {
    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void rejectsArtifactThatDoesNotBelongToDatasetLoadBeforeWriting() {
        JdbcTemplate dataPlane = mock(JdbcTemplate.class);
        when(dataPlane.query(contains("JOIN ingest.artifact"), any(ResultSetExtractor.class), eq(900L), eq(73L)))
                .thenReturn(null);
        PlatformSnapshotPreparationService service = new PlatformSnapshotPreparationService(dataPlane, TenantAccessGuards.permitAll());

        assertThrows(IllegalArgumentException.class,
                () -> service.prepare(new PrepareSnapshotRequest(73L, 900L, "a".repeat(64))));

        verify(dataPlane).query(contains("JOIN ingest.artifact"), any(ResultSetExtractor.class), eq(900L), eq(73L));
    }

    @Test
    void rejectsMalformedChecksumBeforeDatabaseAccess() {
        JdbcTemplate dataPlane = mock(JdbcTemplate.class);
        PlatformSnapshotPreparationService service = new PlatformSnapshotPreparationService(dataPlane, TenantAccessGuards.permitAll());

        assertThrows(IllegalArgumentException.class,
                () -> service.prepare(new PrepareSnapshotRequest(73L, 900L, "not-a-checksum")));

        org.mockito.Mockito.verifyNoInteractions(dataPlane);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void rejectsChecksumThatDiffersFromTheLoadArtifact() throws Exception {
        JdbcTemplate dataPlane = mock(JdbcTemplate.class);
        String storedChecksum = "b".repeat(64);
        when(dataPlane.query(contains("JOIN ingest.artifact"), any(ResultSetExtractor.class), eq(900L), eq(73L)))
                .thenAnswer(call -> extract(call.getArgument(1), contextRow(73L, "VALIDATED", storedChecksum)));
        PlatformSnapshotPreparationService service = new PlatformSnapshotPreparationService(dataPlane, TenantAccessGuards.permitAll());

        assertThrows(IllegalArgumentException.class,
                () -> service.prepare(new PrepareSnapshotRequest(73L, 900L, "a".repeat(64))));
        verify(dataPlane).query(contains("JOIN ingest.artifact"), any(ResultSetExtractor.class), eq(900L), eq(73L));
        verifyNoMoreInteractions(dataPlane);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void returnsAnExistingSnapshotOnlyWhenItsArtifactProvenanceIsConsistent() throws Exception {
        JdbcTemplate dataPlane = mock(JdbcTemplate.class);
        String checksum = "b".repeat(64);
        when(dataPlane.query(contains("JOIN ingest.artifact"), any(ResultSetExtractor.class), eq(900L), eq(73L)))
                .thenAnswer(call -> extract(call.getArgument(1), contextRow(73L, "PREPARED", checksum)));
        when(dataPlane.query(contains("FROM publication.dataset_snapshot"), any(ResultSetExtractor.class), eq(73L)))
                .thenAnswer(call -> extract(call.getArgument(1), snapshotRow(990L, 73L, 2L, checksum)));
        when(dataPlane.queryForObject(contains("AND artifact_id<>?"), eq(Long.class), eq(990L), eq(900L))).thenReturn(0L);
        when(dataPlane.queryForObject(contains("raw.source_record WHERE dataset_snapshot_id=?"), eq(Long.class), eq(990L))).thenReturn(2L);

        long result = new PlatformSnapshotPreparationService(dataPlane, TenantAccessGuards.permitAll())
                .prepare(new PrepareSnapshotRequest(73L, 900L, checksum));

        assertEquals(990L, result);
        verify(dataPlane).query(contains("JOIN ingest.artifact"), any(ResultSetExtractor.class), eq(900L), eq(73L));
        verify(dataPlane).query(contains("FROM publication.dataset_snapshot"), any(ResultSetExtractor.class), eq(73L));
        verify(dataPlane).queryForObject(contains("raw.source_record WHERE dataset_snapshot_id=?"), eq(Long.class), eq(990L));
        verify(dataPlane).queryForObject(contains("AND artifact_id<>?"), eq(Long.class), eq(990L), eq(900L));
        verifyNoMoreInteractions(dataPlane);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void replayRejectsRowsFromAnotherArtifact() throws Exception {
        JdbcTemplate dataPlane = replayFixture("b".repeat(64), 73L, 2L);
        when(dataPlane.queryForObject(contains("AND artifact_id<>?"), eq(Long.class), eq(990L), eq(900L))).thenReturn(1L);
        assertThrows(IllegalStateException.class, () -> new PlatformSnapshotPreparationService(dataPlane, TenantAccessGuards.permitAll())
                .prepare(new PrepareSnapshotRequest(73L, 900L, "b".repeat(64))));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void replayRejectsRowCountDriftAndVersionOrChecksumConflict() throws Exception {
        JdbcTemplate drift = replayFixture("b".repeat(64), 73L, 3L);
        assertThrows(IllegalStateException.class, () -> new PlatformSnapshotPreparationService(drift, TenantAccessGuards.permitAll())
                .prepare(new PrepareSnapshotRequest(73L, 900L, "b".repeat(64))));
        JdbcTemplate version = replayFixture("b".repeat(64), 74L, 2L);
        assertThrows(IllegalStateException.class, () -> new PlatformSnapshotPreparationService(version, TenantAccessGuards.permitAll())
                .prepare(new PrepareSnapshotRequest(73L, 900L, "b".repeat(64))));
        JdbcTemplate checksum = replayFixture("c".repeat(64), 73L, 2L);
        assertThrows(IllegalStateException.class, () -> new PlatformSnapshotPreparationService(checksum, TenantAccessGuards.permitAll())
                .prepare(new PrepareSnapshotRequest(73L, 900L, "b".repeat(64))));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void freshPrepareRequiresValidatedLoad() throws Exception {
        JdbcTemplate dataPlane = freshFixture("STAGING");
        assertThrows(IllegalStateException.class, () -> new PlatformSnapshotPreparationService(dataPlane, TenantAccessGuards.permitAll())
                .prepare(new PrepareSnapshotRequest(73L, 900L, "b".repeat(64))));
        verify(dataPlane, org.mockito.Mockito.never()).queryForObject(contains("INSERT INTO publication.dataset_snapshot"), eq(Long.class), any(), any(), any(), any());
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void freshPrepareStoresArtifactChecksumAndFailsOnPartialRowCopyOrLostStatusRace() throws Exception {
        JdbcTemplate ok = freshFixture("VALIDATED");
        when(ok.update(contains("INSERT INTO raw.source_record"), any(), any(), any())).thenReturn(2);
        when(ok.update(contains("SET status='PREPARED'"), any(Object[].class))).thenReturn(1);
        assertEquals(990L, new PlatformSnapshotPreparationService(ok, TenantAccessGuards.permitAll()).prepare(new PrepareSnapshotRequest(73L, 900L, "B".repeat(64))));
        verify(ok).queryForObject(contains("INSERT INTO publication.dataset_snapshot"), eq(Long.class), eq(73L), eq(73L), eq(2L), eq("b".repeat(64)));

        JdbcTemplate partial = freshFixture("VALIDATED");
        when(partial.update(contains("INSERT INTO raw.source_record"), any(), any(), any())).thenReturn(1);
        assertThrows(IllegalStateException.class, () -> new PlatformSnapshotPreparationService(partial, TenantAccessGuards.permitAll()).prepare(new PrepareSnapshotRequest(73L, 900L, "b".repeat(64))));

        JdbcTemplate raced = freshFixture("VALIDATED");
        when(raced.update(contains("INSERT INTO raw.source_record"), any(), any(), any())).thenReturn(2);
        when(raced.update(contains("SET status='PREPARED'"), any(Object[].class))).thenReturn(0);
        assertThrows(IllegalStateException.class, () -> new PlatformSnapshotPreparationService(raced, TenantAccessGuards.permitAll()).prepare(new PrepareSnapshotRequest(73L, 900L, "b".repeat(64))));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void existingSnapshotLookupHoldsARangeLock() throws Exception {
        JdbcTemplate dataPlane = replayFixture("b".repeat(64), 73L, 2L);
        new PlatformSnapshotPreparationService(dataPlane, TenantAccessGuards.permitAll()).prepare(new PrepareSnapshotRequest(73L, 900L, "b".repeat(64)));
        verify(dataPlane).query(contains("WITH (UPDLOCK,HOLDLOCK) WHERE dataset_load_id=?"), any(ResultSetExtractor.class), eq(73L));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static JdbcTemplate replayFixture(String snapshotChecksum, long snapshotVersion, long rowCount) throws Exception {
        JdbcTemplate dataPlane = mock(JdbcTemplate.class);
        when(dataPlane.query(contains("JOIN ingest.artifact"), any(ResultSetExtractor.class), eq(900L), eq(73L)))
                .thenAnswer(call -> extract(call.getArgument(1), contextRow(73L, "PREPARED", "b".repeat(64))));
        when(dataPlane.query(contains("FROM publication.dataset_snapshot"), any(ResultSetExtractor.class), eq(73L)))
                .thenAnswer(call -> extract(call.getArgument(1), snapshotRow(990L, snapshotVersion, rowCount, snapshotChecksum)));
        when(dataPlane.queryForObject(contains("AND artifact_id<>?"), eq(Long.class), eq(990L), eq(900L))).thenReturn(0L);
        when(dataPlane.queryForObject(contains("raw.source_record WHERE dataset_snapshot_id=?"), eq(Long.class), eq(990L))).thenReturn(2L);
        return dataPlane;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static JdbcTemplate freshFixture(String loadStatus) throws Exception {
        JdbcTemplate dataPlane = mock(JdbcTemplate.class);
        when(dataPlane.query(contains("JOIN ingest.artifact"), any(ResultSetExtractor.class), eq(900L), eq(73L)))
                .thenAnswer(call -> extract(call.getArgument(1), contextRow(73L, loadStatus, "b".repeat(64))));
        when(dataPlane.query(contains("FROM publication.dataset_snapshot"), any(ResultSetExtractor.class), eq(73L))).thenReturn(null);
        when(dataPlane.queryForObject(contains("FROM ingest.staged_row"), eq(Long.class), eq(73L))).thenReturn(2L);
        when(dataPlane.queryForObject(contains("INSERT INTO publication.dataset_snapshot"), eq(Long.class), any(), any(), any(), any())).thenReturn(990L);
        return dataPlane;
    }

    private static ResultSet contextRow(long version, String status, String checksum) throws Exception {
        ResultSet row = mock(ResultSet.class);
        when(row.next()).thenReturn(true);
        when(row.getLong(1)).thenReturn(version);
        when(row.getString(2)).thenReturn(status);
        when(row.getString(3)).thenReturn(checksum);
        return row;
    }

    private static ResultSet snapshotRow(long id, long version, long rowCount, String checksum) throws Exception {
        ResultSet row = mock(ResultSet.class);
        when(row.next()).thenReturn(true);
        when(row.getLong(1)).thenReturn(id);
        when(row.getLong(2)).thenReturn(version);
        when(row.getLong(3)).thenReturn(rowCount);
        when(row.getString(4)).thenReturn(checksum);
        return row;
    }

    private static Object extract(Object extractor, ResultSet row) throws Exception {
        return ((ResultSetExtractor<?>) extractor).extractData(row);
    }
}
