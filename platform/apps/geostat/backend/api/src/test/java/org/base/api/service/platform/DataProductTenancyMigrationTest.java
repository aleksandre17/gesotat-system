package org.base.api.service.platform;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Structure of the tenancy migration: additive, idempotent, free of any site or tenant literal, and
 * deferring every statement that names the column the same batch adds.
 */
class DataProductTenancyMigrationTest {
    private static final String MIGRATION = "104_data_product_tenancy.sql";

    private static Path backendRoot() {
        for (Path dir = Path.of("").toAbsolutePath(); dir != null; dir = dir.getParent()) {
            if (Files.isDirectory(dir.resolve(Path.of("core", "src", "main", "resources", "db", "platform")))) return dir;
        }
        throw new IllegalStateException("backend root not found from " + Path.of("").toAbsolutePath());
    }

    private static String script() throws Exception {
        return Files.readString(backendRoot().resolve(Path.of("core", "src", "main", "resources", "db", "platform", MIGRATION)));
    }

    @Test
    void itIsAdditiveAndIdempotent() throws Exception {
        String sql = script();
        assertTrue(sql.contains("IF NOT EXISTS (SELECT 1 FROM sys.columns") && sql.contains("name=N'tenant_key'"),
                "the column is added only when it is absent");
        assertTrue(sql.contains("ADD tenant_key NVARCHAR(160) NULL"), "existing products must stay UNASSIGNED");
        assertTrue(sql.contains("IF OBJECT_ID(N'platform.data_product_tenant_assignment',N'U') IS NULL"),
                "the assignment history is created only when it is absent");
        assertFalse(sql.contains("NOT NULL,\n  tenant_key") || sql.contains("ALTER COLUMN tenant_key"),
                "the migration must not make an existing product's tenant mandatory");
        for (String destructive : new String[]{"DROP TABLE", "DELETE FROM", "TRUNCATE"}) {
            assertFalse(sql.toUpperCase(java.util.Locale.ROOT).contains(destructive), "migration must be additive: " + destructive);
        }
    }

    @Test
    void statementsThatNameTheNewColumnAreDeferredThroughExec() throws Exception {
        String sql = script();
        assertTrue(sql.contains("EXEC(N'ALTER TABLE platform.data_product ADD CONSTRAINT ck_data_product_tenant_key_not_blank"),
                "the check constraint compiles only after the ALTER TABLE in the same batch");
        assertTrue(sql.contains("EXEC(N'CREATE INDEX ix_data_product_tenant_key ON platform.data_product(tenant_key)"),
                "the index compiles only after the ALTER TABLE in the same batch");
    }

    @Test
    void assignmentHistoryIsAppendOnlyAndAudited() throws Exception {
        String sql = script();
        assertTrue(sql.contains("tr_data_product_tenant_assignment_append_only"), "history must be append-only");
        assertTrue(sql.contains("assigned_by") && sql.contains("assigned_at") && sql.contains("previous_tenant_key"),
                "who assigned, when, and from which tenant must be recorded");
        assertTrue(sql.contains("ck_data_product_tenant_assignment_transfer_reason"),
                "a transfer away from an existing owner needs a recorded reason");
    }

    @Test
    void itCarriesNoSiteOrTenantLiteral() throws Exception {
        String sql = script();
        assertFalse(sql.contains("KIDS"), "engine migrations must not name a site");
        assertFalse(sql.matches("(?s).*INSERT\\s+(INTO\\s+)?platform\\.data_product_tenant_assignment.*"),
                "the migration must not assign any tenant; assignment is a governed, audited operation");
        assertFalse(sql.contains("UPDATE platform.data_product SET tenant_key"),
                "the migration must not set an owner for any product");
    }

    @Test
    void itIsRegisteredInTheRunner() throws Exception {
        String runner = Files.readString(backendRoot().resolve(
                Path.of("api", "src", "main", "java", "org", "base", "api", "service", "platform", "PlatformSchemaMigrationRunner.java")));
        assertTrue(runner.contains("db/platform/" + MIGRATION), "the migration must be registered");
        assertTrue(runner.indexOf("db/platform/103_") < runner.indexOf("db/platform/" + MIGRATION), "migrations run in numeric order");
    }
}
