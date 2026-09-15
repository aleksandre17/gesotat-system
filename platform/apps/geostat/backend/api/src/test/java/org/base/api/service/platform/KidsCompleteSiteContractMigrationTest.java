package org.base.api.service.platform;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards the architectural promises that distinguish the complete KIDS revision-6 contract. */
class KidsCompleteSiteContractMigrationTest {
    @Test
    void revisionSixDeclaresTheWholeSiteAndDoesNotSeedStatisticalFacts() throws Exception {
        try (var stream = getClass().getClassLoader().getResourceAsStream("db/platform/022_kids_complete_site_contract.sql")) {
            assertTrue(stream != null, "revision-6 migration must be packaged with the application");
            String sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

            for (String registry : new String[]{
                    "site_contract_revision", "site_contract_node", "site_contract_dataset",
                    "site_contract_field", "site_contract_relation", "site_contract_classifier",
                    "statistical_dataflow", "statistical_dsd", "statistical_component", "site_contract_gate",
                    "ingestion_contract_revision", "contract_revision_source"}) {
                assertTrue(sql.contains("platform." + registry), "missing contract registry: " + registry);
            }
            for (String component : new String[]{
                    "TIME_PERIOD", "AGE_GROUP", "OBS_VALUE", "UNIT_MEASURE", "UNIT_MULTIPLIER",
                    "DECIMALS", "AGGREGATION", "OBS_STATUS", "CONF_STATUS",
                    "SOURCE_CARRIER", "SOURCE_CELL_KEY", "SOURCE_JSON_PATH"}) {
                assertTrue(sql.contains("N'" + component + "'"), "missing statistical component: " + component);
            }
            assertTrue(sql.contains("m.metric_code LIKE N'KIDS_FILE_%'"));
            assertTrue(sql.contains("status='SUPERSEDED'"));
            assertTrue(sql.contains("MERGE platform.contract_source"));
            assertTrue(sql.contains("ACCESS_CANONICAL_R6"));
            assertFalse(sql.contains("VALUES(162)"), "a fixed carrier/metric ID list must never return");
            assertFalse(sql.contains("payload_raw"), "the revision-6 contract must not declare duplicated raw JSON");
        }
    }
}
