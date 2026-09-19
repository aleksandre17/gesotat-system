package org.base.api.service.platform.statistical.workflow;

import org.base.api.service.platform.statistical.canonical.CanonicalJson;
import org.base.api.service.platform.statistical.compat.CompatibilityClassifier;
import org.base.api.service.platform.statistical.compiler.ContractIssue;
import org.base.api.service.platform.statistical.compiler.StatisticalContractCompiler;
import org.base.api.service.platform.statistical.compiler.StatisticalContractCompiler.Mode;
import org.base.api.service.platform.statistical.plan.SemanticPlan;
import org.base.api.service.platform.statistical.registry.StatisticalRegistry.Scope;

import java.util.List;
import java.util.Optional;

/**
 * Lifecycle of one statistical contract: DRAFT -> REVIEW_REQUIRED -> APPROVED -> SUPERSEDED, with a return to
 * DRAFT from review. States are the existing contract-revision lifecycle values; no new vocabulary is persisted.
 *
 * Invariants (register Q06, Q40, Q42, Q44):
 * - every mutation is a compare-and-set on the draft version; a stale writer loses, nothing is overwritten;
 * - create is idempotent per (product, key): same payload returns the first result, another payload conflicts;
 * - an approval is bound to the exact revision digest that was reviewed and is recompiled at execution time;
 * - authorization is decided per call, at execution time; a draft of another product is simply not found;
 * - the approver is not the author unless the deployment policy explicitly allows a single-person team.
 */
public final class ContractWorkflow {

    public enum State { DRAFT, REVIEW_REQUIRED, APPROVED, SUPERSEDED, ROLLED_BACK }

    /** Separate duties (register Q06): describing a contract, deciding on it, loading data under it. */
    public enum Authority { AUTHOR, APPROVE, IMPORT }

    public record Actor(String subject) {
        public Actor { if (subject == null || subject.isBlank()) throw new IllegalArgumentException("actor is required"); }
    }

    /** Port to the platform's authorization (tenant scope and function-level authority); deny by default. */
    public interface AccessDecision { boolean allowed(Actor actor, String productCode, Authority authority); }

    public record Policy(boolean allowSelfApproval) { }

    public record DraftRecord(String draftId, String productCode, String datasetKey, long version, State state, String document,
                              String author, String idempotencyKey, String requestHash,
                              String semanticDigest, String revisionDigest, String decidedBy) { }

    /** Persistence port. Both mutating methods are atomic and succeed only when the stored version matches. */
    public interface Store {
        Optional<DraftRecord> find(String draftId);
        Optional<DraftRecord> findByIdempotencyKey(String productCode, String idempotencyKey);
        Optional<DraftRecord> findApproved(String productCode, String datasetKey);
        /** @return false when (product, idempotency key) already exists */
        boolean insert(DraftRecord draft);
        boolean update(DraftRecord next, long expectedVersion);
        boolean approve(DraftRecord approved, long expectedVersion, Optional<DraftRecord> superseded);
    }

    public enum Failure {
        NOT_FOUND, FORBIDDEN, PRECONDITION_REQUIRED, STALE_VERSION, ILLEGAL_TRANSITION, IDEMPOTENCY_CONFLICT,
        NOT_COMPILABLE, DIGEST_MISMATCH, SELF_APPROVAL
    }

    public static final class WorkflowException extends RuntimeException {
        private final Failure failure;
        private final transient List<ContractIssue> issues;

        WorkflowException(Failure failure, String message, List<ContractIssue> issues) {
            super(message);
            this.failure = failure;
            this.issues = List.copyOf(issues);
        }
        public Failure failure() { return failure; }
        public List<ContractIssue> issues() { return issues; }
    }

    public record Approval(DraftRecord contract, SemanticPlan plan, Optional<CompatibilityClassifier.Verdict> againstPrevious) { }

    private final Store store;
    private final StatisticalContractCompiler compiler;
    private final AccessDecision access;
    private final Policy policy;
    private final java.util.function.Supplier<String> ids;

