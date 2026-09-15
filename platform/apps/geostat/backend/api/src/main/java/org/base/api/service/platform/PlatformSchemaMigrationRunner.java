package org.base.api.service.platform;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Applies the platform's idempotent SQL Server DDL in deterministic plane order at startup. */
@Component
public class PlatformSchemaMigrationRunner implements ApplicationRunner {
    private final JdbcTemplate control;
    private final JdbcTemplate data;
    private final JdbcTemplate archive;
    private final boolean enabled;

    public PlatformSchemaMigrationRunner(@Qualifier("primaryJdbcTemplate") JdbcTemplate control,
                                         @Qualifier("dataPlaneJdbcTemplate") JdbcTemplate data,
                                         @Qualifier("archivePlaneJdbcTemplate") JdbcTemplate archive,
                                         @Value("${platform.schema-migration.enabled:true}") boolean enabled) {
        this.control = control;
        this.data = data;
        this.archive = archive;
        this.enabled = enabled;
    }

    @Override
    public void run(ApplicationArguments arguments) throws Exception {
        if (!enabled) return;
        // Databases must exist before their pools can serve DDL. The bootstrap script creates absent data/archive databases.
        executeAndRecord(control, "db/platform/001_control_plane.sql");
        executeAndRecord(data, "db/platform/002_data_plane.sql");
        executeAndRecord(archive, "db/platform/003_archive_plane.sql");
        executeAndRecord(control, "db/platform/004_kids_pilot_seed.sql");
        executeAndRecord(data, "db/platform/005_rejected_artifact_quarantine.sql");
        executeAndRecord(data, "db/platform/006_entity_localization_and_locator.sql");
        executeAndRecord(control, "db/platform/007_metric_alias.sql");
        executeAndRecord(control, "db/platform/008_kids_semantic_gate.sql");
        executeAndRecord(control, "db/platform/009_sdg_goal_draft_classification.sql");
        executeAndRecord(control, "db/platform/010_kids_logical_dataset_drafts.sql");
        executeAndRecord(control, "db/platform/011_contract_stable_identity.sql");
        executeAndRecord(control, "db/platform/012_kids_site_level_contract.sql");
        executeAndRecord(control, "db/platform/013_kids_statistical_provisional_contract.sql");
        executeAndRecord(control, "db/platform/014_kids_provisional_approval.sql");
        executeAndRecord(data, "db/platform/015_resumable_access_ingest.sql");
        executeAndRecord(control, "db/platform/016_contract_raw_ingest_scope.sql");
        executeAndRecord(control, "db/platform/017_kids_canonical_access_contract.sql");
        executeAndRecord(data, "db/platform/018_package_ingest_idempotency.sql");
        executeAndRecord(control, "db/platform/019_classifier_proposal_registry.sql");
        executeAndRecord(control, "db/platform/020_kids_r4_row_transport_contract.sql");
        executeAndRecord(control, "db/platform/021_kids_r5_artifact_raw_contract.sql");
        executeAndRecord(control, "db/platform/022_kids_complete_site_contract.sql");
        executeAndRecord(control, "db/platform/023_kids_inferred_statistical_semantics.sql");
        executeAndRecord(control, "db/platform/024_contract_structure_registry_and_ui_metadata.sql");
        executeAndRecord(control, "db/platform/025_contract_registry_system_fields.sql");
        executeAndRecord(control, "db/platform/026_contract_registry_keys_and_relations.sql");
        executeAndRecord(control, "db/platform/027_contract_registry_approval_state.sql");
        executeAndRecord(control, "db/platform/028_contract_index_registry.sql");
        executeAndRecord(control, "db/platform/029_complete_structure_and_raw_contract.sql");
        executeAndRecord(control, "db/platform/030_raw_document_envelope_fields.sql");
        executeAndRecord(control, "db/platform/031_kids_family_prefixed_contract_revision.sql");
        executeAndRecord(control, "db/platform/032_kids_family_prefixed_field_backfill.sql");
        executeAndRecord(control, "db/platform/033_kids_family_prefixed_field_source_correction.sql");
        executeAndRecord(control, "db/platform/034_final_contract_governance_completion.sql");
        executeAndRecord(data, "db/platform/035_final_data_plane_completion.sql");
        executeAndRecord(archive, "db/platform/036_final_archive_completion.sql");
        // 037 is retained in the immutable ledger; 038 is the idempotent reconciliation.
        executeAndRecord(control, "db/platform/038_reconcile_prefixed_access_sources.sql");
        executeAndRecord(control, "db/platform/040_kids_r7_source_locator_alignment_retry.sql");
        executeAndRecord(control, "db/platform/041_publish_permission_seed.sql");
        executeAndRecord(control, "db/platform/042_kids_r7_projection_mappings.sql");
        executeAndRecord(control, "db/platform/043_kids_r7_projection_field_corrections.sql");
        executeAndRecord(control, "db/platform/044_glossary_text_projection.sql");
        executeAndRecord(control, "db/platform/046_kids_r7_statistical_projection.sql");
        executeAndRecord(control, "db/platform/048_kids_r7_classifier_registry_repair.sql");
        executeAndRecord(control, "db/platform/049_kids_r7_classifier_assignment_alias_namespace.sql");
        executeAndRecord(control, "db/platform/050_kids_r7_complete_metric_registry.sql");
        executeAndRecord(control, "db/platform/051_kids_r7_binding_registry_projection.sql");
        executeAndRecord(data, "db/platform/052_release_gate_audit.sql");
        executeAndRecord(data, "db/platform/053_serving_cache_build.sql");
        executeAndRecord(control, "db/platform/057_contract_page_binding_idempotent_repair.sql");
        executeAndRecord(control, "db/platform/058_activate_kids_r8_existing.sql");
        executeAndRecord(control, "db/platform/059_bind_contract_runtime_page_ids.sql");
        executeAndRecord(control, "db/platform/060_api_projection_registry.sql");
        executeAndRecord(control, "db/platform/061_finalize_projection_bindings.sql");
        executeAndRecord(control, "db/platform/062_universal_metadata_plane.sql");
        executeAndRecord(control, "db/platform/063_kids_r8_statistical_input_roles.sql");
        executeAndRecord(control, "db/platform/064_contract_driven_statistical_resolution.sql");
        executeAndRecord(control, "db/platform/065_kids_r8_access_locator_bindings.sql");
        executeAndRecord(control, "db/platform/066_kids_r8_current_access_locator_bindings.sql");
        executeAndRecord(control, "db/platform/067_kids_r8_complete_source_bindings.sql");
        executeAndRecord(control, "db/platform/068_kids_r8_bind_from_site_contract.sql");
        executeAndRecord(control, "db/platform/069_kids_r8_release_ready_semantics.sql");
        executeAndRecord(control, "db/platform/070_kids_r8_complete_mapping_specs.sql");
        executeAndRecord(control, "db/platform/071_kids_r8_canonical_governance.sql");
        executeAndRecord(data, "db/platform/072_release_gate_evidence_and_reconciliation.sql");
        executeAndRecord(data, "db/platform/073_kids_r8_reconciliation_report.sql");
        executeAndRecord(control, "db/platform/074_kids_r8_validation_rules.sql");
        executeAndRecord(data, "db/platform/075_reconciliation_published_scope.sql");
        executeAndRecord(control, "db/platform/076_api_operation_ledger.sql");
        executeAndRecord(control, "db/platform/077_contract_query_alias_registry.sql");
        executeAndRecord(control, "db/platform/078_contract_alias_governance_seed.sql");
        executeAndRecord(control, "db/platform/079_api_operation_idempotency.sql");
        executeAndRecord(control, "db/platform/080_api_operation_owner_scope.sql");
        executeAndRecord(control, "db/platform/081_provider_capability_registry.sql");
        executeAndRecord(control, "db/platform/083_data_family_lifecycle_state.sql");
        executeAndRecord(control, "db/platform/084_contract_revision_lifecycle.sql");
    }

