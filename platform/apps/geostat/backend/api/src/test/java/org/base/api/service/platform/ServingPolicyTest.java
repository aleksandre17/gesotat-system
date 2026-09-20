package org.base.api.service.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Serving decisions of the approved contract (migration 109, AIR-2026-045): the Data Plane is served, a
 * non-DATA table only when the contract says so, and only to a caller holding the authority it demands.
 */
class ServingPolicyTest {
    private static final String CARRIER = "KIDS_STATISTICAL_CARRIER", RAW = "KIDS_RAW_DOCUMENT", ENTITY = "KIDS_RESOURCE", SEMANTIC = "KIDS_STATISTICAL_METRIC";
    private JdbcTemplate jdbc;
    private ServingPolicy policy;

    @BeforeEach void schema() {
        SingleConnectionDataSource dataSource = new SingleConnectionDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MSSQLServer;CASE_INSENSITIVE_IDENTIFIERS=TRUE", true);
        jdbc = new JdbcTemplate(dataSource);
        for (String schema : new String[]{"platform", "publication", "raw"}) jdbc.execute("CREATE SCHEMA " + schema);
        jdbc.execute("CREATE TABLE platform.contract_structure(structure_id BIGINT IDENTITY PRIMARY KEY, lifecycle_status VARCHAR(24) NOT NULL)");
        jdbc.execute("""
                CREATE TABLE platform.contract_table_definition(table_definition_id BIGINT IDENTITY PRIMARY KEY, structure_id BIGINT NOT NULL,
                  logical_table_code VARCHAR(160) NOT NULL, storage_plane VARCHAR(32) NOT NULL, lifecycle_status VARCHAR(24) NOT NULL,
                  revision INT NOT NULL, servable BIT DEFAULT 0 NOT NULL, serving_authority VARCHAR(64))""");
        jdbc.execute("CREATE TABLE platform.dataset(dataset_id BIGINT IDENTITY PRIMARY KEY, dataset_code VARCHAR(120) NOT NULL)");
        jdbc.execute("CREATE TABLE platform.dataset_version(dataset_version_id BIGINT IDENTITY PRIMARY KEY, dataset_id BIGINT NOT NULL)");
        jdbc.execute("CREATE TABLE publication.dataset_snapshot(dataset_snapshot_id BIGINT IDENTITY PRIMARY KEY, dataset_version_id BIGINT NOT NULL, status VARCHAR(24) NOT NULL)");
        jdbc.execute("CREATE TABLE raw.source_record(source_record_id BIGINT IDENTITY PRIMARY KEY, dataset_snapshot_id BIGINT NOT NULL, payload_json VARCHAR(4000) NOT NULL)");
        jdbc.update("INSERT INTO platform.contract_structure(lifecycle_status) VALUES('APPROVED')");
        table(ENTITY, "DATA", false, null);
        table(CARRIER, "ACCESS", true, null);
        table(RAW, "ACCESS", true, "RAW_READ");
        table(SEMANTIC, "ACCESS", false, null);
        policy = new ServingPolicy(jdbc, jdbc, new ObjectMapper());
    }

    @AfterEach void clearCaller() { SecurityContextHolder.clearContext(); }

    private void table(String code, String plane, boolean servable, String authority) {
        jdbc.update("INSERT INTO platform.contract_table_definition(structure_id,logical_table_code,storage_plane,lifecycle_status,revision,servable,serving_authority) VALUES(1,?,?,'APPROVED',8,?,?)",
                code, plane, servable, authority);
    }

    private void caller(String... authorities) {
        TestingAuthenticationToken token = new TestingAuthenticationToken("caller", "n/a",
                java.util.Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList());
        token.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(token);
    }

    @Test void theDataPlaneIsServedAndAnUndeclaredTableIsNot() {
        caller("READ_RESOURCE");
        assertTrue(policy.permits(8, ENTITY));
        assertEquals("DATA", policy.require(8, ENTITY).storagePlane());
        assertFalse(policy.permits(8, "NO_SUCH_TABLE"), "a table the contract does not declare is never served");
        assertThrows(IllegalArgumentException.class, () -> policy.require(8, "NO_SUCH_TABLE"));
    }

    @Test void aNonDataTableIsServedOnlyWhenTheContractSaysSo() {
        caller("READ_RESOURCE");
        assertTrue(policy.permits(8, CARRIER), "lineage the contract publishes to every reader");
        assertFalse(policy.permits(8, SEMANTIC), "deny by default: not marked servable");
        ServingPolicy.ServingForbidden refused = assertThrows(ServingPolicy.ServingForbidden.class, () -> policy.require(8, SEMANTIC));
        assertTrue(refused.getMessage().contains(SEMANTIC));
    }

    @Test void rawDocumentsNeedTheirAuthorityAndAnonymousCallersGetNothing() {
        caller("READ_RESOURCE");
        assertFalse(policy.permits(8, RAW), "raw source documents are not page content");
        assertTrue(assertThrows(ServingPolicy.ServingForbidden.class, () -> policy.require(8, RAW)).getMessage().contains("RAW_READ"));
        caller("READ_RESOURCE", "RAW_READ");
        assertTrue(policy.permits(8, RAW));
        assertEquals(RAW, policy.require(8, RAW).logicalTableCode());
        SecurityContextHolder.clearContext();
        assertFalse(policy.permits(8, RAW), "no caller, no raw access");
        assertTrue(policy.permits(8, CARRIER), "a table without a required authority stays readable");
    }

    @Test void rowsComeFromThePublishedSnapshotAndCarryOnlyDeclaredFields() {
        jdbc.update("INSERT INTO platform.dataset(dataset_code) VALUES(?)", CARRIER);
        jdbc.update("INSERT INTO platform.dataset_version(dataset_id) VALUES(1)");
        jdbc.update("INSERT INTO platform.dataset_version(dataset_id) VALUES(1)");
        jdbc.update("INSERT INTO publication.dataset_snapshot(dataset_version_id,status) VALUES(1,'PUBLISHED')");
        jdbc.update("INSERT INTO publication.dataset_snapshot(dataset_version_id,status) VALUES(2,'REVIEW_REQUIRED')");
        jdbc.update("INSERT INTO raw.source_record(dataset_snapshot_id,payload_json) VALUES(1,?)", "{\"carrier_code\":\"CARRIER|1\",\"parse_status\":\"PARSED_ARRAY\",\"payload_checksum\":\"abc\"}");
        jdbc.update("INSERT INTO raw.source_record(dataset_snapshot_id,payload_json) VALUES(2,?)", "{\"carrier_code\":\"DRAFT|2\",\"parse_status\":\"PARSED_ARRAY\"}");

        List<Map<String, Object>> rows = policy.rawRows(CARRIER, List.of("carrier_code", "parse_status"), 100);
        assertEquals(1, rows.size(), "only the published snapshot is served");
        assertEquals(Map.of("carrier_code", "CARRIER|1", "parse_status", "PARSED_ARRAY"), rows.get(0));
        assertFalse(rows.get(0).containsKey("payload_checksum"), "a field the contract does not declare is not served");
        assertTrue(policy.rawRows("NOT_A_DATASET", List.of("x"), 10).isEmpty());
    }

    @Test void unreadablePayloadIsSkippedRatherThanFailingTheAnswer() {
        assertEquals(List.of(Map.of("a", "1")), policy.project(List.of("{\"a\":\"1\"}", "not json"), List.of("a")));
    }
}
