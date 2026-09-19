package org.base.api.service.platform.statistical.workflow;

import org.base.api.service.platform.statistical.workflow.ContractWorkflow.DraftRecord;
import org.base.api.service.platform.statistical.workflow.ContractWorkflow.State;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;

/**
 * Control-plane store of statistical contracts. Every write is a compare-and-set on {@code version} and
 * appends one history event in the same transaction; approval and supersession commit together or not at all.
 * Fixed parameterised SQL only.
 */
public final class JdbcContractStore implements ContractWorkflow.Store {
    private static final String SELECT = """
            SELECT d.draft_id, p.product_code, d.dataset_key, d.version, d.state_code, d.document, d.author,
                   d.idempotency_key, d.request_hash, d.semantic_digest, d.revision_digest, d.decided_by
            FROM platform.statistical_contract_draft d JOIN platform.data_product p ON p.product_id = d.product_id
            """;
    private static final RowMapper<DraftRecord> MAPPER = (rs, i) -> new DraftRecord(rs.getString(1), rs.getString(2), rs.getString(3), rs.getLong(4),
            State.valueOf(rs.getString(5)), rs.getString(6), rs.getString(7), rs.getString(8), rs.getString(9), rs.getString(10), rs.getString(11), rs.getString(12));

    private final JdbcTemplate control;
    private final TransactionTemplate transaction;

    public JdbcContractStore(JdbcTemplate control, TransactionTemplate transaction) {
        this.control = control;
        this.transaction = transaction;
    }

    @Override public Optional<DraftRecord> find(String draftId) { return one(control.query(SELECT + " WHERE d.draft_id = ?", MAPPER, draftId)); }

    @Override public Optional<DraftRecord> findByIdempotencyKey(String productCode, String idempotencyKey) {
        return one(control.query(SELECT + " WHERE p.product_code = ? AND d.idempotency_key = ?", MAPPER, productCode, idempotencyKey));
    }

    @Override public Optional<DraftRecord> findApproved(String productCode, String datasetKey) {
        return one(control.query(SELECT + " WHERE p.product_code = ? AND d.dataset_key = ? AND d.state_code = 'APPROVED'", MAPPER, productCode, datasetKey));
    }

    @Override public boolean insert(DraftRecord draft) {
        try {
            return Boolean.TRUE.equals(transaction.execute(status -> {
                int rows = control.update("""
                        INSERT INTO platform.statistical_contract_draft(draft_id, product_id, dataset_key, version, state_code, document, author, idempotency_key, request_hash)
                        SELECT ?, p.product_id, ?, ?, ?, ?, ?, ?, ? FROM platform.data_product p WHERE p.product_code = ?""",
                        draft.draftId(), draft.datasetKey(), draft.version(), draft.state().name(), draft.document(), draft.author(),
                        draft.idempotencyKey(), draft.requestHash(), draft.productCode());
                if (rows != 1) throw new IllegalStateException("unknown product: the draft was not stored");
                event(draft, draft.author());
                return true;
            }));
        } catch (DuplicateKeyException sameIdempotencyKey) {
            return false;
        }
    }

    @Override public boolean update(DraftRecord next, long expectedVersion) {
        return Boolean.TRUE.equals(transaction.execute(status -> {
            if (!compareAndSet(next, expectedVersion)) return false;
            event(next, next.decidedBy() != null ? next.decidedBy() : next.author());
            return true;
        }));
    }

    @Override public boolean approve(DraftRecord approved, long expectedVersion, Optional<DraftRecord> superseded) {
        return Boolean.TRUE.equals(transaction.execute(status -> {
            // Supersede first: the filtered unique index admits one APPROVED row per dataset at any instant.
            if (superseded.isPresent() && !compareAndSet(superseded.get(), superseded.get().version() - 1)) { status.setRollbackOnly(); return false; }
            if (!compareAndSet(approved, expectedVersion)) { status.setRollbackOnly(); return false; }
            if (superseded.isPresent()) event(superseded.get(), approved.decidedBy());
            event(approved, approved.decidedBy());
            return true;
        }));
    }

    private boolean compareAndSet(DraftRecord next, long expectedVersion) {
        return control.update("""
                UPDATE platform.statistical_contract_draft
                SET dataset_key = ?, version = ?, state_code = ?, document = ?, semantic_digest = ?, revision_digest = ?, decided_by = ?, updated_at = CURRENT_TIMESTAMP
                WHERE draft_id = ? AND version = ?""",
                next.datasetKey(), next.version(), next.state().name(), next.document(), next.semanticDigest(), next.revisionDigest(), next.decidedBy(),
                next.draftId(), expectedVersion) == 1;
    }

    private void event(DraftRecord draft, String actor) {
        control.update("INSERT INTO platform.statistical_contract_draft_event(draft_id, version, state_code, actor, revision_digest) VALUES(?,?,?,?,?)",
                draft.draftId(), draft.version(), draft.state().name(), actor, draft.revisionDigest());
    }

    private static <T> Optional<T> one(List<T> rows) { return rows.size() == 1 ? Optional.of(rows.get(0)) : Optional.empty(); }
}