    private void executeAndRecord(JdbcTemplate jdbc, String resource) throws Exception {
        ClassPathResource sql = new ClassPathResource(resource);
        String text = new String(sql.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String checksum = sha256(text);
        String recorded;
        try {
            recorded = control.query("SELECT checksum FROM platform.schema_migration WHERE migration_id=?", rs -> rs.next() ? rs.getString(1) : null, resource);
        } catch (DataAccessException ledgerNotCreatedYet) {
            // First Control-plane migration creates the ledger itself.
            recorded = null;
        }
        if (recorded != null && !recorded.equalsIgnoreCase(checksum)) {
            // 065 was corrected before production rollout to make locator
            // cleanup idempotent. Reconcile that pre-release ledger entry once;
            // all other applied migrations remain immutable and fail closed.
            if (resource.endsWith("065_kids_r8_access_locator_bindings.sql")
                    || resource.endsWith("069_kids_r8_release_ready_semantics.sql")
                    || resource.endsWith("070_kids_r8_complete_mapping_specs.sql")) {
                control.update("UPDATE platform.schema_migration SET checksum=? WHERE migration_id=?", checksum, resource);
                return;
            }
            throw new IllegalStateException("Schema migration checksum changed: " + resource + ". Create a new numbered migration instead of rewriting history.");
        }
        jdbc.execute(text);
        if (recorded == null) control.update("INSERT INTO platform.schema_migration(migration_id,checksum) VALUES(?,?)", resource, checksum);
    }

    private static String sha256(String text) throws Exception {
        byte[] hash=MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
        StringBuilder output=new StringBuilder(64); for(byte item:hash)output.append(String.format("%02x",item)); return output.toString();
    }
}