    public ContractWorkflow(Store store, StatisticalContractCompiler compiler, AccessDecision access, Policy policy,
                            java.util.function.Supplier<String> ids) {
        this.store = store;
        this.compiler = compiler;
        this.access = access;
        this.policy = policy;
        this.ids = ids;
    }

    public DraftRecord create(Actor actor, String productCode, String document, String idempotencyKey) {
        authorize(actor, productCode, Authority.AUTHOR);
        if (idempotencyKey == null || idempotencyKey.isBlank()) throw fail(Failure.PRECONDITION_REQUIRED, "an idempotency key is required to create a draft");
        String hash = CanonicalJson.digest("geostat.stat-contract.create.v1", List.of(productCode, document));
        DraftRecord draft = new DraftRecord(ids.get(), productCode, null, 1, State.DRAFT, document, actor.subject(), idempotencyKey, hash, null, null, null);
        if (store.insert(draft)) return draft;
        DraftRecord first = store.findByIdempotencyKey(productCode, idempotencyKey).orElseThrow(() -> fail(Failure.IDEMPOTENCY_CONFLICT, "idempotency key is in use"));
        if (!first.requestHash().equals(hash)) throw fail(Failure.IDEMPOTENCY_CONFLICT, "the same idempotency key was used with a different request");
        return first;
    }

    /** Saving does not require a complete contract; only submission does. */
    public DraftRecord save(Actor actor, String draftId, String document, Long expectedVersion) {
        DraftRecord current = load(actor, draftId, Authority.AUTHOR, expectedVersion);
        require(current, State.DRAFT);
        return swap(current, new DraftRecord(current.draftId(), current.productCode(), current.datasetKey(), current.version() + 1, State.DRAFT, document,
                current.author(), current.idempotencyKey(), current.requestHash(), null, null, null));
    }

    public DraftRecord submit(Actor actor, String draftId, Long expectedVersion) {
        DraftRecord current = load(actor, draftId, Authority.AUTHOR, expectedVersion);
        require(current, State.DRAFT);
        SemanticPlan plan = compile(current, Mode.DRAFT);
        return swap(current, new DraftRecord(current.draftId(), current.productCode(), plan.datasetNamespace() + ":" + plan.datasetCode(), current.version() + 1,
                State.REVIEW_REQUIRED, current.document(), current.author(), current.idempotencyKey(), current.requestHash(),
                plan.semanticDigest(), plan.revisionDigest(), null));
    }

    public DraftRecord returnToDraft(Actor actor, String draftId, Long expectedVersion) {
        DraftRecord current = load(actor, draftId, Authority.APPROVE, expectedVersion);
        require(current, State.REVIEW_REQUIRED);
        return swap(current, new DraftRecord(current.draftId(), current.productCode(), current.datasetKey(), current.version() + 1, State.DRAFT, current.document(),
                current.author(), current.idempotencyKey(), current.requestHash(), null, null, null));
    }

    /** @param reviewedRevisionDigest the digest the approver actually looked at */
    public Approval approve(Actor actor, String draftId, Long expectedVersion, String reviewedRevisionDigest) {
        DraftRecord current = load(actor, draftId, Authority.APPROVE, expectedVersion);
        if (current.state() == State.APPROVED && current.revisionDigest().equals(reviewedRevisionDigest) && actor.subject().equals(current.decidedBy()))
            return new Approval(current, compile(current, Mode.APPROVAL), Optional.empty()); // replay of the same decision
        require(current, State.REVIEW_REQUIRED);
        if (!policy.allowSelfApproval() && actor.subject().equals(current.author())) throw fail(Failure.SELF_APPROVAL, "the author cannot approve their own revision");
        SemanticPlan plan = compile(current, Mode.APPROVAL);
        if (!plan.revisionDigest().equals(current.revisionDigest()) || !plan.revisionDigest().equals(reviewedRevisionDigest))
            throw fail(Failure.DIGEST_MISMATCH, "the reviewed revision is not the revision that would be approved");

        Optional<DraftRecord> previous = store.findApproved(current.productCode(), current.datasetKey());
        Optional<CompatibilityClassifier.Verdict> verdict = previous.map(p -> CompatibilityClassifier.classify(compile(p, Mode.DRAFT), plan));
        DraftRecord approved = new DraftRecord(current.draftId(), current.productCode(), current.datasetKey(), current.version() + 1, State.APPROVED, current.document(),
                current.author(), current.idempotencyKey(), current.requestHash(), plan.semanticDigest(), plan.revisionDigest(), actor.subject());
        Optional<DraftRecord> superseded = previous.map(p -> new DraftRecord(p.draftId(), p.productCode(), p.datasetKey(), p.version() + 1, State.SUPERSEDED, p.document(),
                p.author(), p.idempotencyKey(), p.requestHash(), p.semanticDigest(), p.revisionDigest(), p.decidedBy()));
        if (!store.approve(approved, current.version(), superseded)) throw fail(Failure.STALE_VERSION, "the contract changed while it was being approved");
        return new Approval(approved, plan, verdict);
    }

