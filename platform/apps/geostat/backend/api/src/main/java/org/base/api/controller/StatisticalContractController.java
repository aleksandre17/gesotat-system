package org.base.api.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.base.api.security.tenancy.TenantScoped;
import org.base.api.service.platform.statistical.access.AuthoringFileService;
import org.base.api.service.platform.statistical.compiler.ContractIssue;
import org.base.api.service.platform.statistical.ingest.CanonicalObservationWriter;
import org.base.api.service.platform.statistical.ingest.StatisticalLoadService;
import org.base.api.service.platform.statistical.compiler.StatisticalContractCompiler;
import org.base.api.service.platform.statistical.workflow.ContractWorkflow;
import org.base.api.service.platform.statistical.workflow.ContractWorkflow.Actor;
import org.base.api.service.platform.statistical.workflow.ContractWorkflow.DraftRecord;
import org.base.api.service.platform.statistical.workflow.ContractWorkflow.WorkflowException;
import org.base.core.anotation.Api;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Authoring surface of the common statistical contract. The route carries {@code productCode}, so the tenant
 * interceptor decides the product boundary before any handler runs; function-level authority is decided per
 * call inside the workflow. Concurrency is RFC 9110: a strong ETag is the contract version, every mutation
 * needs If-Match (428 when absent, 412 when stale). Create needs an Idempotency-Key. Errors are RFC 9457.
 */
@TenantScoped
@Api
@RestController
@RequestMapping("/platform/products/{productCode}/statistical-contracts")
public class StatisticalContractController {
    private final ContractWorkflow workflow;
    private final AuthoringFileService files;
    private final StatisticalLoadService loads;

    public StatisticalContractController(ContractWorkflow workflow, AuthoringFileService files, StatisticalLoadService loads) {
        this.workflow = workflow;
        this.files = files;
        this.loads = loads;
    }

    public record ContractView(String id, String productCode, String datasetKey, long version, String state, String author,
                               String semanticDigest, String revisionDigest, String decidedBy, String document) { }

    public record PreviewView(boolean accepted, List<ContractIssue> issues, Object plan) { }

    public record ApproveRequest(String revisionDigest) { }

    public record ApprovalView(ContractView contract, String compatibility, List<String> reasons) { }

    @PostMapping
    public ResponseEntity<ContractView> create(@PathVariable String productCode, @RequestBody String document,
                                               @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                               Authentication authentication) {
        DraftRecord draft = workflow.create(actor(authentication), productCode, document, idempotencyKey);
        return respond(ResponseEntity.created(URI.create(draft.draftId())), draft);
    }

    @GetMapping("/{contractId}")
    public ResponseEntity<ContractView> read(@PathVariable String productCode, @PathVariable String contractId, Authentication authentication) {
        return respond(ResponseEntity.ok(), owned(productCode, workflow.get(actor(authentication), contractId)));
    }

    @PutMapping("/{contractId}")
    public ResponseEntity<ContractView> save(@PathVariable String productCode, @PathVariable String contractId, @RequestBody String document,
                                             @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch, Authentication authentication) {
        owned(productCode, workflow.get(actor(authentication), contractId));
        return respond(ResponseEntity.ok(), workflow.save(actor(authentication), contractId, document, version(ifMatch)));
    }

