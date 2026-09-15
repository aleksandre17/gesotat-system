package org.base.mobile.controller;

import org.base.core.anotation.NoApiPrefix;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

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

    public HealthController(
            @Qualifier("primaryJdbcTemplate") JdbcTemplate primaryJdbc,
            @Qualifier("secondaryJdbcTemplate") JdbcTemplate secondaryJdbc) {
        this.primaryJdbc = primaryJdbc;
        this.secondaryJdbc = secondaryJdbc;
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
