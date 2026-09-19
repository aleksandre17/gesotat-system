package org.base.api.service.artifact;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthmarketscience.jackcess.ColumnBuilder;
import com.healthmarketscience.jackcess.DataType;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.TableBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Package-time relation preview: contract-declared identity, shared matcher, fail-closed before any write. */
class ArtifactPackageRelationPreviewTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final ArtifactMatchRules RULES = new ArtifactMatchRules(List.of(new SourcePathMatchRuleParser()));
    private static final String CSV = MediaTypes.forFileName("a.csv");
    private static final ArtifactPackageContractResolver.DatasetContract CONTRACT = new ArtifactPackageContractResolver.DatasetContract(
            "SITE_A", 3, "c".repeat(64), 77, "RECORDS", "__ent_records", List.of("record_id", "file_path"), List.of("record_id"));

    private static final ArtifactManifestDocuments DOCUMENTS = mock(ArtifactManifestDocuments.class);

    private Path access;

    @AfterEach
    void cleanUp() throws Exception {
        if (access != null) Files.deleteIfExists(access);
    }

    private Path access(String[]... rows) throws Exception {
        access = Files.createTempFile("preview-", ".accdb");
        try (Database database = DatabaseBuilder.create(Database.FileFormat.V2010, access.toFile())) {
            var table = new TableBuilder("__ent_records").addColumn(new ColumnBuilder("record_id", DataType.TEXT))
                    .addColumn(new ColumnBuilder("file_path", DataType.TEXT)).toTable(database);
            for (String[] row : rows) table.addRow((Object[]) row);
        }
        return access;
    }

    private static ArtifactRelationDescriptor descriptor() throws Exception {
        return new ArtifactRelationDescriptor(77, "PRIMARY_FILE", "PRIMARY", 1, 1, false,
                JSON.readTree("{\"type\":\"SOURCE_PATH\",\"stripPrefix\":\"files/\",\"packageRoot\":\"docs/\",\"bindings\":[{\"language\":\"und\",\"field\":\"file_path\"}]}"),
                new ArtifactRelationDescriptor.Policy("POLICY", 1, "AUTHENTICATED", null, Set.of(CSV), 1024, 300, "RETAIN_INDEFINITE"));
    }

    private byte[] zip(Map<String, byte[]> entries) throws Exception {
        var out = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(out)) {
            for (var e : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(e.getKey()));
                zip.write(e.getValue());
                zip.closeEntry();
            }
        }
        return out.toByteArray();
    }

    @SuppressWarnings("unchecked")
    private static ArtifactPackageService service(ArtifactPackageServiceTest.MemoryStore store, ArtifactRegistry registry) throws Exception {
        ObjectProvider<ArtifactObjectStore> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(store);
        ArtifactPackageContractResolver datasets = mock(ArtifactPackageContractResolver.class);
        when(datasets.resolve("SITE_A", 3, "RECORDS")).thenReturn(CONTRACT);
        ArtifactContractResolver relations = mock(ArtifactContractResolver.class);
        when(relations.approved(77)).thenReturn(List.of(descriptor().toDefinition(RULES)));
        return new ArtifactPackageService(provider, registry, JSON, new ArtifactMetrics(mock(ObjectProvider.class)), new ArtifactProperties(),
                new ArtifactContentTypeVerifier(), datasets, new PackageDatasetCarriers(java.util.List.of(new AccessDatasetCarrier(new ArtifactAccessPackageValidator()))), relations, DOCUMENTS);
    }

    private Map<String, byte[]> packageWith(String... files) throws Exception {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("dataset.accdb", Files.readAllBytes(access));
        for (String file : files) entries.put(file, "id,value\n1,2\n".getBytes(StandardCharsets.UTF_8));
        return entries;
    }

    @Test
    void rowIdentityIsTheContractDeclaredKey() throws Exception {
        var rows = ArtifactAccessRowReader.read(access(new String[]{"128", "files/a.csv"}).toFile(), CONTRACT);
        assertEquals("128", rows.get(0).externalKey());
        assertEquals("files/a.csv", rows.get(0).payload().path("file_path").asText());
    }

    @Test
    void duplicateOrUndeclaredKeysFailClosed() throws Exception {
        var file = access(new String[]{"1", "files/a.csv"}, new String[]{"1", "files/b.csv"}).toFile();
        assertThrows(IllegalArgumentException.class, () -> ArtifactAccessRowReader.read(file, CONTRACT));
        var keyless = new ArtifactPackageContractResolver.DatasetContract("SITE_A", 3, "c".repeat(64), 77, "RECORDS", "__ent_records", List.of());
        assertThrows(IllegalArgumentException.class, () -> ArtifactAccessRowReader.read(file, keyless));
    }

    @Test
    void descriptorRoundTripsIntoTheSameDefinition() throws Exception {
        var descriptor = new ArtifactPackageDescriptor(ArtifactPackageDescriptor.SCHEMA, CONTRACT, List.of(descriptor()));
        var restored = JSON.readValue(JSON.writeValueAsBytes(descriptor), ArtifactPackageDescriptor.class);
        assertEquals(descriptor, restored);
        assertEquals("docs/a.csv", restored.relations().get(0).toDefinition(RULES).matchRule().resolve("files/a.csv"));
    }

    @Test
    void unmatchedRowBlocksAdmissionBeforeAnyObjectIsWritten() throws Exception {
        access(new String[]{"1", "files/a.csv"}, new String[]{"2", "files/missing.csv"});
        var store = new ArtifactPackageServiceTest.MemoryStore();
        ArtifactRegistry registry = mock(ArtifactRegistry.class);
        var blocked = assertThrows(ArtifactRelationPreviewException.class, () -> service(store, registry)
                .uploadPackage("PKG", "SITE_A", 3, "RECORDS", new ByteArrayInputStream(zip(packageWith("docs/a.csv")))));
        assertEquals(ArtifactIssue.Code.UNMATCHED_ROW, blocked.relations().get(0).issues().get(0).code());
        assertTrue(store.objects.isEmpty());
        verify(registry, never()).register(any());
    }

    /** The manifest a conformant producer ships: exactly what the platform derives from the same package. */
    private byte[] exactManifest(Map<String, byte[]> entries, String rowKey, String path) throws Exception {
        List<PackageManifestDocument.FileClaim> files = new java.util.ArrayList<>();
        for (var entry : entries.entrySet())
            files.add(new PackageManifestDocument.FileClaim(entry.getKey(), Sha256.of(new ByteArrayInputStream(entry.getValue())), entry.getValue().length, MediaTypes.forFileName(entry.getKey())));
        return JSON.writeValueAsBytes(new PackageManifestDocument(PackageManifestDocument.SCHEMA, "SITE_A", 3, "RECORDS", "dataset.accdb", files,
                List.of(new PackageManifestDocument.EdgeClaim(rowKey, "PRIMARY_FILE", "und", 1, path))));
    }

    @Test
    void shippedManifestIsAcceptedOnlyWhenItIsExactlyWhatThePlatformDerives() throws Exception {
        access(new String[]{"1", "files/a.csv"});
        var store = new ArtifactPackageServiceTest.MemoryStore();
        ArtifactRegistry registry = mock(ArtifactRegistry.class);
        when(registry.register(any())).thenReturn(new ArtifactRegistry.Registration(5, true));
        when(registry.entries(5)).thenReturn(List.of());
        Map<String, byte[]> entries = packageWith("docs/a.csv");
        Map<String, byte[]> withManifest = new LinkedHashMap<>(entries);
        withManifest.put(PackageManifestDocument.ENTRY_NAME, exactManifest(entries, "1", "docs/a.csv"));

        var receipt = service(store, registry).uploadPackage("PKG", "SITE_A", 3, "RECORDS", new ByteArrayInputStream(zip(withManifest)));

        assertEquals(1, receipt.relations().get(0).edgeCount());
        var registered = org.mockito.ArgumentCaptor.forClass(ArtifactManifest.class);
        verify(registry).register(registered.capture());
        assertEquals(2, registered.getValue().entries().size(), "the manifest describes the package and is not package content");
    }

    @Test
    void shippedManifestThatMisstatesFilesOrRowBindingsBlocksAdmissionBeforeAnyWrite() throws Exception {
        access(new String[]{"1", "files/a.csv"});
        Map<String, byte[]> entries = packageWith("docs/a.csv");
        for (byte[] claim : List.of(exactManifest(entries, "2", "docs/a.csv"), exactManifest(packageWith("docs/a.csv", "docs/ghost.csv"), "1", "docs/a.csv"),
                "{\"schema\":\"other\"}".getBytes(StandardCharsets.UTF_8))) {
            var store = new ArtifactPackageServiceTest.MemoryStore();
            ArtifactRegistry registry = mock(ArtifactRegistry.class);
            Map<String, byte[]> withManifest = new LinkedHashMap<>(entries);
            withManifest.put(PackageManifestDocument.ENTRY_NAME, claim);
            assertThrows(IllegalArgumentException.class, () -> service(store, registry)
                    .uploadPackage("PKG", "SITE_A", 3, "RECORDS", new ByteArrayInputStream(zip(withManifest))));
            assertTrue(store.objects.isEmpty());
            verify(registry, never()).register(any());
        }
    }

    @Test
    void matchedPackageIsAdmittedWithItsRelationPreview() throws Exception {
        access(new String[]{"1", "files/a.csv"});
        var store = new ArtifactPackageServiceTest.MemoryStore();
        ArtifactRegistry registry = mock(ArtifactRegistry.class);
        when(registry.register(any())).thenReturn(new ArtifactRegistry.Registration(5, true));
        when(registry.entries(5)).thenReturn(List.of());
        var receipt = service(store, registry).uploadPackage("PKG", "SITE_A", 3, "RECORDS", new ByteArrayInputStream(zip(packageWith("docs/a.csv"))));
        assertEquals(1, receipt.relations().get(0).edgeCount());
        assertEquals(2, store.objects.size());
    }

    @Test
    void previewWritesNothingAndReportsOrphans() throws Exception {
        access(new String[]{"1", "files/a.csv"});
        var store = new ArtifactPackageServiceTest.MemoryStore();
        ArtifactRegistry registry = mock(ArtifactRegistry.class);
        var preview = service(store, registry).previewPackage("PKG", "SITE_A", 3, "RECORDS",
                new ByteArrayInputStream(zip(packageWith("docs/a.csv", "docs/unused.csv"))));
        assertFalse(preview.blocked());
        assertEquals(1, preview.relations().get(0).orphanCount());
        assertEquals(3, preview.entryCount());
        assertTrue(store.objects.isEmpty());
        verify(registry, never()).register(any());
    }
}
