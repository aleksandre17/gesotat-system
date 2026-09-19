package org.base.api.service.platform.statistical;

import org.base.api.service.platform.statistical.compat.CompatibilityClassifier;
import org.base.api.service.platform.statistical.workflow.ContractWorkflow;
import org.base.api.service.platform.statistical.workflow.ContractWorkflow.Actor;
import org.base.api.service.platform.statistical.workflow.ContractWorkflow.Authority;
import org.base.api.service.platform.statistical.workflow.ContractWorkflow.DraftRecord;
import org.base.api.service.platform.statistical.workflow.ContractWorkflow.Failure;
import org.base.api.service.platform.statistical.workflow.ContractWorkflow.State;
import org.base.api.service.platform.statistical.workflow.ContractWorkflow.WorkflowException;
import org.base.api.service.platform.statistical.workflow.JdbcContractStore;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.base.api.service.platform.statistical.StatisticalContractFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

/** One behavioural contract, two stores: the JDBC store must be substitutable for the reference store. */
abstract class ContractWorkflowTest {
    static final Actor AUTHOR = new Actor("author@labour"), APPROVER = new Actor("approver@labour"), OUTSIDER = new Actor("someone@land");

    abstract ContractWorkflow.Store newStore();

    private final Map<String, Set<Authority>> grants = new HashMap<>(Map.of(
            AUTHOR.subject() + "|" + LABOUR.productCode(), Set.of(Authority.AUTHOR),
            APPROVER.subject() + "|" + LABOUR.productCode(), Set.of(Authority.APPROVE),
            OUTSIDER.subject() + "|" + LAND.productCode(), Set.of(Authority.AUTHOR, Authority.APPROVE)));

    ContractWorkflow workflow(ContractWorkflow.Store store, boolean selfApproval) {
        return new ContractWorkflow(store, compiler(registry()),
                (actor, product, authority) -> grants.getOrDefault(actor.subject() + "|" + product, Set.of()).contains(authority),
                new ContractWorkflow.Policy(selfApproval), () -> UUID.randomUUID().toString());
    }

    private static Failure failure(Runnable action) { return assertThrows(WorkflowException.class, action::run).failure(); }

    @Test void happyPathBindsTheApprovalToTheReviewedDigest() {
        ContractWorkflow w = workflow(newStore(), false);
        DraftRecord draft = w.create(AUTHOR, LABOUR.productCode(), labourDraft(), "k1");
        assertEquals(State.DRAFT, draft.state());
        DraftRecord review = w.submit(AUTHOR, draft.draftId(), draft.version());
        assertEquals(State.REVIEW_REQUIRED, review.state());
        assertEquals("LABOUR:LABOUR_FORCE", review.datasetKey());
        ContractWorkflow.Approval approval = w.approve(APPROVER, draft.draftId(), review.version(), review.revisionDigest());
        assertEquals(State.APPROVED, approval.contract().state());
        assertEquals(APPROVER.subject(), approval.contract().decidedBy());
        assertEquals(review.revisionDigest(), approval.plan().revisionDigest());
        assertEquals(approval.contract(), w.approve(APPROVER, draft.draftId(), approval.contract().version(), review.revisionDigest()).contract(), "replayed decision is the same decision");
    }

    @Test void createIsIdempotentPerProductAndKey() {
        ContractWorkflow w = workflow(newStore(), false);
        DraftRecord first = w.create(AUTHOR, LABOUR.productCode(), labourDraft(), "same-key");
        assertEquals(first.draftId(), w.create(AUTHOR, LABOUR.productCode(), labourDraft(), "same-key").draftId(), "double click");
        assertEquals(Failure.IDEMPOTENCY_CONFLICT, failure(() -> w.create(AUTHOR, LABOUR.productCode(), labourDraft() + " ", "same-key")));
        assertEquals(Failure.PRECONDITION_REQUIRED, failure(() -> w.create(AUTHOR, LABOUR.productCode(), labourDraft(), " ")));
    }