    @GetMapping("/{contractId}/preview")
    public ResponseEntity<PreviewView> preview(@PathVariable String productCode, @PathVariable String contractId, Authentication authentication) {
        owned(productCode, workflow.get(actor(authentication), contractId));
        StatisticalContractCompiler.Result result = workflow.preview(actor(authentication), contractId);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new PreviewView(result.accepted(), result.issues(), result.plan().orElse(null)));
    }

    @PostMapping("/{contractId}/submit")
    public ResponseEntity<ContractView> submit(@PathVariable String productCode, @PathVariable String contractId,
                                               @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch, Authentication authentication) {
        owned(productCode, workflow.get(actor(authentication), contractId));
        return respond(ResponseEntity.ok(), workflow.submit(actor(authentication), contractId, version(ifMatch)));
    }

    @PostMapping("/{contractId}/return")
    public ResponseEntity<ContractView> returnToDraft(@PathVariable String productCode, @PathVariable String contractId,
                                                      @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch, Authentication authentication) {
        owned(productCode, workflow.get(actor(authentication), contractId));
        return respond(ResponseEntity.ok(), workflow.returnToDraft(actor(authentication), contractId, version(ifMatch)));
    }

    @PostMapping("/{contractId}/approve")
    public ResponseEntity<ApprovalView> approve(@PathVariable String productCode, @PathVariable String contractId, @RequestBody ApproveRequest request,
                                                @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch, Authentication authentication) {
        owned(productCode, workflow.get(actor(authentication), contractId));
        ContractWorkflow.Approval approval = workflow.approve(actor(authentication), contractId, version(ifMatch), request == null ? null : request.revisionDigest());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).eTag(etag(approval.contract()))
                .body(new ApprovalView(view(approval.contract()),
                        approval.againstPrevious().map(v -> v.compatibilityMode()).orElse(null),
                        approval.againstPrevious().map(v -> v.reasons()).orElse(List.of())));
    }

    /** The authoring file of the approved revision; produced on demand, never cached, integrity in the headers. */
    @GetMapping("/{contractId}/authoring-file")
    public ResponseEntity<byte[]> authoringFile(@PathVariable String productCode, @PathVariable String contractId,
                                                @RequestParam(defaultValue = "ka") String language, Authentication authentication) throws java.io.IOException {
        owned(productCode, workflow.get(actor(authentication), contractId));
        if (!language.matches("[A-Za-z]{2,8}(-[A-Za-z0-9]{1,8})*")) throw new IllegalArgumentException("language must be a BCP 47 tag");
        AuthoringFileService.GeneratedFile file = files.generate(actor(authentication), contractId, language);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_TYPE, "application/msaccess")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.fileName() + "\"")
                .header("X-Content-Type-Options", "nosniff")
                .header("Digest", "sha-256=" + java.util.Base64.getEncoder().encodeToString(java.util.HexFormat.of().parseHex(file.sha256())))
                .header("X-Contract-Revision-Digest", file.revisionDigest())
                .body(file.content());
    }

    /** Read-only check of a filled file against the approved revision; nothing is stored or loaded. */
    @PostMapping("/{contractId}/authoring-file/validation")
    public ResponseEntity<AuthoringFileService.ValidationReport> validate(@PathVariable String productCode, @PathVariable String contractId,
                                                                        @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
                                                                        Authentication authentication) throws java.io.IOException {
        owned(productCode, workflow.get(actor(authentication), contractId));
        try (java.io.InputStream in = file.getInputStream()) {
            return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(files.validate(actor(authentication), contractId, in));
        }
    }

    /**
     * Loads a filled file into a candidate (REVIEW_REQUIRED) snapshot. Never publishes. Safe to repeat: the same file for the same
     * approved revision returns the first receipt. A rejected file answers 422 with every finding and writes nothing.
     */
    @PostMapping("/{contractId}/loads")
    public ResponseEntity<StatisticalLoadService.Receipt> load(@PathVariable String productCode, @PathVariable String contractId,
                                                               @RequestParam(defaultValue = StatisticalLoadService.MODE_FULL_SNAPSHOT) String mode,
                                                               @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
                                                               Authentication authentication) throws java.io.IOException {
        owned(productCode, workflow.get(actor(authentication), contractId));
        try (java.io.InputStream in = file.getInputStream()) {
            StatisticalLoadService.Receipt receipt = loads.load(actor(authentication), contractId, mode, file.getOriginalFilename(), in);
            HttpStatus status = switch (receipt.outcome()) { case LOADED -> HttpStatus.CREATED; case ALREADY_LOADED -> HttpStatus.OK; case REJECTED -> HttpStatus.UNPROCESSABLE_ENTITY; };
            return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(receipt);
        }
    }

    @ExceptionHandler(StatisticalLoadService.UnsupportedMode.class)
    public ResponseEntity<ProblemDetail> unsupportedMode(StatisticalLoadService.UnsupportedMode ex, HttpServletRequest request) {
        return ApiProblems.problem(HttpStatus.UNPROCESSABLE_ENTITY, "statistical-contract-unsupported-load-mode", ex.getMessage(), request);
    }

    @ExceptionHandler(StatisticalLoadService.ImportForbidden.class)
    public ResponseEntity<ProblemDetail> importForbidden(StatisticalLoadService.ImportForbidden ex, HttpServletRequest request) {
        return ApiProblems.problem(HttpStatus.FORBIDDEN, "statistical-contract-forbidden", "missing authority: IMPORT", request);
    }

    @ExceptionHandler(CanonicalObservationWriter.WriteConflict.class)
    public ResponseEntity<ProblemDetail> writeConflict(CanonicalObservationWriter.WriteConflict ex, HttpServletRequest request) {
        return ApiProblems.problem(HttpStatus.CONFLICT, "statistical-contract-load-conflict", ex.getMessage(), request);
    }

    @ExceptionHandler(AuthoringFileService.FileTooLarge.class)
    public ResponseEntity<ProblemDetail> tooLarge(AuthoringFileService.FileTooLarge ex, HttpServletRequest request) {
        return ApiProblems.problem(HttpStatus.PAYLOAD_TOO_LARGE, "statistical-contract-file-too-large", ex.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> invalid(IllegalArgumentException ex, HttpServletRequest request) {
        return ApiProblems.problem(HttpStatus.BAD_REQUEST, "statistical-contract-invalid-request", ex.getMessage(), request);
    }

    /** The path product is the tenant-checked one; a contract of another product must not be reachable through it. */
    private static DraftRecord owned(String productCode, DraftRecord draft) {
        if (!draft.productCode().equals(productCode)) throw new WorkflowNotFound();
        return draft;
    }

    private static final class WorkflowNotFound extends RuntimeException { }

    private static Actor actor(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) throw new WorkflowNotFound();
        return new Actor(authentication.getName());
    }

    /** Strong validator only: {@code "<version>"}. Weak validators and {@code *} do not identify a revision. */
    static Long version(String ifMatch) {
        if (ifMatch == null || ifMatch.isBlank()) return null;
        String value = ifMatch.trim();
        if (!value.matches("\"\\d{1,18}\"")) return -1L; // never equals a stored version: answered as 412
        return Long.parseLong(value.substring(1, value.length() - 1));
    }

    private static String etag(DraftRecord draft) { return "\"" + draft.version() + "\""; }

    private static ContractView view(DraftRecord d) {
        return new ContractView(d.draftId(), d.productCode(), d.datasetKey(), d.version(), d.state().name(), d.author(),
                d.semanticDigest(), d.revisionDigest(), d.decidedBy(), d.document());
    }

    private static ResponseEntity<ContractView> respond(ResponseEntity.BodyBuilder builder, DraftRecord draft) {
        return builder.cacheControl(CacheControl.noStore()).eTag(etag(draft)).body(view(draft));
    }

    @ExceptionHandler(WorkflowNotFound.class)
    public ResponseEntity<ProblemDetail> notFound(WorkflowNotFound ex, HttpServletRequest request) {
        return ApiProblems.problem(HttpStatus.NOT_FOUND, "statistical-contract-not-found", "contract not found", request);
    }

    @ExceptionHandler(WorkflowException.class)
    public ResponseEntity<ProblemDetail> workflow(WorkflowException ex, HttpServletRequest request) {
        HttpStatus status = STATUS.get(ex.failure());
        ResponseEntity<ProblemDetail> response = ApiProblems.problem(status, "statistical-contract-" + ex.failure().name().toLowerCase().replace('_', '-'), ex.getMessage(), request);
        if (!ex.issues().isEmpty() && response.getBody() != null) response.getBody().setProperty("issues", ex.issues());
        return response;
    }

    private static final Map<ContractWorkflow.Failure, HttpStatus> STATUS = Map.of(
            ContractWorkflow.Failure.NOT_FOUND, HttpStatus.NOT_FOUND,
            ContractWorkflow.Failure.FORBIDDEN, HttpStatus.FORBIDDEN,
            ContractWorkflow.Failure.PRECONDITION_REQUIRED, HttpStatus.PRECONDITION_REQUIRED,
            ContractWorkflow.Failure.STALE_VERSION, HttpStatus.PRECONDITION_FAILED,
            ContractWorkflow.Failure.ILLEGAL_TRANSITION, HttpStatus.CONFLICT,
            ContractWorkflow.Failure.IDEMPOTENCY_CONFLICT, HttpStatus.CONFLICT,
            ContractWorkflow.Failure.NOT_COMPILABLE, HttpStatus.UNPROCESSABLE_ENTITY,
            ContractWorkflow.Failure.DIGEST_MISMATCH, HttpStatus.CONFLICT,
            ContractWorkflow.Failure.SELF_APPROVAL, HttpStatus.FORBIDDEN);
}
