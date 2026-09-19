package org.base.api.service.platform.statistical.access;

import org.base.api.service.platform.statistical.access.AccessAuthoringAdapter.CodeItem;
import org.base.api.service.platform.statistical.ingest.WideRowNormalizer;
import org.base.api.service.platform.statistical.model.Ref;
import org.base.api.service.platform.statistical.model.Representation;
import org.base.api.service.platform.statistical.plan.SemanticPlan;
import org.base.api.service.platform.statistical.registry.StatisticalRegistry;
import org.base.api.service.platform.statistical.workflow.ContractWorkflow;
import org.base.api.service.platform.statistical.workflow.ContractWorkflow.Actor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/**
 * Delivery and read-only validation of the authoring file of an APPROVED contract.
 *
 * Generation is a pure function of the approved plan and the pinned codelists, so the file is never stored as a
 * source of truth: it is produced on demand, streamed and discarded. Validation parses an uploaded file against
 * the same plan and reports every finding; it writes nothing (lifecycle §3, steps 11 and 13).
 * Temporary files live in a directory owned by the single call and are removed in every outcome.
 */
public final class AuthoringFileService {

    /** Labels of the codes of a pinned codelist, in the requested language; the adapter shows them, stores codes. */
    public interface CodelistLabels { List<CodeItem> items(Ref codelistRef, String languageTag); }

    public record GeneratedFile(String fileName, byte[] content, String sha256, String revisionDigest) { }

    public record ValidationReport(boolean accepted, String revisionDigest, int rows, int observations,
                                   List<String> fileIssues, List<WideRowNormalizer.RowIssue> rowIssues, List<Long> quarantinedRows) { }

    public static final class FileTooLarge extends RuntimeException {
        public FileTooLarge(long limit) { super("the file exceeds the configured limit of " + limit + " bytes"); }
    }

    private final ContractWorkflow workflow;
    private final StatisticalRegistry registry;
    private final CodelistLabels labels;
    private final AccessAuthoringAdapter adapter = new AccessAuthoringAdapter();
    private final String statusConceptCode;
    private final long maxUploadBytes;

    public AuthoringFileService(ContractWorkflow workflow, StatisticalRegistry registry, CodelistLabels labels, String statusConceptCode, long maxUploadBytes) {
        this.workflow = workflow;
        this.registry = registry;
        this.labels = labels;
        this.statusConceptCode = statusConceptCode;
        this.maxUploadBytes = maxUploadBytes;
    }

    public GeneratedFile generate(Actor actor, String contractId, String languageTag) throws IOException {
        SemanticPlan plan = workflow.approvedPlan(actor, contractId);
        Map<Ref, List<CodeItem>> codelists = new HashMap<>();
        plan.components().forEach(c -> {
            if (c.representation() instanceof Representation.Coded coded) codelists.computeIfAbsent(coded.codelistRef(), ref -> labels.items(ref, languageTag));
        });
        Path directory = Files.createTempDirectory("stat-authoring-");
        try {
            Path file = directory.resolve("authoring.accdb");
            adapter.emit(plan, codelists, languageTag, file.toFile());
            byte[] content = Files.readAllBytes(file);
            String name = plan.datasetNamespace() + "_" + plan.datasetCode() + "_" + plan.revisionDigest().substring(0, 12) + ".accdb";
            return new GeneratedFile(name, content, sha256(content), plan.revisionDigest());
        } finally {
            deleteQuietly(directory);
        }
    }

    public ValidationReport validate(Actor actor, String contractId, InputStream upload) throws IOException {
        SemanticPlan plan = workflow.approvedPlan(actor, contractId);
        String product = workflow.get(actor, contractId).productCode();
        Path directory = Files.createTempDirectory("stat-validation-");
        try {
            Path file = directory.resolve("upload.accdb");
            try (InputStream bounded = new BoundedInputStream(upload, maxUploadBytes)) {
                Files.copy(bounded, file, StandardCopyOption.REPLACE_EXISTING);
            }
            AccessAuthoringAdapter.ReadResult read;
            try {
                read = adapter.read(file.toFile(), plan);
            } catch (IOException | RuntimeException notAnAccessFile) {
                return new ValidationReport(false, plan.revisionDigest(), 0, 0, List.of("NOT_A_READABLE_AUTHORING_FILE"), List.of(), List.of());
            }
            if (!read.accepted()) return new ValidationReport(false, plan.revisionDigest(), 0, 0, read.issues(), List.of(), List.of());
            StatisticalRegistry.Scope scope = new StatisticalRegistry.Scope(product);
            WideRowNormalizer.Result result = new WideRowNormalizer(plan,
                    (ref, code) -> registry.codelist(ref, scope).map(list -> list.codes().contains(code)).orElse(false), statusConceptCode).normalize(read.rows());
            return new ValidationReport(result.accepted() && result.issues().isEmpty(), plan.revisionDigest(), read.rows().size(), result.observations().size(),
                    List.of(), result.issues(), result.quarantinedRows().stream().sorted().toList());
        } finally {
            deleteQuietly(directory);
        }
    }

    private static String sha256(byte[] content) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content)); }
        catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    private static void deleteQuietly(Path directory) {
        try (var paths = Files.walk(directory)) {
            paths.sorted(java.util.Comparator.reverseOrder()).forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) { } });
        } catch (IOException ignored) { }
    }

    /** Stops reading at the byte budget instead of trusting a declared length. */
    private static final class BoundedInputStream extends java.io.FilterInputStream {
        private final long limit;
        private long seen;
        BoundedInputStream(InputStream in, long limit) { super(in); this.limit = limit; }
        @Override public int read() throws IOException { int b = super.read(); if (b >= 0) count(1); return b; }
        @Override public int read(byte[] buffer, int offset, int length) throws IOException { int n = super.read(buffer, offset, length); if (n > 0) count(n); return n; }
        private void count(int n) { seen += n; if (seen > limit) throw new FileTooLarge(limit); }
    }
}