    /** Reading is open to every role of the product: an approver must see what they approve. */
    public DraftRecord get(Actor actor, String draftId) { return load(actor, draftId, null, null, false); }

    /**
     * The plan of an approved contract, recompiled now. Generation and ingestion read the contract through this
     * method only, so a registry that drifted since approval is detected instead of silently reinterpreted.
     */
    public SemanticPlan approvedPlan(Actor actor, String draftId) {
        DraftRecord current = get(actor, draftId);
        require(current, State.APPROVED);
        SemanticPlan plan = compile(current, Mode.APPROVAL);
        if (!plan.revisionDigest().equals(current.revisionDigest())) throw fail(Failure.DIGEST_MISMATCH, "the approved contract no longer compiles to the approved revision");
        return plan;
    }

    /** Read-only preview for the authoring UI: what the compiler says about the current text. */
    public StatisticalContractCompiler.Result preview(Actor actor, String draftId) {
        DraftRecord current = get(actor, draftId);
        return compiler.compile(current.document(), new Scope(current.productCode()), Mode.DRAFT);
    }

    private DraftRecord load(Actor actor, String draftId, Authority authority, Long expectedVersion) { return load(actor, draftId, authority, expectedVersion, true); }

    private DraftRecord load(Actor actor, String draftId, Authority authority, Long expectedVersion, boolean mutation) {
        DraftRecord current = store.find(draftId).orElseThrow(() -> fail(Failure.NOT_FOUND, "contract not found"));
        if (java.util.Arrays.stream(Authority.values()).noneMatch(a -> access.allowed(actor, current.productCode(), a)))
            throw fail(Failure.NOT_FOUND, "contract not found"); // no existence oracle across products
        if (authority != null && !access.allowed(actor, current.productCode(), authority)) throw fail(Failure.FORBIDDEN, "missing authority: " + authority);
        if (mutation && expectedVersion == null) throw fail(Failure.PRECONDITION_REQUIRED, "the expected version is required");
        if (mutation && expectedVersion != current.version()) throw fail(Failure.STALE_VERSION, "the contract was changed by someone else");
        return current;
    }

    private void authorize(Actor actor, String productCode, Authority authority) {
        if (!access.allowed(actor, productCode, authority)) throw fail(Failure.FORBIDDEN, "missing authority: " + authority);
    }

    private static void require(DraftRecord current, State state) {
        if (current.state() != state) throw fail(Failure.ILLEGAL_TRANSITION, "not allowed from " + current.state());
    }

    private DraftRecord swap(DraftRecord current, DraftRecord next) {
        if (!store.update(next, current.version())) throw fail(Failure.STALE_VERSION, "the contract was changed by someone else");
        return next;
    }

    private SemanticPlan compile(DraftRecord draft, Mode mode) {
        StatisticalContractCompiler.Result result = compiler.compile(draft.document(), new Scope(draft.productCode()), mode);
        return result.plan().orElseThrow(() -> new WorkflowException(Failure.NOT_COMPILABLE, "the contract does not compile", result.issues()));
    }

    private static WorkflowException fail(Failure failure, String message) { return new WorkflowException(failure, message, List.of()); }
}
