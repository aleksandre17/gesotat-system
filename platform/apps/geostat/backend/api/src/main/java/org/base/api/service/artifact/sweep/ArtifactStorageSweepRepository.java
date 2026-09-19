package org.base.api.service.artifact.sweep;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Data Plane state of the storage sweep: scopes, resumable cursors and orphan evidence. */
@Repository
public class ArtifactStorageSweepRepository {
    private final JdbcTemplate dataPlane;

    public record Scope(long sweepId, String bucket, String prefix, String cursorKey) {}

    public record Orphan(String key, long byteSize) {}

    /**
     * One listed page, already classified.
     *
     * @param lastKey  last listed key, or {@code null} for an empty page
     * @param finished the listing ended with this page, so the cycle is complete
     */
    public record PageResult(String lastKey, int objectsListed, List<Orphan> orphans, Set<String> listedKeys, Set<String> registeredKeys, boolean finished) {}

    public ArtifactStorageSweepRepository(@Qualifier("dataPlaneJdbcTemplate") JdbcTemplate dataPlane) {
        this.dataPlane = dataPlane;
    }

    /**
     * Registers the scopes to sweep: the configured upload pool plus every (bucket, directory) that
     * holds a registered object. Scopes are data; no prefix is known to the code.
     */
    public void syncScopes(String bucket, String uploadPrefix) {
        dataPlane.update("INSERT INTO ingest.artifact_storage_sweep(bucket,prefix) SELECT s.bucket,s.prefix FROM ("
                + "SELECT ? AS bucket, ? AS prefix UNION "
                + "SELECT DISTINCT bucket, LEFT(object_key, LEN(object_key)-CHARINDEX('/',REVERSE(object_key))+1) FROM ingest.artifact_object WHERE CHARINDEX('/',object_key)>0"
                + ") s WHERE NOT EXISTS(SELECT 1 FROM ingest.artifact_storage_sweep e WHERE e.bucket=s.bucket AND e.prefix=s.prefix)", bucket, uploadPrefix);
    }

    /** The scope that has waited longest for progress. */
    public Optional<Scope> nextScope() {
        return dataPlane.query("SELECT TOP 1 storage_sweep_id,bucket,prefix,cursor_key FROM ingest.artifact_storage_sweep ORDER BY last_progress_at,storage_sweep_id",
                (rs, n) -> new Scope(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4))).stream().findFirst();
    }

    public Set<String> registeredKeys(String bucket, Collection<String> keys) {
        Set<String> registered = new HashSet<>();
        if (keys.isEmpty()) return registered;
        String placeholders = String.join(",", keys.stream().map(key -> "?").toList());
        Object[] arguments = new Object[keys.size() + 1];
        arguments[0] = bucket;
        int index = 1;
        for (String key : keys) arguments[index++] = key;
        dataPlane.query("SELECT object_key FROM ingest.artifact_object WHERE bucket=? AND object_key IN (" + placeholders + ")",
                rs -> { registered.add(rs.getString(1)); }, arguments);
        return registered;
    }

    /** Applies one page atomically: orphan evidence, resolutions inside the page's key range, and the cursor. */
    @Transactional(transactionManager = "dataPlaneTransactionManager")
    public void recordPage(Scope scope, PageResult page) {
        for (Orphan orphan : page.orphans()) {
            int seen = dataPlane.update("UPDATE ingest.artifact_storage_orphan SET last_detected_at=SYSUTCDATETIME(),occurrence_count=occurrence_count+1,byte_size=?"
                    + " WHERE bucket=? AND object_key=? AND resolved_at IS NULL", orphan.byteSize(), scope.bucket(), orphan.key());
            if (seen == 0) dataPlane.update("INSERT INTO ingest.artifact_storage_orphan(bucket,object_key,byte_size) VALUES(?,?,?)",
                    scope.bucket(), orphan.key(), orphan.byteSize());
        }
        resolveWithinRange(scope, page);
        if (page.finished()) {
            dataPlane.update("UPDATE ingest.artifact_storage_sweep SET last_completed_at=SYSUTCDATETIME(),"
                    + "last_completed_objects_listed=cycle_objects_listed+?,last_completed_orphans_detected=cycle_orphans_detected+?,"
                    + "cycle=cycle+1,cursor_key=NULL,cycle_objects_listed=0,cycle_orphans_detected=0,cycle_started_at=SYSUTCDATETIME(),last_progress_at=SYSUTCDATETIME()"
                    + " WHERE storage_sweep_id=?", page.objectsListed(), page.orphans().size(), scope.sweepId());
        } else {
            dataPlane.update("UPDATE ingest.artifact_storage_sweep SET cursor_key=?,cycle_objects_listed=cycle_objects_listed+?,"
                    + "cycle_orphans_detected=cycle_orphans_detected+?,last_progress_at=SYSUTCDATETIME() WHERE storage_sweep_id=?",
                    page.lastKey(), page.objectsListed(), page.orphans().size(), scope.sweepId());
        }
    }

    /**
     * Open orphans between the previous cursor and this page's end were either registered since
     * (REGISTERED) or are no longer in storage (ABSENT). A finished page covers the rest of the scope.
     * Keys compare in binary order, the order Object Storage lists them in.
     */
    private void resolveWithinRange(Scope scope, PageResult page) {
        String from = scope.cursorKey() == null ? "" : scope.cursorKey();
        List<String> open = page.finished()
                ? dataPlane.queryForList("SELECT object_key FROM ingest.artifact_storage_orphan WHERE bucket=? AND resolved_at IS NULL"
                        + " AND object_key LIKE ? ESCAPE '\\' AND object_key COLLATE Latin1_General_BIN2>?", String.class, scope.bucket(), likePrefix(scope.prefix()), from)
                : dataPlane.queryForList("SELECT object_key FROM ingest.artifact_storage_orphan WHERE bucket=? AND resolved_at IS NULL"
                        + " AND object_key LIKE ? ESCAPE '\\' AND object_key COLLATE Latin1_General_BIN2>? AND object_key COLLATE Latin1_General_BIN2<=?", String.class, scope.bucket(),
                        likePrefix(scope.prefix()), from, page.lastKey());
        for (String key : open) {
            // Listed but neither registered nor reported is an object still inside its grace period: stays open.
            String resolution = page.registeredKeys().contains(key) ? "REGISTERED" : page.listedKeys().contains(key) ? null : "ABSENT";
            if (resolution != null) dataPlane.update("UPDATE ingest.artifact_storage_orphan SET resolved_at=SYSUTCDATETIME(),resolution=?"
                    + " WHERE bucket=? AND object_key=? AND resolved_at IS NULL", resolution, scope.bucket(), key);
        }
    }

    private static String likePrefix(String prefix) {
        return prefix.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_").replace("[", "\\[") + "%";
    }
}
