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
    private final ObjectProvider<ArtifactMalwareScanner> malwareScanners;
    private final ArtifactQuarantineRegistry quarantineRegistry;
    private final ArtifactPackageContractResolver packageContracts;
    private final ArtifactAccessPackageValidator accessPackageValidator;
    private final ArtifactMalwareAdmission malwareAdmission;

    public record ManifestReceipt(long manifestId, boolean created, String packageChecksum, int entryCount, int objectCount,
                                  int verified, int missing, int checksumMismatch) {}

    public ArtifactPackageService(ObjectProvider<ArtifactObjectStore> store, ArtifactRegistry registry, ObjectMapper json, ArtifactMetrics metrics,
                                  ArtifactProperties properties, ArtifactContentTypeVerifier contentTypes,
                                  ObjectProvider<ArtifactMalwareScanner> malwareScanners,
                                  ArtifactQuarantineRegistry quarantineRegistry,
                                  ArtifactPackageContractResolver packageContracts,
                                  ArtifactAccessPackageValidator accessPackageValidator) {
        this.store = store;
        this.registry = registry;
        this.json = json;
        this.metrics = metrics;
        this.properties = properties;
        this.contentTypes = contentTypes;
        this.malwareScanners = malwareScanners;
        this.quarantineRegistry = quarantineRegistry;
        this.packageContracts = packageContracts;
        this.accessPackageValidator = accessPackageValidator;
        this.malwareAdmission = new ArtifactMalwareAdmission(properties, malwareScanners, quarantineRegistry, metrics);
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
        return registerAndVerify(manifest, true);
    }

    /** Accepts a ZIP package: every entry is stored under its checksum key, then manifested and verified. */
    public ManifestReceipt uploadPackage(String packageCode, InputStream zip) {
        return uploadPackage(packageCode, zip, null);
    }

    /** Canonical contract-bound upload; the contract determines the Access table/field structure. */
    public ManifestReceipt uploadPackage(String packageCode, String contractCode, int revision, String datasetCode, InputStream zip) {
        ArtifactPackageContractResolver.DatasetContract contract = packageContracts.resolve(contractCode, revision, datasetCode);
        return uploadPackage(packageCode, zip, contract);
    }

    private ManifestReceipt uploadPackage(String packageCode, InputStream zip, ArtifactPackageContractResolver.DatasetContract contract) {
        ArtifactObjectStore objects = requireStore();
        String objectPrefix = properties.getUploadPrefix();
        List<ArtifactManifestGenerator.InventoryEntry> inventory = new ArrayList<>();
        List<StagedEntry> stagedEntries = new ArrayList<>();
        long packageBytes = 0;
        int accessDatasets = 0;
        Path stagingDirectory = null;
        try (ZipInputStream entries = new ZipInputStream(zip)) {
            stagingDirectory = Files.createTempDirectory("artifact-package-");
            for (ZipEntry entry; (entry = entries.getNextEntry()) != null; ) {
                if (entry.isDirectory()) continue;
                if (inventory.size() >= properties.getMaxPackageEntries()) throw new IllegalArgumentException("Package exceeds " + properties.getMaxPackageEntries() + " entries");
                String path = ArtifactManifestGenerator.normalizePath(entry.getName());
                String extension = ArtifactKeys.extensionOf(path);
                Path staged = Files.createTempFile(stagingDirectory, "entry-", ".bin");
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                long size;
                try (OutputStream out = new DigestOutputStream(Files.newOutputStream(staged), digest)) {
                    size = copyBounded(entries, out, properties.getMaxEntryBytes());
                }
                packageBytes += size;
                if (packageBytes > properties.getMaxPackageBytes()) throw new IllegalArgumentException("Package exceeds " + properties.getMaxPackageBytes() + " uncompressed bytes");
                String sha = hex(digest.digest());
                contentTypes.verify(path, staged);
                if (contract != null && extension.equals("accdb")) {
                    accessPackageValidator.validate(staged.toFile(), contract);
                    accessDatasets++;
                }
                scanForMalware(staged, objects, packageCode, path, sha, size, "ZIP_PACKAGE");
                inventory.add(new ArtifactManifestGenerator.InventoryEntry(path, sha, size));
                stagedEntries.add(new StagedEntry(path, extension, sha, size, staged));
            }
        } catch (IllegalArgumentException | ArtifactStorageException | ArtifactMalwareDetectedException | ArtifactScannerUnavailableException error) {
            deleteStagingDirectory(stagingDirectory);
            throw error;
        } catch (Exception error) {
            deleteStagingDirectory(stagingDirectory);
            throw new IllegalArgumentException("Package is not a readable ZIP archive", error);
        }
        try {
            if (contract != null && accessDatasets != 1)
                throw new IllegalArgumentException("Contract-bound package must contain exactly one Access dataset file");
            for (StagedEntry entry : stagedEntries) {
                try (InputStream content = Files.newInputStream(entry.path())) {
                    objects.putContentAddressed(objectPrefix, entry.sha256(), entry.extension(), MediaTypes.forFileName(entry.originalPath()), content, entry.byteSize());
                }
            }
            ArtifactManifest manifest = contract == null
                    ? ArtifactManifestGenerator.generate(packageCode, objects.ingestBucket(), objectPrefix, "upload:" + packageCode, inventory)
                    : ArtifactManifestGenerator.generate(packageCode, objects.ingestBucket(), objectPrefix, "upload:" + packageCode,
                        inventory, contract.contractCode(), contract.revision(), contract.datasetVersionId());
            return registerAndVerify(manifest, false);
        } catch (IOException failure) {
            throw new ArtifactStorageException("Validated package content could not be stored", failure);
        } finally {
            deleteStagingDirectory(stagingDirectory);
        }
    }

    private record StagedEntry(String originalPath, String extension, String sha256, long byteSize, Path path) {}

    private static void deleteStagingDirectory(Path directory) {
        if (directory == null) return;
        try (var paths = Files.walk(directory)) {
            paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try { Files.deleteIfExists(path); } catch (IOException ignored) { /* best-effort temp cleanup */ }
            });
        } catch (IOException ignored) { /* best-effort temp cleanup */ }
    }

    /** Re-checks every object of a manifest against Object Storage (existence and full SHA-256). */
    public ManifestReceipt verify(long manifestId) {
        if (!registry.manifestExists(manifestId)) throw new ArtifactNotFoundException("Manifest " + manifestId + " not found");
        return verify(manifestId, false, null);
    }

    private ManifestReceipt registerAndVerify(ArtifactManifest manifest, boolean inspectStoredContent) {
        ArtifactObjectStore objects = requireStore();
        // Inventory import is an independent trust boundary: validate every staged object's
        // bytes before creating a manifest that can later be bound to a published snapshot.
        if (inspectStoredContent) {
            for (ArtifactManifest.Entry entry : manifest.entries()) {
                Path staged = null;
                try {
                    var location = new ArtifactObjectStore.ObjectLocation(entry.bucket(), entry.objectKey());
                    var stat = objects.stat(location).orElseThrow(() -> new IllegalArgumentException("Inventory object is missing: " + entry.originalPath()));
                    if (stat.byteSize() != entry.byteSize() || entry.byteSize() > properties.getMaxEntryBytes())
                        throw new IllegalArgumentException("Inventory object size is invalid: " + entry.originalPath());
                    staged = Files.createTempFile("artifact-inspection-", ".bin");
                    long copied;
                    try (InputStream content = objects.open(location); OutputStream out = Files.newOutputStream(staged)) {
                        copied = copyBounded(content, out, properties.getMaxEntryBytes());
                    }
                    if (copied != entry.byteSize() || !sha256(staged).equals(entry.sha256()))
                        throw new IllegalArgumentException("Inventory object checksum or size does not match: " + entry.originalPath());
                    contentTypes.verify(entry.originalPath(), staged);
                    scanForMalware(staged, objects, manifest.packageCode(), entry.originalPath(), entry.sha256(), entry.byteSize(), "INVENTORY_IMPORT");
                } catch (IOException error) {
                    throw new ArtifactStorageException("Artifact content could not be inspected", error);
                } finally {
                    if (staged != null) try { Files.deleteIfExists(staged); } catch (IOException ignored) { /* best-effort temp cleanup */ }
                }
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

    private void scanForMalware(Path content, ArtifactObjectStore objects, String packageCode, String originalPath,
                                String sha256, long byteSize, String sourceType) {
        malwareAdmission.admit(content, objects, packageCode, originalPath, sha256, byteSize, sourceType);
    }

    private static String sha256(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(path)) {
                byte[] bytes = new byte[64 * 1024];
                for (int read; (read = input.read(bytes)) > 0; ) digest.update(bytes, 0, read);
            }
            return hex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            return hex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }


}
