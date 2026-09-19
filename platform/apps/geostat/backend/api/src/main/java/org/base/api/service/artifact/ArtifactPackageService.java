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
    private final ArtifactPackageContractResolver packageContracts;
    private final PackageDatasetCarriers datasetCarriers;
    private final ArtifactContractResolver relationContracts;
    private final ArtifactManifestDocuments documents;

    /** Admission receipt; {@code relations} is the package-time relation preview (empty for inventory imports). */
    public record ManifestReceipt(long manifestId, boolean created, String packageChecksum, int entryCount, int objectCount,
                                  int verified, int missing, int checksumMismatch, List<ArtifactRelationPreview.RelationResult> relations) {
        public ManifestReceipt(long manifestId, boolean created, String packageChecksum, int entryCount, int objectCount,
                               int verified, int missing, int checksumMismatch) {
            this(manifestId, created, packageChecksum, entryCount, objectCount, verified, missing, checksumMismatch, List.of());
        }

        public ManifestReceipt {
            relations = List.copyOf(relations);
        }

        ManifestReceipt withRelations(List<ArtifactRelationPreview.RelationResult> evaluated) {
            return new ManifestReceipt(manifestId, created, packageChecksum, entryCount, objectCount, verified, missing, checksumMismatch, evaluated);
        }
    }

    /** Result of a validate-only admission; nothing was stored or registered. */
    public record PackagePreview(String packageChecksum, int entryCount, boolean blocked, List<ArtifactRelationPreview.RelationResult> relations) {
        public PackagePreview {
            relations = List.copyOf(relations);
        }
    }

    public ArtifactPackageService(ObjectProvider<ArtifactObjectStore> store, ArtifactRegistry registry, ObjectMapper json, ArtifactMetrics metrics,
                                  ArtifactProperties properties, ArtifactContentTypeVerifier contentTypes,
                                  ArtifactPackageContractResolver packageContracts,
                                  PackageDatasetCarriers datasetCarriers,
                                  ArtifactContractResolver relationContracts,
                                  ArtifactManifestDocuments documents) {
        this.store = store;
        this.registry = registry;
        this.json = json;
        this.metrics = metrics;
        this.properties = properties;
        this.contentTypes = contentTypes;
        this.packageContracts = packageContracts;
        this.datasetCarriers = datasetCarriers;
        this.relationContracts = relationContracts;
        this.documents = documents;
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

    /** Canonical contract-bound upload; the contract determines the dataset structure. */
    public ManifestReceipt uploadPackage(String packageCode, String contractCode, int revision, String datasetCode, InputStream zip) {
        ArtifactPackageContractResolver.DatasetContract contract = packageContracts.resolve(contractCode, revision, datasetCode);
        return uploadPackage(packageCode, zip, contract);
    }

    /**
     * Validate-only admission (contract §26 "Validate → Confirm"): stages and checks the package, generates
     * its manifest and evaluates every approved relation, but writes no object and registers nothing.
     */
    public PackagePreview previewPackage(String packageCode, String contractCode, int revision, String datasetCode, InputStream zip) {
        ArtifactPackageContractResolver.DatasetContract contract = packageContracts.resolve(contractCode, revision, datasetCode);
        StagedPackage staged = stage(zip, contract);
        try {
            ArtifactManifest manifest = manifest(packageCode, staged.inventory(), contract);
            ArtifactRelationPreview.Report report = preview(staged, manifest, contract);
            return new PackagePreview(manifest.packageChecksum(), manifest.entries().size(), report.blocked(), report.relations());
        } finally {
            deleteStagingDirectory(staged.directory());
        }
    }

    private ManifestReceipt uploadPackage(String packageCode, InputStream zip, ArtifactPackageContractResolver.DatasetContract contract) {
        ArtifactObjectStore objects = requireStore();
        StagedPackage staged = stage(zip, contract);
        try {
            ArtifactManifest manifest = manifest(packageCode, staged.inventory(), contract);
            List<ArtifactRelationPreview.RelationResult> relations = List.of();
            PackageManifestDocument accepted = null;
            if (contract == null && staged.suppliedManifest() != null)
                throw new IllegalArgumentException("A supplied " + PackageManifestDocument.ENTRY_NAME + " can only be verified by a contract-bound admission");
            if (contract != null) {
                // Relation ambiguity is rejected before any byte is written (contract §6/§12).
                ArtifactRelationPreview.Report report = preview(staged, manifest, contract);
                if (report.blocked()) throw new ArtifactRelationPreviewException(report.relations(), report.errors().size());
                relations = report.relations();
                accepted = PackageManifestDocument.describe(contract, staged.dataset().originalPath(), manifest.entries(), report.plans());
            }
            for (StagedEntry entry : staged.entries()) {
                try (InputStream content = Files.newInputStream(entry.path())) {
                    objects.putContentAddressed(properties.getUploadPrefix(), entry.sha256(), entry.extension(),
                            MediaTypes.forFileName(entry.originalPath()), content, entry.byteSize());
                }
            }
            ManifestReceipt receipt = registerAndVerify(manifest, false).withRelations(relations);
            // Persisted once per manifest; a replayed admission finds it already recorded (contract §25).
            if (accepted != null) documents.record(receipt.manifestId(), accepted);
            return receipt;
        } catch (IOException failure) {
            throw new ArtifactStorageException("Validated package content could not be stored", failure);
        } finally {
            deleteStagingDirectory(staged.directory());
        }
    }

    /** Expands the archive into bounded private staging; path, type, size and contract structure are checked per entry. */
    private StagedPackage stage(InputStream zip, ArtifactPackageContractResolver.DatasetContract contract) {
        List<ArtifactManifestGenerator.InventoryEntry> inventory = new ArrayList<>();
        List<StagedEntry> stagedEntries = new ArrayList<>();
        long packageBytes = 0;
        StagedDataset dataset = null;
        PackageManifestDocument suppliedManifest = null;
        Path stagingDirectory = null;
        try (ZipInputStream entries = new ZipInputStream(zip)) {
            stagingDirectory = Files.createTempDirectory("artifact-package-");
            for (ZipEntry entry; (entry = entries.getNextEntry()) != null; ) {
                if (entry.isDirectory()) continue;
                if (inventory.size() >= properties.getMaxPackageEntries()) throw new IllegalArgumentException("Package exceeds " + properties.getMaxPackageEntries() + " entries");
                String path = ArtifactManifestGenerator.normalizePath(entry.getName());
                if (PackageManifestDocument.ENTRY_NAME.equals(path)) {
                    if (suppliedManifest != null) throw new IllegalArgumentException("Package contains more than one " + PackageManifestDocument.ENTRY_NAME);
                    suppliedManifest = readSuppliedManifest(entries);
                    continue;
                }
                String extension = ArtifactKeys.extensionOf(path);
                Path staged = Files.createTempFile(stagingDirectory, "entry-", ".bin");
                MessageDigest digest = Sha256.newDigest();
                long size;
                try (OutputStream out = new DigestOutputStream(Files.newOutputStream(staged), digest)) {
                    size = copyBounded(entries, out, properties.getMaxEntryBytes());
                }
                packageBytes += size;
                if (packageBytes > properties.getMaxPackageBytes()) throw new IllegalArgumentException("Package exceeds " + properties.getMaxPackageBytes() + " uncompressed bytes");
                String sha = Sha256.hex(digest);
                contentTypes.verify(path, staged);
                var carrier = contract == null ? java.util.Optional.<PackageDatasetCarrier>empty() : datasetCarriers.forExtension(extension);
                if (carrier.isPresent()) {
                    if (dataset != null) throw new IllegalArgumentException("Contract-bound package must contain exactly one dataset file");
                    carrier.get().validate(staged.toFile(), contract);
                    dataset = new StagedDataset(path, staged, carrier.get());
                }
                inventory.add(new ArtifactManifestGenerator.InventoryEntry(path, sha, size));
                stagedEntries.add(new StagedEntry(path, extension, sha, size, staged));
            }
            if (contract != null && dataset == null)
                throw new IllegalArgumentException("Contract-bound package must contain exactly one dataset file");
            return new StagedPackage(stagingDirectory, List.copyOf(inventory), List.copyOf(stagedEntries), dataset, suppliedManifest);
        } catch (IllegalArgumentException | ArtifactStorageException error) {
            deleteStagingDirectory(stagingDirectory);
            throw error;
        } catch (Exception error) {
            deleteStagingDirectory(stagingDirectory);
            throw new IllegalArgumentException("Package is not a readable ZIP archive", error);
        }
    }

    private ArtifactManifest manifest(String packageCode, List<ArtifactManifestGenerator.InventoryEntry> inventory,
                                      ArtifactPackageContractResolver.DatasetContract contract) {
        ArtifactObjectStore objects = requireStore();
        return contract == null
                ? ArtifactManifestGenerator.generate(packageCode, objects.ingestBucket(), properties.getUploadPrefix(), "upload:" + packageCode, inventory)
                : ArtifactManifestGenerator.generate(packageCode, objects.ingestBucket(), properties.getUploadPrefix(), "upload:" + packageCode,
                    inventory, contract.contractCode(), contract.revision(), contract.datasetVersionId());
    }

    /** Evaluates the dataset's approved relations over the package's own Access rows and entries. */
    private ArtifactRelationPreview.Report preview(StagedPackage staged, ArtifactManifest manifest,
                                                   ArtifactPackageContractResolver.DatasetContract contract) {
        List<ArtifactRelationDefinition> definitions = relationContracts.approved(contract.datasetVersionId());
        ArtifactRelationPreview.Report report;
        try {
            report = definitions.isEmpty() ? ArtifactRelationPreview.evaluate(List.of(), List.of(), manifest.entries())
                    : ArtifactRelationPreview.evaluate(definitions, DeclaredRowValues.packageRows(definitions, staged.dataset().carrier(), staged.dataset().path().toFile(), contract), manifest.entries());
        } catch (IOException unreadable) {
            throw new IllegalArgumentException("Package dataset rows could not be read", unreadable);
        }
        requireExactSuppliedManifest(staged, manifest, contract, report);
        return report;
    }

    /** A shipped manifest is a claim: it is accepted only when it is exactly what the platform derived (contract §25). */
    private static void requireExactSuppliedManifest(StagedPackage staged, ArtifactManifest manifest,
                                                     ArtifactPackageContractResolver.DatasetContract contract, ArtifactRelationPreview.Report report) {
        if (staged.suppliedManifest() == null) return;
        PackageManifestDocument derived = PackageManifestDocument.describe(contract, staged.dataset().originalPath(), manifest.entries(), report.plans());
        List<String> differences = staged.suppliedManifest().differencesFrom(derived);
        if (!differences.isEmpty())
            throw new IllegalArgumentException("Supplied " + PackageManifestDocument.ENTRY_NAME + " does not describe this package: " + String.join("; ", differences));
    }

    private PackageManifestDocument readSuppliedManifest(InputStream entry) throws IOException {
        byte[] bytes = entry.readNBytes(properties.getMaxInventoryBytes() + 1);
        if (bytes.length > properties.getMaxInventoryBytes())
            throw new IllegalArgumentException(PackageManifestDocument.ENTRY_NAME + " exceeds " + properties.getMaxInventoryBytes() + " bytes");
        try {
            return json.readValue(bytes, PackageManifestDocument.class);
        } catch (IOException | IllegalArgumentException invalid) {
            throw new IllegalArgumentException(PackageManifestDocument.ENTRY_NAME + " is not a valid package manifest", invalid);
        }
    }

    private record StagedPackage(Path directory, List<ArtifactManifestGenerator.InventoryEntry> inventory, List<StagedEntry> entries, StagedDataset dataset, PackageManifestDocument suppliedManifest) {}

    private record StagedDataset(String originalPath, Path path, PackageDatasetCarrier carrier) {}

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
                    if (copied != entry.byteSize() || !Sha256.of(staged).equals(entry.sha256()))
                        throw new IllegalArgumentException("Inventory object checksum or size does not match: " + entry.originalPath());
                    contentTypes.verify(entry.originalPath(), staged);
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
}
