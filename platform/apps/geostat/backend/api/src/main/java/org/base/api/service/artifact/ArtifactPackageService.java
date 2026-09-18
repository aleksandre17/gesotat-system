package org.base.api.service.artifact;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Package boundary: turns a package (an existing storage inventory or an uploaded ZIP) into an
 * accepted, verified manifest. Bytes go to Object Storage under checksum keys; the Data Plane gets
 * identity and lineage only.
 */
@Service
public class ArtifactPackageService {
    private static final Logger log = LoggerFactory.getLogger(ArtifactPackageService.class);

    private final ObjectProvider<ArtifactObjectStore> store;
    private final ArtifactRegistry registry;
    private final ObjectMapper json;
    private final ArtifactMetrics metrics;
    private final ArtifactProperties properties;
    private final ArtifactContentTypeVerifier contentTypes;

    public record ManifestReceipt(long manifestId, boolean created, String packageChecksum, int entryCount, int objectCount,
                                  int verified, int missing, int checksumMismatch) {}

    public ArtifactPackageService(ObjectProvider<ArtifactObjectStore> store, ArtifactRegistry registry, ObjectMapper json, ArtifactMetrics metrics,
                                  ArtifactProperties properties, ArtifactContentTypeVerifier contentTypes) {
        this.store = store;
        this.registry = registry;
        this.json = json;
        this.metrics = metrics;
        this.properties = properties;
        this.contentTypes = contentTypes;
    }

    /**
     * Adopts objects that already live in Object Storage, described by an inventory JSON array of
     * {@code {originalPath, sha256, bytes}}. Object keys are recomputed from {@code objectPrefix};
     * any key recorded inside the inventory is not trusted.
     */
    public ManifestReceipt importInventory(String packageCode, String inventoryKey, String objectPrefix) {
        ArtifactObjectStore objects = requireStore();
        ArtifactObjectStore.ObjectLocation inventoryLocation = new ArtifactObjectStore.ObjectLocation(objects.ingestBucket(), inventoryKey);
        List<ArtifactManifestGenerator.InventoryEntry> inventory = new ArrayList<>();
        try {
            JsonNode root = json.readTree(objects.read(inventoryLocation, properties.getMaxInventoryBytes()));
            if (!root.isArray()) throw new IllegalArgumentException("Inventory must be a JSON array");
            for (JsonNode node : root) {
                inventory.add(new ArtifactManifestGenerator.InventoryEntry(node.path("originalPath").asText(null),
                        node.path("sha256").asText(null), node.path("bytes").asLong(-1)));
            }
        } catch (IOException invalid) {
            throw new IllegalArgumentException("Inventory is not valid JSON", invalid);
        }
        ArtifactManifest manifest = ArtifactManifestGenerator.generate(packageCode, objects.ingestBucket(), objectPrefix,
                "s3://" + inventoryLocation.bucket() + "/" + inventoryLocation.key(), inventory);
        return registerAndVerify(manifest);
    }

    /** Accepts a ZIP package: every entry is stored under its checksum key, then manifested and verified. */
    public ManifestReceipt uploadPackage(String packageCode, InputStream zip) {
        ArtifactObjectStore objects = requireStore();
        String objectPrefix = properties.getUploadPrefix();
        List<ArtifactManifestGenerator.InventoryEntry> inventory = new ArrayList<>();
        long packageBytes = 0;
        Path buffer = null;
        try (ZipInputStream entries = new ZipInputStream(zip)) {
            buffer = Files.createTempFile("artifact-entry-", ".bin");
            for (ZipEntry entry; (entry = entries.getNextEntry()) != null; ) {
                if (entry.isDirectory()) continue;
                if (inventory.size() >= properties.getMaxPackageEntries()) throw new IllegalArgumentException("Package exceeds " + properties.getMaxPackageEntries() + " entries");
                String path = ArtifactManifestGenerator.normalizePath(entry.getName());
                String extension = ArtifactKeys.extensionOf(path);
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                long size;
                try (OutputStream out = new DigestOutputStream(Files.newOutputStream(buffer), digest)) {
                    size = copyBounded(entries, out, properties.getMaxEntryBytes());
                }
                packageBytes += size;
                if (packageBytes > properties.getMaxPackageBytes()) throw new IllegalArgumentException("Package exceeds " + properties.getMaxPackageBytes() + " uncompressed bytes");
                String sha = hex(digest.digest());
                try (InputStream content = Files.newInputStream(buffer)) {
                    contentTypes.verify(path, content);
                }
                try (InputStream content = Files.newInputStream(buffer)) {
                    objects.putContentAddressed(objectPrefix, sha, extension, MediaTypes.forFileName(path), content, size);
                }
                inventory.add(new ArtifactManifestGenerator.InventoryEntry(path, sha, size));
            }
        } catch (IllegalArgumentException | ArtifactStorageException error) {
            throw error;
        } catch (Exception error) {
            throw new IllegalArgumentException("Package is not a readable ZIP archive", error);
        } finally {
            if (buffer != null) try { Files.deleteIfExists(buffer); } catch (IOException ignored) { /* temp cleanup is best effort */ }
        }
        return registerAndVerify(ArtifactManifestGenerator.generate(packageCode, objects.ingestBucket(), objectPrefix, "upload:" + packageCode, inventory));
    }

