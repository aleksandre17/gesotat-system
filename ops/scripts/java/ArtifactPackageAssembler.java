import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.base.api.service.artifact.ArtifactAccessPackageValidator;
import org.base.api.service.artifact.DeclaredRowValues;
import org.base.api.service.artifact.AccessDatasetCarrier;
import org.base.api.service.artifact.ArtifactAccessRowReader;
import org.base.api.service.artifact.ArtifactIssue;
import org.base.api.service.artifact.ArtifactManifest;
import org.base.api.service.artifact.ArtifactManifestGenerator;
import org.base.api.service.artifact.ArtifactMatchRule;
import org.base.api.service.artifact.ArtifactMatchRuleParser;
import org.base.api.service.artifact.ArtifactMatchRules;
import org.base.api.service.artifact.ArtifactMatcher;
import org.base.api.service.artifact.ArtifactPackageDescriptor;
import org.base.api.service.artifact.ArtifactRelationDefinition;
import org.base.api.service.artifact.ArtifactRelationPreview;
import org.base.api.service.artifact.PackageManifestDocument;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Contract-driven package producer for any site/contract/dataset (ARTIFACT-ATTACHMENT-CONTRACT §2, §24–§25).
 *
 * Input is the approved package descriptor served by
 * {@code GET /platform/artifacts/contracts/{code}/revisions/{rev}/datasets/{dataset}/package-descriptor};
 * nothing about tables, fields, keys, rules, roots or counts is known to this tool. Row identities, rule
 * resolution, cardinality, policy and orphan checks are the platform's own {@link ArtifactMatcher}, so the
 * package this tool emits is exactly the package admission and snapshot binding will accept.
 *
 * Only files referenced by a row are packaged. The build is deterministic (sorted entries, fixed
 * timestamps), is refused when the preview has any ERROR, and is re-read byte-for-byte after writing.
 *
 * Usage: ArtifactPackageAssembler <descriptor.json> <dataset.accdb> <resourceRoot> <package.zip> <evidence.json>
 */
public final class ArtifactPackageAssembler {
    private static final String EVIDENCE_SCHEMA = "geostat.artifact-package-assembly.v1";
    private static final LocalDateTime FIXED_ENTRY_TIME = LocalDateTime.of(1980, 1, 1, 0, 0);
    private static final String PREVIEW_PACKAGE_CODE = "assembly-preview";
    private static final String PREVIEW_BUCKET = "assembly";
    private static final String PREVIEW_PREFIX = "assembly/";

