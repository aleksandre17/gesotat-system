package org.base.api.service.platform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;

/** Applies the platform's idempotent SQL Server DDL in deterministic plane order at startup. */
@Component
public class PlatformSchemaMigrationRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(PlatformSchemaMigrationRunner.class);
    static final String ADOPTION_PROBE_SUFFIX = ".adopt";
    private final JdbcTemplate control;
    private final JdbcTemplate data;
    private final JdbcTemplate archive;
    private final boolean enabled;
    private final ApplicationEventPublisher events;

    public PlatformSchemaMigrationRunner(@Qualifier("primaryJdbcTemplate") JdbcTemplate control,
                                         @Qualifier("dataPlaneJdbcTemplate") JdbcTemplate data,
                                         @Qualifier("archivePlaneJdbcTemplate") JdbcTemplate archive,
                                         @Value("${platform.schema-migration.enabled:true}") boolean enabled,
                                         ApplicationEventPublisher events) {
        this.control = control;
        this.data = data;
        this.archive = archive;
        this.enabled = enabled;
        this.events = events;
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
        executeAndRecord(control, "db/platform/054_contract_page_binding.sql");
        executeAndRecord(control, "db/platform/055_kids_final_page_contract_revision.sql");
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
        executeAndRecord(control, "db/platform/082_contract_approval_receipt.sql");
        executeAndRecord(control, "db/platform/083_data_family_lifecycle_state.sql");
        executeAndRecord(control, "db/platform/084_contract_revision_lifecycle.sql");
        executeAndRecord(control, "db/platform/085_canonical_storage_bindings.sql");
        executeAndRecord(control, "db/platform/086_artifact_attachment_meta_schema.sql");
        executeAndRecord(data, "db/platform/087_artifact_registry_data_plane.sql");
        executeAndRecord(control, "db/platform/088_kids_r8_resource_artifact_binding.sql");
        executeAndRecord(control, "db/platform/089_artifact_contract_lifecycle_immutability.sql");
        executeAndRecord(data, "db/platform/090_artifact_malware_quarantine.sql");
        executeAndRecord(data, "db/platform/091_artifact_manifest_contract_binding.sql");
        executeAndRecord(data, "db/platform/092_artifact_manifest_contract_binding_integrity.sql");
        executeAndRecord(data, "db/platform/093_artifact_upload_sessions.sql");
        executeAndRecord(data, "db/platform/094_artifact_integrity_audit.sql");
        executeAndRecord(data, "db/platform/095_access_package_malware_admission.sql");
        executeAndRecord(control, "db/platform/096_kids_r8_source_registry_reconciliation.sql");
        executeAndRecord(control, "db/platform/097_site_contract_dataset_table_binding.sql");
        executeAndRecord(control, "db/platform/098_site_contract_dataset_table_binding_backfill.sql");
        executeAndRecord(control, "db/platform/099_site_contract_revision_governance.sql");
        executeAndRecord(data, "db/platform/100_artifact_storage_sweep.sql");
        executeAndRecord(data, "db/platform/101_artifact_package_run.sql");
        executeAndRecord(data, "db/platform/102_artifact_manifest_document.sql");
        executeAndRecord(control, "db/platform/103_ingestion_contract_code_not_null_repair.sql");
        events.publishEvent(new PlatformSchemaReadyEvent(Instant.now()));
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
        /* Applied migrations run exactly once. Re-executing recorded scripts on every
           startup re-ran non-idempotent DML (new dataset versions, contract state
           resets) on each restart; the ledger is the single source of "applied". */
        if (recorded != null && recorded.equalsIgnoreCase(checksum)) return;
        if (recorded != null) {
            // These pre-release migrations were corrected before production
            // rollout without changing their applied effects. Reconcile their
            // ledger checksum once; all other drift fails closed.
            if (resource.endsWith("065_kids_r8_access_locator_bindings.sql")
                    || resource.endsWith("069_kids_r8_release_ready_semantics.sql")
                    || resource.endsWith("070_kids_r8_complete_mapping_specs.sql")
                    || resource.endsWith("097_site_contract_dataset_table_binding.sql")
                    /* Corrected so that an empty database can be built (statement order, deferred
                       compilation, guarded insert); what they did to databases that already
                       recorded them is unchanged. Proof: ops/tests/sql/migration-chain-fresh-replay.sh. */
                    || resource.endsWith("011_contract_stable_identity.sql")
                    || resource.endsWith("020_kids_r4_row_transport_contract.sql")
                    || resource.endsWith("021_kids_r5_artifact_raw_contract.sql")
                    || resource.endsWith("079_api_operation_idempotency.sql")
                    || resource.endsWith("080_api_operation_owner_scope.sql")
                    /* The file creates the same revision document that recorded installations hold
                       (SHA-256 94AFFBA7..., 15 datasets, 104 fields, 21 relations). */
                    || resource.endsWith("055_kids_final_page_contract_revision.sql")) {
                control.update("UPDATE platform.schema_migration SET checksum=? WHERE migration_id=?", checksum, resource);
                return;
            }
            throw new IllegalStateException("Schema migration checksum changed: " + resource + ". Create a new numbered migration instead of rewriting history.");
        }
        if (alreadyEffective(jdbc, resource)) {
            log.info("schema.migration adopted without execution (its effect is already present): {}", resource);
            control.update("INSERT INTO platform.schema_migration(migration_id,checksum) VALUES(?,?)", resource, checksum);
            return;
        }
        // Every statement must succeed before the script is recorded as applied.
        SqlScriptExecutor.execute(jdbc, text);
        control.update("INSERT INTO platform.schema_migration(migration_id,checksum) VALUES(?,?)", resource, checksum);
    }

    /**
     * A migration that is not idempotent may ship a probe beside it ({@code <migration>.adopt}): one SELECT
     * returning 1 when the migration's effect already exists. An installation that holds the effect but
     * not the ledger row then adopts the migration instead of executing it a second time.
     */
    private static boolean alreadyEffective(JdbcTemplate jdbc, String resource) throws Exception {
        ClassPathResource probe = new ClassPathResource(resource + ADOPTION_PROBE_SUFFIX);
        if (!probe.exists()) return false;
        String sql = new String(probe.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        try {
            return Boolean.TRUE.equals(jdbc.query(sql, rs -> rs.next() && rs.getInt(1) == 1));
        } catch (DataAccessException objectsNotCreatedYet) {
            // The probe names tables an empty database does not have: nothing to adopt.
            return false;
        }
    }

    private static String sha256(String text) throws Exception {
        byte[] hash=MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
        StringBuilder output=new StringBuilder(64); for(byte item:hash)output.append(String.format("%02x",item)); return output.toString();
    }
}
