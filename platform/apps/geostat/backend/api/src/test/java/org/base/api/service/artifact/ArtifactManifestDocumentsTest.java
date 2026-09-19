package org.base.api.service.artifact;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArtifactManifestDocumentsTest {
    private static final PackageManifestDocument DOCUMENT = new PackageManifestDocument(PackageManifestDocument.SCHEMA, "SITE_A", 3, "RECORDS", "dataset.accdb",
            List.of(new PackageManifestDocument.FileClaim("docs/a.csv", "a".repeat(64), 12, "text/csv")),
            List.of(new PackageManifestDocument.EdgeClaim("1", "PRIMARY_FILE", "und", 1, "docs/a.csv")));

    private final JdbcTemplate data = mock(JdbcTemplate.class);
    private final ArtifactPackageServiceTest.MemoryStore store = new ArtifactPackageServiceTest.MemoryStore();

    @SuppressWarnings("unchecked")
    private ArtifactManifestDocuments documents(String prefix) {
        ObjectProvider<ArtifactObjectStore> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(store);
        return new ArtifactManifestDocuments(data, provider, new ObjectMapper(), prefix, 1_000_000);
    }

    @Test
    @SuppressWarnings("unchecked")
    void documentIsStoredUnderItsChecksumOutsideTheContentPoolAndPointedToOnce() {
        when(data.query(anyString(), any(RowMapper.class), eq(7L))).thenReturn(List.of());

        var pointer = documents("manifests/").record(7, DOCUMENT);

        assertEquals(1, pointer.fileCount());
        assertEquals(1, pointer.edgeCount());
        assertEquals(1, store.objects.size());
        String key = store.objects.keySet().iterator().next();
        assertTrue(key.contains("manifests/") && key.endsWith(pointer.sha256() + ".json"), key);
        verify(data).update(contains("WHERE NOT EXISTS"), eq(7L), eq(PackageManifestDocument.SCHEMA), eq(pointer.sha256()), eq((int) pointer.byteSize()),
                anyString(), anyString(), eq(1), eq(1), eq(7L));
    }

    @Test
    @SuppressWarnings("unchecked")
    void replayedAdmissionReturnsTheRecordedPointerWithoutWriting() {
        var recorded = new ArtifactManifestDocuments.Pointer(7, "b".repeat(64), 10, 1, 1);
        when(data.query(anyString(), any(RowMapper.class), eq(7L))).thenReturn(List.of(recorded));

        assertEquals(recorded, documents("manifests/").record(7, DOCUMENT));

        assertTrue(store.objects.isEmpty());
        verify(data, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void unsafePrefixIsRejectedAtStartup() {
        assertThrows(IllegalArgumentException.class, () -> documents("../escape/"));
    }
}