    @Test void staleWritersLoseAndNothingIsOverwritten() {
        ContractWorkflow w = workflow(newStore(), false);
        DraftRecord draft = w.create(AUTHOR, LABOUR.productCode(), "{}", "k");
        DraftRecord saved = w.save(AUTHOR, draft.draftId(), labourDraft(), draft.version());
        assertEquals(Failure.STALE_VERSION, failure(() -> w.save(AUTHOR, draft.draftId(), "{\"other\":1}", draft.version())));
        assertEquals(Failure.PRECONDITION_REQUIRED, failure(() -> w.save(AUTHOR, draft.draftId(), "{}", null)));
        assertEquals(labourDraft(), w.get(AUTHOR, draft.draftId()).document());
        assertEquals(saved.version(), w.get(AUTHOR, draft.draftId()).version());
    }

    @Test void incompleteDraftSavesButCannotBeSubmitted() {
        ContractWorkflow w = workflow(newStore(), false);
        DraftRecord draft = w.create(AUTHOR, LABOUR.productCode(), "{\"profileRef\":\"profile:SHARED:STAT_AGGREGATE(1.0.0)\"}", "k");
        WorkflowException e = assertThrows(WorkflowException.class, () -> w.submit(AUTHOR, draft.draftId(), draft.version()));
        assertEquals(Failure.NOT_COMPILABLE, e.failure());
        assertFalse(e.issues().isEmpty(), "the author is told exactly what is missing");
        assertFalse(w.preview(AUTHOR, draft.draftId()).accepted());
    }

    @Test void editAfterReviewVoidsTheReviewedDigest() {
        ContractWorkflow w = workflow(newStore(), false);
        DraftRecord draft = w.create(AUTHOR, LABOUR.productCode(), labourDraft(), "k");
        DraftRecord review = w.submit(AUTHOR, draft.draftId(), draft.version());
        assertEquals(Failure.ILLEGAL_TRANSITION, failure(() -> w.save(AUTHOR, draft.draftId(), "{}", review.version())), "text is frozen during review");
        DraftRecord back = w.returnToDraft(APPROVER, draft.draftId(), review.version());
        assertNull(back.revisionDigest());
        DraftRecord edited = w.save(AUTHOR, draft.draftId(), labourDraft().replace("რეგიონი", "ტერიტორია"), back.version());
        DraftRecord second = w.submit(AUTHOR, draft.draftId(), edited.version());
        assertEquals(Failure.DIGEST_MISMATCH, failure(() -> w.approve(APPROVER, draft.draftId(), second.version(), review.revisionDigest())), "the old review does not cover the new text");
        assertEquals(State.APPROVED, w.approve(APPROVER, draft.draftId(), second.version(), second.revisionDigest()).contract().state());
    }

    @Test void approvalRequiresApprovedDependenciesAtExecutionTime() {
        grants.put(OUTSIDER.subject() + "|" + LAND.productCode(), Set.of(Authority.AUTHOR));
        grants.put("approver@land|" + LAND.productCode(), Set.of(Authority.APPROVE));
        ContractWorkflow w = workflow(newStore(), false);
        DraftRecord draft = w.create(OUTSIDER, LAND.productCode(), landDraft(true), "k");
        DraftRecord review = w.submit(OUTSIDER, draft.draftId(), draft.version());
        WorkflowException e = assertThrows(WorkflowException.class, () -> w.approve(new Actor("approver@land"), draft.draftId(), review.version(), review.revisionDigest()));
        assertEquals(Failure.NOT_COMPILABLE, e.failure(), "a proposed measure previews and reviews, but does not approve");
        assertEquals(State.REVIEW_REQUIRED, w.get(OUTSIDER, draft.draftId()).state());
    }

    @Test void authorizationIsPerCallAndAnotherProductsContractDoesNotExist() {
        ContractWorkflow w = workflow(newStore(), false);
        DraftRecord draft = w.create(AUTHOR, LABOUR.productCode(), labourDraft(), "k");
        assertEquals(Failure.FORBIDDEN, failure(() -> w.create(OUTSIDER, LABOUR.productCode(), labourDraft(), "k2")));
        assertEquals(Failure.NOT_FOUND, failure(() -> w.get(OUTSIDER, draft.draftId())), "no existence oracle across products");
        assertEquals(Failure.NOT_FOUND, failure(() -> w.get(AUTHOR, "no-such-id")));
        DraftRecord review = w.submit(AUTHOR, draft.draftId(), draft.version());
        assertEquals(Failure.FORBIDDEN, failure(() -> w.approve(AUTHOR, draft.draftId(), review.version(), review.revisionDigest())), "authoring is not approving");

        grants.put(AUTHOR.subject() + "|" + LABOUR.productCode(), Set.of(Authority.AUTHOR, Authority.APPROVE));
        assertEquals(Failure.SELF_APPROVAL, failure(() -> w.approve(AUTHOR, draft.draftId(), review.version(), review.revisionDigest())), "four eyes");
        grants.remove(APPROVER.subject() + "|" + LABOUR.productCode());
        assertEquals(Failure.NOT_FOUND, failure(() -> w.approve(APPROVER, draft.draftId(), review.version(), review.revisionDigest())), "a revoked permission is honoured at execution time");
    }

