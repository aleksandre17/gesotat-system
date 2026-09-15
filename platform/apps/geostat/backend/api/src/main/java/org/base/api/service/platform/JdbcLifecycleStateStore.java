package org.base.api.service.platform;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Objects;

/** SQL-backed lifecycle store; schema and state values are contract-governed. */
public final class JdbcLifecycleStateStore implements LifecycleStateStore {
    private final JdbcTemplate jdbc;
    public JdbcLifecycleStateStore(JdbcTemplate jdbc) { this.jdbc = Objects.requireNonNull(jdbc, "jdbc"); }
    @Override public DataFamilyLifecycle.State load(String lifecycleId) {
        return jdbc.query("SELECT state_code FROM platform.data_family_lifecycle WHERE lifecycle_id=?",
                rs -> rs.next() ? DataFamilyLifecycle.State.valueOf(rs.getString(1)) : null, lifecycleId);
    }
    @Override public void save(String lifecycleId, DataFamilyLifecycle.State state) {
        int updated = jdbc.update("UPDATE platform.data_family_lifecycle SET state_code=?, updated_at=CURRENT_TIMESTAMP WHERE lifecycle_id=?", state.name(), lifecycleId);
        if (updated == 0) {
            try { jdbc.update("INSERT INTO platform.data_family_lifecycle(lifecycle_id,state_code) VALUES(?,?)", lifecycleId, state.name()); }
            catch (org.springframework.dao.DuplicateKeyException race) {
                jdbc.update("UPDATE platform.data_family_lifecycle SET state_code=?, updated_at=CURRENT_TIMESTAMP WHERE lifecycle_id=?", state.name(), lifecycleId);
            }
        }
    }
}
