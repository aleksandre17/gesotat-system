package org.base.api.service.platform;

import org.springframework.jdbc.core.JdbcTemplate;
import java.util.Objects;

/** SQL-backed contract lifecycle store; approval evidence remains a separate immutable ledger. */
public final class JdbcContractLifecycleStateStore implements ContractLifecycleStateStore {
    private final JdbcTemplate jdbc;
    public JdbcContractLifecycleStateStore(JdbcTemplate jdbc) { this.jdbc = Objects.requireNonNull(jdbc, "jdbc"); }
    @Override public ContractLifecycle.State load(String revisionKey) {
        return jdbc.query("SELECT state_code FROM platform.contract_revision_lifecycle WHERE revision_key=?",
                rs -> rs.next() ? ContractLifecycle.State.valueOf(rs.getString(1)) : null, revisionKey);
    }
    @Override public void save(String revisionKey, ContractLifecycle.State state) {
        int updated = jdbc.update("UPDATE platform.contract_revision_lifecycle SET state_code=?, updated_at=CURRENT_TIMESTAMP WHERE revision_key=?", state.name(), revisionKey);
        if (updated == 0) {
            try { jdbc.update("INSERT INTO platform.contract_revision_lifecycle(revision_key,state_code) VALUES(?,?)", revisionKey, state.name()); }
            catch (org.springframework.dao.DuplicateKeyException race) {
                jdbc.update("UPDATE platform.contract_revision_lifecycle SET state_code=?, updated_at=CURRENT_TIMESTAMP WHERE revision_key=?", state.name(), revisionKey);
            }
        }
    }
}