    @Test void singlePersonTeamIsAnExplicitPolicyNotADefault() {
        grants.put(AUTHOR.subject() + "|" + LABOUR.productCode(), Set.of(Authority.AUTHOR, Authority.APPROVE));
        ContractWorkflow w = workflow(newStore(), true);
        DraftRecord draft = w.create(AUTHOR, LABOUR.productCode(), labourDraft(), "k");
        DraftRecord review = w.submit(AUTHOR, draft.draftId(), draft.version());
        assertEquals(State.APPROVED, w.approve(AUTHOR, draft.draftId(), review.version(), review.revisionDigest()).contract().state());
    }

    @Test void newApprovedRevisionSupersedesThePreviousOneAndIsClassified() {
        ContractWorkflow.Store store = newStore();
        ContractWorkflow w = workflow(store, false);
        DraftRecord v1 = w.create(AUTHOR, LABOUR.productCode(), labourDraft(), "v1");
        DraftRecord r1 = w.submit(AUTHOR, v1.draftId(), v1.version());
        assertTrue(w.approve(APPROVER, v1.draftId(), r1.version(), r1.revisionDigest()).againstPrevious().isEmpty());

        DraftRecord v2 = w.create(AUTHOR, LABOUR.productCode(), labourDraft().replace("[\"YEAR\",\"QUARTER\"]", "[\"YEAR\"]"), "v2");
        DraftRecord r2 = w.submit(AUTHOR, v2.draftId(), v2.version());
        ContractWorkflow.Approval second = w.approve(APPROVER, v2.draftId(), r2.version(), r2.revisionDigest());
        assertEquals(CompatibilityClassifier.Level.BREAKING, second.againstPrevious().orElseThrow().level());
        assertEquals(State.SUPERSEDED, store.find(v1.draftId()).orElseThrow().state());
        assertEquals(v2.draftId(), store.findApproved(LABOUR.productCode(), "LABOUR:LABOUR_FORCE").orElseThrow().draftId(), "exactly one approved contract per dataset");
    }

    @Test void concurrentApprovalsProduceExactlyOneDecision() throws Exception {
        ContractWorkflow w = workflow(newStore(), false);
        DraftRecord draft = w.create(AUTHOR, LABOUR.productCode(), labourDraft(), "k");
        DraftRecord review = w.submit(AUTHOR, draft.draftId(), draft.version());
        for (int i = 0; i < 8; i++) grants.put("approver" + i + "|" + LABOUR.productCode(), Set.of(Authority.APPROVE));
        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> outcomes = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            Actor approver = new Actor("approver" + i);
            outcomes.add(pool.submit(() -> {
                start.await();
                try { w.approve(approver, draft.draftId(), review.version(), review.revisionDigest()); return true; }
                catch (WorkflowException e) { assertTrue(Set.of(Failure.STALE_VERSION, Failure.ILLEGAL_TRANSITION).contains(e.failure()), e.failure().name()); return false; }
            }));
        }
        start.countDown();
        int winners = 0;
        for (Future<Boolean> outcome : outcomes) if (outcome.get()) winners++;
        pool.shutdown();
        assertEquals(1, winners);
    }

    static final class InMemoryStore implements ContractWorkflow.Store {
        private final Map<String, DraftRecord> rows = new HashMap<>();

        public synchronized Optional<DraftRecord> find(String id) { return Optional.ofNullable(rows.get(id)); }
        public synchronized Optional<DraftRecord> findByIdempotencyKey(String product, String key) {
            return rows.values().stream().filter(r -> r.productCode().equals(product) && r.idempotencyKey().equals(key)).findFirst();
        }
        public synchronized Optional<DraftRecord> findApproved(String product, String dataset) {
            return rows.values().stream().filter(r -> r.state() == State.APPROVED && r.productCode().equals(product) && dataset.equals(r.datasetKey())).findFirst();
        }
        public synchronized boolean insert(DraftRecord draft) {
            if (findByIdempotencyKey(draft.productCode(), draft.idempotencyKey()).isPresent()) return false;
            rows.put(draft.draftId(), draft);
            return true;
        }
        public synchronized boolean update(DraftRecord next, long expected) {
            DraftRecord current = rows.get(next.draftId());
            if (current == null || current.version() != expected) return false;
            rows.put(next.draftId(), next);
            return true;
        }
        public synchronized boolean approve(DraftRecord approved, long expected, Optional<DraftRecord> superseded) {
            DraftRecord current = rows.get(approved.draftId());
            if (current == null || current.version() != expected) return false;
            if (superseded.isPresent() && rows.get(superseded.get().draftId()).version() != superseded.get().version() - 1) return false;
            superseded.ifPresent(s -> rows.put(s.draftId(), s));
            rows.put(approved.draftId(), approved);
            return true;
        }
    }
}