    /** Re-checks every object of a manifest against Object Storage (existence and full SHA-256). */
    public ManifestReceipt verify(long manifestId) {
        if (!registry.manifestExists(manifestId)) throw new ArtifactNotFoundException("Manifest " + manifestId + " not found");
        return verify(manifestId, false, null);
    }

    private ManifestReceipt registerAndVerify(ArtifactManifest manifest) {
        ArtifactObjectStore objects = requireStore();
        // Inventory import is an independent trust boundary: validate every staged object's
        // bytes before creating a manifest that can later be bound to a published snapshot.
        for (ArtifactManifest.Entry entry : manifest.entries()) {
            try (InputStream content = objects.open(new ArtifactObjectStore.ObjectLocation(entry.bucket(), entry.objectKey()))) {
                contentTypes.verify(entry.originalPath(), content);
            } catch (IOException error) {
                throw new ArtifactStorageException("Artifact content could not be inspected", error);
            }
        }
        ArtifactRegistry.Registration registration = registry.register(manifest);
        log.info("artifact.manifest registered id={} created={} package={} checksum={} entries={}", registration.manifestId(),
                registration.created(), manifest.packageCode(), manifest.packageChecksum(), manifest.entries().size());
        return verify(registration.manifestId(), registration.created(), manifest.packageChecksum());
    }

    private ManifestReceipt verify(long manifestId, boolean created, String packageChecksum) {
        ArtifactObjectStore objects = requireStore();
        List<ArtifactRegistry.StoredEntry> entries = registry.entries(manifestId);
        Map<Long, VerificationStatus> statusByObject = new HashMap<>();
        for (ArtifactRegistry.StoredEntry stored : entries) {
            if (statusByObject.containsKey(stored.artifactObjectId())) continue;
            ArtifactManifest.Entry entry = stored.entry();
            ArtifactObjectStore.ObjectLocation location = new ArtifactObjectStore.ObjectLocation(entry.bucket(), entry.objectKey());
            var stat = objects.stat(location);
            VerificationStatus status = stat.isEmpty() ? VerificationStatus.MISSING
                    : stat.get().byteSize() != entry.byteSize() || !objects.sha256(location).equals(entry.sha256()) ? VerificationStatus.CHECKSUM_MISMATCH
                    : VerificationStatus.VERIFIED;
            registry.markVerification(stored.artifactObjectId(), status);
            statusByObject.put(stored.artifactObjectId(), status);
        }
        Map<VerificationStatus, Integer> tally = new java.util.EnumMap<>(VerificationStatus.class);
        for (VerificationStatus status : statusByObject.values()) tally.merge(status, 1, Integer::sum);
        tally.forEach((status, count) -> metrics.verification(status.name(), count));
        int verified = tally.getOrDefault(VerificationStatus.VERIFIED, 0);
        int missing = tally.getOrDefault(VerificationStatus.MISSING, 0);
        int mismatch = tally.getOrDefault(VerificationStatus.CHECKSUM_MISMATCH, 0);
        log.info("artifact.manifest verified id={} objects={} verified={} missing={} mismatch={}", manifestId, statusByObject.size(), verified, missing, mismatch);
        return new ManifestReceipt(manifestId, created, packageChecksum, entries.size(), statusByObject.size(), verified, missing, mismatch);
    }

    private ArtifactObjectStore requireStore() {
        ArtifactObjectStore objects = store.getIfAvailable();
        if (objects == null) throw new ArtifactStorageException("Object Storage is not configured", null);
        return objects;
    }

    private static long copyBounded(InputStream in, OutputStream out, long limit) throws IOException {
        byte[] buffer = new byte[64 * 1024];
        long total = 0;
        for (int read; (read = in.read(buffer)) > 0; ) {
            total += read;
            if (total > limit) throw new IllegalArgumentException("Package entry exceeds " + limit + " bytes");
            out.write(buffer, 0, read);
        }
        return total;
    }

    private static String hex(byte[] digest) {
        StringBuilder out = new StringBuilder(64);
        for (byte b : digest) out.append(String.format("%02x", b));
        return out.toString();
    }
}