    public static void main(String[] args) throws Exception {
        if (args.length != 5) {
            System.err.println("usage: ArtifactPackageAssembler <descriptor.json> <dataset.accdb> <resourceRoot> <package.zip> <evidence.json>");
            System.exit(64);
        }
        ObjectMapper json = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        ArtifactPackageDescriptor descriptor = json.readValue(Path.of(args[0]).toFile(), ArtifactPackageDescriptor.class);
        Path access = Path.of(args[1]).toAbsolutePath().normalize();
        Path resourceRoot = Path.of(args[2]).toAbsolutePath().normalize();
        Path archive = Path.of(args[3]).toAbsolutePath().normalize();
        Path evidence = Path.of(args[4]).toAbsolutePath().normalize();

        new ArtifactAccessPackageValidator().validate(access.toFile(), descriptor.dataset());
        ArtifactMatchRules rules = discoverRules();
        List<ArtifactRelationDefinition> definitions = descriptor.relations().stream().map(r -> r.toDefinition(rules)).toList();
        if (definitions.isEmpty()) throw new IllegalStateException("Dataset " + descriptor.dataset().datasetCode() + " declares no artifact relations");
        // The same row view admission uses, including attachments a package declares in its own tables.
        List<ArtifactMatcher.SourceRow> rows = DeclaredRowValues.packageRows(definitions,
                new AccessDatasetCarrier(new ArtifactAccessPackageValidator()), access.toFile(), descriptor.dataset());

        // Package path → local source file, for every value a declared rule resolves.
        TreeMap<String, Path> files = new TreeMap<>();
        String accessEntry = ArtifactManifestGenerator.normalizePath(access.getFileName().toString());
        files.put(accessEntry, access);
        for (ArtifactRelationDefinition definition : definitions) {
            ArtifactMatchRule rule = definition.matchRule();
            for (ArtifactMatcher.SourceRow row : rows)
                for (ArtifactMatchRule.Binding binding : rule.bindings())
                    for (String value : ArtifactMatcher.sourceValues(row.payload().path(binding.field()))) {
                        String packagePath;
                        try {
                            packagePath = rule.resolve(value);
                        } catch (IllegalArgumentException unsafe) {
                            continue; // reported by the matcher as UNMATCHED_ROW
                        }
                        if (packagePath == null) continue;
                        Path source = resourceRoot.resolve(packagePath).normalize();
                        if (source.startsWith(resourceRoot) && Files.isRegularFile(source)) files.putIfAbsent(packagePath, source);
                    }
        }

        List<ArtifactManifestGenerator.InventoryEntry> inventory = new ArrayList<>(files.size());
        for (Map.Entry<String, Path> file : files.entrySet())
            inventory.add(new ArtifactManifestGenerator.InventoryEntry(file.getKey(), sha256(file.getValue()), Files.size(file.getValue())));
        var dataset = descriptor.dataset();
        ArtifactManifest manifest = ArtifactManifestGenerator.generate(PREVIEW_PACKAGE_CODE, PREVIEW_BUCKET, PREVIEW_PREFIX, "assembly", inventory,
                dataset.contractCode(), dataset.revision(), dataset.datasetVersionId());

        ArtifactRelationPreview.Report report = ArtifactRelationPreview.evaluate(definitions, rows, manifest.entries());
        report.relations().forEach(r -> {
            System.out.println("relation " + r.relationCode() + ": edges=" + r.edgeCount() + " orphans=" + r.orphanCount() + " issues=" + r.issues().size());
            r.issues().forEach(i -> System.out.println("  " + i.severity() + " " + i.code() + " " + i.subject() + ": " + i.detail()));
        });
        if (report.blocked()) {
            System.err.println("Package refused: " + report.errors().size() + " relation error(s); no archive written");
            System.exit(2);
        }

        // The shipped manifest is the producer's claim; admission accepts it only when it equals what the API derives.
        byte[] shippedManifest = new ObjectMapper().writeValueAsBytes(
                PackageManifestDocument.describe(dataset, accessEntry, manifest.entries(), report.plans()));
        writeArchive(archive, files, shippedManifest);
        verifyArchive(archive, manifest.entries());
        writeEvidence(json, evidence, descriptor, access, archive, manifest, report);
        System.out.println("archive=" + archive + " entries=" + manifest.entries().size() + " sha256=" + sha256(archive) + " bytes=" + Files.size(archive));
    }