class ContractWorkflowReferenceStoreTest extends ContractWorkflowTest {
    @Override ContractWorkflow.Store newStore() { return new ContractWorkflowTest.InMemoryStore(); }
}

class ContractWorkflowJdbcStoreTest extends ContractWorkflowTest {
    @Override ContractWorkflow.Store newStore() {
        SingleConnectionDataSource dataSource = new SingleConnectionDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MSSQLServer", true);
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE SCHEMA platform");
        jdbc.execute("CREATE TABLE platform.data_product(product_id BIGINT IDENTITY PRIMARY KEY, product_code VARCHAR(120) UNIQUE)");
        jdbc.execute("""
                CREATE TABLE platform.statistical_contract_draft(draft_id VARCHAR(64) PRIMARY KEY, product_id BIGINT NOT NULL, dataset_key VARCHAR(256),
                  version BIGINT NOT NULL, state_code VARCHAR(32) NOT NULL, document CLOB NOT NULL, author VARCHAR(255) NOT NULL,
                  idempotency_key VARCHAR(128) NOT NULL, request_hash CHAR(64) NOT NULL, semantic_digest CHAR(64), revision_digest CHAR(64),
                  decided_by VARCHAR(255), updated_at TIMESTAMP, UNIQUE(product_id, idempotency_key))""");
        jdbc.execute("""
                CREATE TABLE platform.statistical_contract_draft_event(event_id BIGINT IDENTITY PRIMARY KEY, draft_id VARCHAR(64) NOT NULL, version BIGINT NOT NULL,
                  state_code VARCHAR(32) NOT NULL, actor VARCHAR(255) NOT NULL, revision_digest CHAR(64), UNIQUE(draft_id, version))""");
        jdbc.update("INSERT INTO platform.data_product(product_code) VALUES(?)", LABOUR.productCode());
        jdbc.update("INSERT INTO platform.data_product(product_code) VALUES(?)", LAND.productCode());
        JdbcContractStore store = new JdbcContractStore(jdbc, new TransactionTemplate(new DataSourceTransactionManager(dataSource)));
        // One shared connection: serialise callers the way the database would serialise row writers.
        return new ContractWorkflow.Store() {
            public synchronized Optional<DraftRecord> find(String id) { return store.find(id); }
            public synchronized Optional<DraftRecord> findByIdempotencyKey(String p, String k) { return store.findByIdempotencyKey(p, k); }
            public synchronized Optional<DraftRecord> findApproved(String p, String d) { return store.findApproved(p, d); }
            public synchronized boolean insert(DraftRecord d) { return store.insert(d); }
            public synchronized boolean update(DraftRecord n, long v) { return store.update(n, v); }
            public synchronized boolean approve(DraftRecord a, long v, Optional<DraftRecord> s) { return store.approve(a, v, s); }
        };
    }

    @Test void everyStateChangeLeavesOneHistoryEvent() {
        ContractWorkflow.Store store = newStore();
        ContractWorkflow w = workflow(store, false);
        DraftRecord draft = w.create(AUTHOR, LABOUR.productCode(), labourDraft(), "k");
        DraftRecord review = w.submit(AUTHOR, draft.draftId(), draft.version());
        w.approve(APPROVER, draft.draftId(), review.version(), review.revisionDigest());
        assertEquals(3, store.find(draft.draftId()).orElseThrow().version());
    }
}

