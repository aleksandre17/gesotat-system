package org.base.api.service.artifact;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import com.healthmarketscience.jackcess.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArtifactPackageServiceTest {

    /** In-memory adapter: proves the service depends only on the port. */
    static final class MemoryStore implements ArtifactObjectStore {
        final Map<String, byte[]> objects = new HashMap<>();
        final Map<String, byte[]> quarantine = new HashMap<>();
        public String ingestBucket() { return "geostat-ingest"; }
        public Optional<ObjectStat> stat(ObjectLocation l) { byte[] b = objects.get(l.key()); return b == null ? Optional.empty() : Optional.of(new ObjectStat(b.length, "x")); }
        public String sha256(ObjectLocation l) { return sha(objects.get(l.key())); }
        public InputStream open(ObjectLocation l) { return new ByteArrayInputStream(objects.get(l.key())); }
        public byte[] read(ObjectLocation l, int max) { return objects.get(l.key()); }
        public ObjectLocation putContentAddressed(String prefix, String sha, String ext, String media, InputStream in, long size) {
            try { String key = ArtifactKeys.contentKey(prefix, sha, ext); objects.putIfAbsent(key, in.readAllBytes()); return new ObjectLocation("geostat-ingest", key); }
            catch (java.io.IOException e) { throw new IllegalStateException(e); }
        }
        public ObjectLocation putQuarantined(String sha, InputStream in, long size) {
            try {
                String key = ArtifactKeys.contentKey("malware/sha256/", sha, "bin");
                quarantine.putIfAbsent(key, in.readAllBytes());
                return new ObjectLocation("geostat-quarantine", key);
            } catch (java.io.IOException e) { throw new IllegalStateException(e); }
        }
        public URI presignGet(ObjectLocation l, Duration ttl, String name, String media) { throw new UnsupportedOperationException(); }
    }

    static String sha(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }

    private static byte[] zip(Map<String, String> files) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            for (Map.Entry<String, String> f : files.entrySet()) { zip.putNextEntry(new ZipEntry(f.getKey())); zip.write(f.getValue().getBytes(StandardCharsets.UTF_8)); zip.closeEntry(); }
        }
        return out.toByteArray();
    }

    @SuppressWarnings("unchecked")
    private static ArtifactPackageService service(MemoryStore store, ArtifactRegistry registry) {
        return service(store, registry, path -> new ArtifactMalwareScanner.ScanResult(ArtifactMalwareScanner.Verdict.CLEAN, ""));
    }

    @SuppressWarnings("unchecked")
    private static ArtifactPackageService service(MemoryStore store, ArtifactRegistry registry, ArtifactMalwareScanner scanner) {
        return service(store, registry, scanner, mock(ArtifactQuarantineRegistry.class));
    }

    private static ArtifactPackageService service(MemoryStore store, ArtifactRegistry registry, ArtifactMalwareScanner scanner,
                                                  ArtifactQuarantineRegistry quarantineRegistry) {
        return service(store, registry, scanner, quarantineRegistry, mock(ArtifactPackageContractResolver.class));
    }

    @SuppressWarnings("unchecked")
    private static ArtifactPackageService service(MemoryStore store, ArtifactRegistry registry, ArtifactMalwareScanner scanner,
                                                  ArtifactQuarantineRegistry quarantineRegistry,
                                                  ArtifactPackageContractResolver packageContracts) {
        ObjectProvider<ArtifactObjectStore> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(store);
        ObjectProvider<ArtifactMalwareScanner> scannerProvider = mock(ObjectProvider.class);
        when(scannerProvider.getIfAvailable()).thenReturn(scanner);
        return new ArtifactPackageService(provider, registry, new ObjectMapper(), new ArtifactMetrics(mock(ObjectProvider.class)), new ArtifactProperties(), new ArtifactContentTypeVerifier(), scannerProvider, quarantineRegistry,
                packageContracts, new ArtifactAccessPackageValidator());
    }

    @Test
    void uploadStoresEveryEntryInThePlatformPoolAndDeduplicatesContent() throws Exception {
        MemoryStore store = new MemoryStore();
        ArtifactRegistry registry = mock(ArtifactRegistry.class);
        when(registry.register(any())).thenReturn(new ArtifactRegistry.Registration(9, true));
        when(registry.entries(9)).thenReturn(List.of());
        service(store, registry).uploadPackage("PKG", new ByteArrayInputStream(zip(Map.of("files/a.csv", "same", "files/b.csv", "same", "c.csv", "other"))));

        assertEquals(2, store.objects.size(), "identical bytes are stored once");
        assertTrue(store.objects.keySet().stream().allMatch(k -> k.startsWith(new ArtifactProperties().getUploadPrefix())));
        ArgumentCaptor<ArtifactManifest> manifest = ArgumentCaptor.forClass(ArtifactManifest.class);
        verify(registry).register(manifest.capture());
        assertEquals(3, manifest.getValue().entries().size());
        assertEquals(sha("same".getBytes(StandardCharsets.UTF_8)), manifest.getValue().entries().stream()
                .filter(e -> e.originalPath().equals("files/a.csv")).findFirst().orElseThrow().sha256());
    }

    @Test
    void traversalEntryRejectsWholePackage() throws Exception {
        ArtifactRegistry registry = mock(ArtifactRegistry.class);
        assertThrows(IllegalArgumentException.class, () -> service(new MemoryStore(), registry)
                .uploadPackage("PKG", new ByteArrayInputStream(zip(Map.of("../escape.xlsx", "x")))));
        verify(registry, never()).register(any());
    }

    @Test
    void notAZipIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service(new MemoryStore(), mock(ArtifactRegistry.class))
                .uploadPackage("PKG", new ByteArrayInputStream("not a zip".getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    void contractBoundUploadValidatesAccessStructureAndPersistsContractIdentity() throws Exception {
        MemoryStore store = new MemoryStore();
        ArtifactRegistry registry = mock(ArtifactRegistry.class);
        when(registry.register(any())).thenReturn(new ArtifactRegistry.Registration(9, true));
        when(registry.entries(9)).thenReturn(List.of());
        ObjectProvider<ArtifactObjectStore> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(store);
        ObjectProvider<ArtifactMalwareScanner> scannerProvider = mock(ObjectProvider.class);
        when(scannerProvider.getIfAvailable()).thenReturn(path -> new ArtifactMalwareScanner.ScanResult(ArtifactMalwareScanner.Verdict.CLEAN, ""));
        ArtifactPackageContractResolver resolver = mock(ArtifactPackageContractResolver.class);
        var contract = new ArtifactPackageContractResolver.DatasetContract("SITE_A", 2, "a".repeat(64), 41,
                "RECORDS", "records", List.of("record_id", "title"));
        when(resolver.resolve("SITE_A", 2, "RECORDS")).thenReturn(contract);
        ArtifactPackageService service = new ArtifactPackageService(provider, registry, new ObjectMapper(), new ArtifactMetrics(mock(ObjectProvider.class)),
                new ArtifactProperties(), new ArtifactContentTypeVerifier(), scannerProvider, mock(ArtifactQuarantineRegistry.class),
                resolver, new ArtifactAccessPackageValidator());
        var accessFile = Files.createTempFile("contract-upload-", ".accdb");
        var archive = new ByteArrayOutputStream();
        try {
            try (Database database = DatabaseBuilder.create(Database.FileFormat.V2010, accessFile.toFile())) {
                new TableBuilder("records").addColumn(new ColumnBuilder("record_id", DataType.TEXT))
                        .addColumn(new ColumnBuilder("title", DataType.TEXT)).toTable(database).addRow("1", "example");
            }
            try (ZipOutputStream zip = new ZipOutputStream(archive)) {
                zip.putNextEntry(new ZipEntry("data.accdb"));
                Files.copy(accessFile, zip);
                zip.closeEntry();
            }
            service.uploadPackage("PACKAGE", "SITE_A", 2, "RECORDS", new ByteArrayInputStream(archive.toByteArray()));
            ArgumentCaptor<ArtifactManifest> manifest = ArgumentCaptor.forClass(ArtifactManifest.class);
            verify(registry).register(manifest.capture());
            assertEquals("SITE_A", manifest.getValue().contractCode());
            assertEquals(2, manifest.getValue().contractRevision());
            assertEquals(41L, manifest.getValue().datasetVersionId());
        } finally {
            Files.deleteIfExists(accessFile);
        }
    }

    @Test
    void extensionCannotMasqueradeAsContentType() throws Exception {
        ArtifactRegistry registry = mock(ArtifactRegistry.class);
        assertThrows(IllegalArgumentException.class, () -> service(new MemoryStore(), registry)
                .uploadPackage("PKG", new ByteArrayInputStream(zip(Map.of("report.pdf", "plain text, not a PDF")))));
        verify(registry, never()).register(any());
    }

    @Test
    void invalidContractPackageDoesNotWriteContentObjects() throws Exception {
        MemoryStore store = new MemoryStore();
        ArtifactRegistry registry = mock(ArtifactRegistry.class);
        ArtifactPackageContractResolver contracts = mock(ArtifactPackageContractResolver.class);
        when(contracts.resolve("SITE_A", 2, "RECORDS")).thenReturn(
                new ArtifactPackageContractResolver.DatasetContract("SITE_A", 2, "checksum", 73, "RECORDS", "resource"));

        assertThrows(IllegalArgumentException.class, () -> service(store, registry,
                path -> new ArtifactMalwareScanner.ScanResult(ArtifactMalwareScanner.Verdict.CLEAN, ""),
                mock(ArtifactQuarantineRegistry.class), contracts).uploadPackage("PACKAGE", "SITE_A", 2, "RECORDS",
                new ByteArrayInputStream(zip(Map.of("resource/file.csv", "valid,csv\n1,2")))));

        assertTrue(store.objects.isEmpty(), "package structure must pass before any accepted content is written");
        verify(registry, never()).register(any());
    }

    @Test
    void inventoryImportRejectsMisrepresentedBytesBeforeManifestRegistration() {
        MemoryStore store = new MemoryStore();
        ArtifactRegistry registry = mock(ArtifactRegistry.class);
        byte[] bytes = "plain text, not a PDF".getBytes(StandardCharsets.UTF_8);
        String sha = sha(bytes);
        store.objects.put("inventory.json", ("[{\"originalPath\":\"report.pdf\",\"sha256\":\"" + sha
                + "\",\"bytes\":" + bytes.length + "}]").getBytes(StandardCharsets.UTF_8));
        store.objects.put(ArtifactKeys.contentKey("kids/r8/resources/", sha, "pdf"), bytes);

        assertThrows(IllegalArgumentException.class,
                () -> service(store, registry).importInventory("PKG", "inventory.json", "kids/r8/resources/"));
        verify(registry, never()).register(any());
    }

    @Test
    void malwareVerdictBlocksStorageAndRegistryWrites() throws Exception {
        MemoryStore store = new MemoryStore();
        ArtifactRegistry registry = mock(ArtifactRegistry.class);
        ArtifactQuarantineRegistry quarantineRegistry = mock(ArtifactQuarantineRegistry.class);
        ArtifactMalwareScanner infected = path -> new ArtifactMalwareScanner.ScanResult(ArtifactMalwareScanner.Verdict.INFECTED, "test-signature");

        assertThrows(ArtifactMalwareDetectedException.class, () -> service(store, registry, infected, quarantineRegistry)
                .uploadPackage("PKG", new ByteArrayInputStream(zip(Map.of("report.csv", "safe-looking text")))));
        assertTrue(store.objects.isEmpty(), "infected content must not enter the ingest object pool");
        assertEquals(1, store.quarantine.size(), "infected content is retained only in private quarantine storage");
        verify(registry, never()).register(any());
        verify(quarantineRegistry).record(anyString(), anyLong(), any(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void requiredScannerFailureBlocksManifestRegistration() throws Exception {
        MemoryStore store = new MemoryStore();
        ArtifactRegistry registry = mock(ArtifactRegistry.class);
        ArtifactMalwareScanner unavailable = path -> { throw new ArtifactScannerUnavailableException("test outage"); };

        assertThrows(ArtifactScannerUnavailableException.class, () -> service(store, registry, unavailable)
                .uploadPackage("PKG", new ByteArrayInputStream(zip(Map.of("report.csv", "safe-looking text")))));
        assertTrue(store.objects.isEmpty());
        verify(registry, never()).register(any());
    }
}