    /** Every {@link ArtifactMatchRuleParser} on the classpath, as the API registers them (open for new rule types). */
    private static ArtifactMatchRules discoverRules() throws ReflectiveOperationException {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(ArtifactMatchRuleParser.class));
        List<ArtifactMatchRuleParser> parsers = new ArrayList<>();
        for (var candidate : scanner.findCandidateComponents(ArtifactMatchRuleParser.class.getPackageName()))
            parsers.add((ArtifactMatchRuleParser) Class.forName(candidate.getBeanClassName()).getDeclaredConstructor().newInstance());
        return new ArtifactMatchRules(parsers);
    }

    private static void writeArchive(Path archive, TreeMap<String, Path> files, byte[] shippedManifest) throws IOException {
        Files.createDirectories(archive.getParent());
        Path temporary = Files.createTempFile(archive.getParent(), ".assembly-", ".zip");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(temporary))) {
            zip.setLevel(Deflater.BEST_COMPRESSION);
            for (Map.Entry<String, Path> file : files.entrySet()) {
                ZipEntry entry = new ZipEntry(file.getKey());
                entry.setTimeLocal(FIXED_ENTRY_TIME);
                zip.putNextEntry(entry);
                Files.copy(file.getValue(), zip);
                zip.closeEntry();
            }
            ZipEntry manifestEntry = new ZipEntry(PackageManifestDocument.ENTRY_NAME);
            manifestEntry.setTimeLocal(FIXED_ENTRY_TIME);
            zip.putNextEntry(manifestEntry);
            zip.write(shippedManifest);
            zip.closeEntry();
        }
        Files.move(temporary, archive, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
    }

    private static void verifyArchive(Path archive, List<ArtifactManifest.Entry> expected) throws IOException {
        Map<String, String> byPath = new TreeMap<>();
        expected.forEach(e -> byPath.put(e.originalPath(), e.sha256()));
        int seen = 0;
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(archive))) {
            for (ZipEntry entry; (entry = zip.getNextEntry()) != null; ) {
                if (PackageManifestDocument.ENTRY_NAME.equals(entry.getName())) continue;
                seen++;
                if (!sha256(zip).equals(byPath.get(entry.getName()))) throw new IllegalStateException("Archive entry does not match its source: " + entry.getName());
            }
        }
        if (seen != byPath.size()) throw new IllegalStateException("Archive has " + seen + " entries, expected " + byPath.size());
    }

    private static void writeEvidence(ObjectMapper json, Path evidence, ArtifactPackageDescriptor descriptor, Path access, Path archive,
                                      ArtifactManifest manifest, ArtifactRelationPreview.Report report) throws IOException {
        ObjectNode root = json.createObjectNode();
        root.put("schema", EVIDENCE_SCHEMA);
        root.put("descriptorSchema", descriptor.schema());
        ObjectNode identity = root.putObject("contract");
        identity.put("contractCode", descriptor.dataset().contractCode());
        identity.put("revision", descriptor.dataset().revision());
        identity.put("contractChecksum", descriptor.dataset().contractChecksum());
        identity.put("datasetCode", descriptor.dataset().datasetCode());
        identity.put("datasetVersionId", descriptor.dataset().datasetVersionId());
        identity.put("accessTableName", descriptor.dataset().accessTableName());
        identity.set("keyFields", json.valueToTree(descriptor.dataset().keyFields()));
        root.set("relations", json.valueToTree(descriptor.relations()));
        ObjectNode pkg = root.putObject("archive");
        pkg.put("fileName", archive.getFileName().toString());
        pkg.put("sha256", sha256(archive));
        pkg.put("bytes", Files.size(archive));
        pkg.put("entryCount", manifest.entries().size());
        pkg.put("datasetEntry", access.getFileName().toString());
        pkg.put("shippedManifest", PackageManifestDocument.ENTRY_NAME);
        root.set("preview", json.valueToTree(report.relations()));
        ArrayNode edges = root.putArray("edges");
        for (ArtifactMatcher.Plan plan : report.plans())
            for (ArtifactMatcher.PlannedEdge edge : plan.edges()) {
                ObjectNode node = edges.addObject();
                node.put("rowKey", edge.externalKey());
                node.put("relationCode", plan.relationCode());
                node.put("language", edge.language());
                node.put("ordinal", edge.ordinal());
                node.put("packagePath", edge.entry().originalPath());
                node.put("mediaType", edge.entry().mediaType());
                node.put("bytes", edge.entry().byteSize());
                node.put("sha256", edge.entry().sha256());
            }
        long errors = report.relations().stream().flatMap(r -> r.issues().stream()).filter(i -> i.severity() == ArtifactIssue.Severity.ERROR).count();
        root.put("result", errors == 0 ? "PASS" : "FAIL");
        Files.createDirectories(evidence.getParent());
        json.writeValue(evidence.toFile(), root);
    }

    private static String sha256(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            return sha256(in);
        }
    }

    private static String sha256(InputStream in) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[64 * 1024];
            for (int read; (read = in.read(buffer)) > 0; ) digest.update(buffer, 0, read);
            return HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private ArtifactPackageAssembler() {}
}
