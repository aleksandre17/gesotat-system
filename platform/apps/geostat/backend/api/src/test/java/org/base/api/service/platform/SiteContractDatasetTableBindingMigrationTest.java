package org.base.api.service.platform;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SiteContractDatasetTableBindingMigrationTest {
    @Test
    void bindsSiteDatasetIdentityToOneApprovedPhysicalTableFailingOnAmbiguity() throws Exception {
        String schema = resource("db/platform/097_site_contract_dataset_table_binding.sql");
        String backfill = resource("db/platform/098_site_contract_dataset_table_binding_backfill.sql");
        assertTrue(schema.contains("contract_table_definition_id BIGINT NULL"));
        assertTrue(schema.contains("FOREIGN KEY(contract_table_definition_id)"));
        assertTrue(backfill.contains("s.structure_code=d.dataset_code"));
        assertTrue(backfill.contains("t.revision=r.revision"));
        assertTrue(backfill.contains("t.lifecycle_status='APPROVED'"));
        assertTrue(backfill.contains("HAVING COUNT_BIG(*)>1"));
        assertTrue(backfill.contains("THROW 51098"));
    }

    private String resource(String name) throws Exception {
        try (var stream = getClass().getClassLoader().getResourceAsStream(name)) {
            assertTrue(stream != null, "migration must be packaged: " + name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
