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
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArtifactPackageServiceTest {

    /** In-memory adapter: proves the service depends only on the port. */
    static final class MemoryStore implements ArtifactObjectStore {
        final Map<String, byte[]> objects = new HashMap<>();
        public String ingestBucket() { return "geostat-ingest"; }
        public Optional<ObjectStat> stat(ObjectLocation l) { byte[] b = objects.get(l.key()); return b == null ? Optional.empty() : Optional.of(new ObjectStat(b.length, "x")); }
        public String sha256(ObjectLocation l) { return sha(objects.get(l.key())); }
        public byte[] read(ObjectLocation l, int max) { return objects.get(l.key()); }
        public ObjectLocation putContentAddressed(String prefix, String sha, String ext, String media, InputStream in, long size) {
            try { String key = ArtifactKeys.contentKey(prefix, sha, ext); objects.putIfAbsent(key, in.readAllBytes()); return new ObjectLocation("geostat-ingest", key); }
            catch (java.io.IOException e) { throw new IllegalStateException(e); }
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
        ObjectProvider<ArtifactObjectStore> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(store);
        return new ArtifactPackageService(provider, registry, new ObjectMapper(), new ArtifactMetrics(mock(ObjectProvider.class)), new ArtifactProperties());
    }

    @Test
    void uploadStoresEveryEntryInThePlatformPoolAndDeduplicatesContent() throws Exception {
        MemoryStore store = new MemoryStore();
        ArtifactRegistry registry = mock(ArtifactRegistry.class);
        when(registry.register(any())).thenReturn(new ArtifactRegistry.Registration(9, true));
        when(registry.entries(9)).thenReturn(List.of());
        service(store, registry).uploadPackage("PKG", new ByteArrayInputStream(zip(Map.of("files/a.xlsx", "same", "files/b.xlsx", "same", "c.csv", "other"))));

        assertEquals(2, store.objects.size(), "identical bytes are stored once");
        assertTrue(store.objects.keySet().stream().allMatch(k -> k.startsWith(new ArtifactProperties().getUploadPrefix())));
        ArgumentCaptor<ArtifactManifest> manifest = ArgumentCaptor.forClass(ArtifactManifest.class);
        verify(registry).register(manifest.capture());
        assertEquals(3, manifest.getValue().entries().size());
        assertEquals(sha("same".getBytes(StandardCharsets.UTF_8)), manifest.getValue().entries().stream()
                .filter(e -> e.originalPath().equals("files/a.xlsx")).findFirst().orElseThrow().sha256());
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
}
