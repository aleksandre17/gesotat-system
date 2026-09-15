package org.base.api.service.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** Proves approved persisted projection metadata controls the final response shape. */
class PersistedProjectionIntegrationTest {
    @Test void approvedRevisionProjectionIsLoadedAndAppliedToRows() {
        var ds = new DriverManagerDataSource("jdbc:h2:mem:projection_integration;MODE=MSSQLServer;DB_CLOSE_DELAY=-1", "sa", "");
        var jdbc = new JdbcTemplate(ds);
        jdbc.execute("CREATE SCHEMA platform");
        jdbc.execute("CREATE TABLE platform.api_projection (projection_code VARCHAR(128), dataset_code VARCHAR(128), projection_family VARCHAR(64), mapping_json VARCHAR(4000), approval_state VARCHAR(32), revision INT, site_contract_revision_id BIGINT)");
        jdbc.update("INSERT INTO platform.api_projection VALUES ('PUBLIC_GOAL', 'ALT_GOAL', 'ENTITY', '{\"include\":[\"label\"],\"fields\":{\"title\":\"label\"}}', 'APPROVED', 1, 9)");
        var service = new ContractProjectionService(jdbc, new ObjectMapper());
        Map<String,Object> envelope = new LinkedHashMap<>();
        envelope.put("data", List.of(new LinkedHashMap<>(Map.of("id", "g-1", "label", "Education", "secret", "redact"))));
        service.applyRows(envelope, "PUBLIC_GOAL", 9);
        var row = (Map<?,?>) ((List<?>) envelope.get("data")).get(0);
        assertEquals("g-1", row.get("id"));
        assertEquals("Education", row.get("label"));
        assertEquals("Education", row.get("title"));
        assertFalse(row.containsKey("secret"));
        assertEquals("APPROVED", service.definition(9, "PUBLIC_GOAL").get("state"));
    }
}
