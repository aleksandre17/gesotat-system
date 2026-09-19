package org.base.api.service.contract.approval;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Structure of migration 099; trigger behaviour is proven on SQL Server by ops/tests/sql/site-contract-revision-governance.sql. */
class SiteContractRevisionGovernanceMigrationTest {
    @Test
    void declaresAppendOnlyEvidenceImmutableRevisionsAndSingleApprovedRevision() throws Exception {
        String sql;
        try (var stream = getClass().getClassLoader().getResourceAsStream("db/platform/099_site_contract_revision_governance.sql")) {
            assertTrue(stream != null, "migration must be packaged");
            sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertTrue(sql.contains("UNIQUE(site_contract_revision_id)"));
        assertTrue(sql.contains("compatibility<>'BREAKING' OR LEN("));
        assertTrue(sql.contains("THROW 51040") && sql.contains("THROW 51041") && sql.contains("THROW 51042"));
        assertTrue(sql.contains("d.status=''APPROVED'' AND i.status=''SUPERSEDED''"));
        assertTrue(sql.contains("ISNULL(d.status,'''')<>''APPROVED''"), "only new approvals may be checked, or legacy rows block their own repair");
    }
}
