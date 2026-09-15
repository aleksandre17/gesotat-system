package org.base.api.controller;

import org.base.core.anotation.NoApiPrefix;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.ObjectProvider;
import org.base.api.service.storage.ObjectStorageService;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Health check endpoint for Docker and monitoring.
 * GET /health → {"status":"UP","db":{"primary":"UP","secondary":"UP"}}
 */
@RestController
@NoApiPrefix
public class HealthController {

    private final JdbcTemplate primaryJdbc;
    private final JdbcTemplate secondaryJdbc;
    private final JdbcTemplate dataPlaneJdbc;
    private final JdbcTemplate archivePlaneJdbc;
    private final ObjectProvider<ObjectStorageService> storage;

    public HealthController(
            @Qualifier("primaryJdbcTemplate") JdbcTemplate primaryJdbc,
            @Qualifier("secondaryJdbcTemplate") JdbcTemplate secondaryJdbc,
            @Qualifier("dataPlaneJdbcTemplate") JdbcTemplate dataPlaneJdbc,
            @Qualifier("archivePlaneJdbcTemplate") JdbcTemplate archivePlaneJdbc,
            ObjectProvider<ObjectStorageService> storage) {
        this.primaryJdbc = primaryJdbc;
        this.secondaryJdbc = secondaryJdbc;
        this.dataPlaneJdbc = dataPlaneJdbc;
        this.archivePlaneJdbc = archivePlaneJdbc;
        this.storage = storage;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, String> db = new LinkedHashMap<>();

        boolean allUp = true;

        try {
            primaryJdbc.queryForObject("SELECT 1", Integer.class);
            db.put("primary", "UP");
        } catch (Exception e) {
            db.put("primary", "DOWN");
            allUp = false;
        }

        try {
            dataPlaneJdbc.queryForObject("SELECT 1", Integer.class);
            db.put("dataPlane", "UP");
        } catch (Exception e) {
            db.put("dataPlane", "DOWN");
            allUp = false;
        }

        try {
            archivePlaneJdbc.queryForObject("SELECT 1", Integer.class);
            db.put("archivePlane", "UP");
        } catch (Exception e) {
            db.put("archivePlane", "DOWN");
            allUp = false;
        }

        ObjectStorageService objectStorage = storage.getIfAvailable();
        if (objectStorage == null || !objectStorage.isReady()) {
            db.put("objectStorage", "DOWN");
            allUp = false;
        } else db.put("objectStorage", "UP");

        try {
            secondaryJdbc.queryForObject("SELECT 1", Integer.class);
            db.put("secondary", "UP");
        } catch (Exception e) {
            db.put("secondary", "DOWN");
            allUp = false;
        }

        result.put("status", allUp ? "UP" : "DOWN");
        result.put("db", db);

        return allUp
                ? ResponseEntity.ok(result)
                : ResponseEntity.status(503).body(result);
    }
}
