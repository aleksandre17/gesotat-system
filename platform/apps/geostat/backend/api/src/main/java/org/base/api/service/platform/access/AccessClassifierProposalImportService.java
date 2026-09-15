package org.base.api.service.platform.access;

import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Row;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Locale;

/** Imports classifier evidence only; promotion into the authoritative registry is always an explicit review action. */
@Service
public class AccessClassifierProposalImportService {
    private final JdbcTemplate control;
    public AccessClassifierProposalImportService(@Qualifier("primaryJdbcTemplate") JdbcTemplate control) { this.control = control; }

    public long importEvidence(File artifact, SemanticAccessPackage pack, long batchId) throws Exception {
        if (batchId <= 0) throw new IllegalArgumentException("batchId must be positive");
        long count = 0;
        try (Database db = new DatabaseBuilder(artifact).setReadOnly(true).open()) {
            var items = table(db, "__cl_item");
            if (items != null) for (Row r : items) {
                insert("ITEM", text(r,"item_ref"), text(r,"version_ref"), text(r,"item_code"), text(r,"label_ka"), text(r,"label_en"), null, null, null, null, null, pack, batchId); count++;
            }
            var aliases = table(db, "__cl_alias");
            if (aliases != null) for (Row r : aliases) {
                insert("ALIAS", text(r,"alias_ref"), null, null, null, null, text(r,"item_ref"), text(r,"source_system"), text(r,"normalization_rule"), text(r,"source_code_raw"), text(r,"lookup_code_normalized"), pack, batchId); count++;
            }
            var hierarchy = table(db, "__cl_hierarchy");
            if (hierarchy != null) for (Row r : hierarchy) {
                insert("HIERARCHY", text(r,"child_item_ref"), text(r,"version_ref"), null, null, null, text(r,"parent_item_ref"), null, null, text(r,"child_item_ref"), text(r,"ordinal"), pack, batchId); count++;
            }
        }
        return count;
    }

    private void insert(String type, String subject, String version, String itemCode, String labelKa, String labelEn,
                        String itemRef, String sourceSystem, String normalization, String child, String ordinal,
                        SemanticAccessPackage pack, long batchId) {
        String sql = "MERGE platform.classifier_proposal AS t USING (SELECT ? contract_code,? contract_revision,? package_batch_id,? proposal_type,? subject_key,? scheme_code,? version_code,? item_code,? item_ref,? label_ka,? label_en,? source_system_code,? external_code_raw,? normalized_code,? normalization_rule) AS s " +
                "ON t.contract_code=s.contract_code AND t.contract_revision=s.contract_revision AND t.package_batch_id=s.package_batch_id AND t.proposal_type=s.proposal_type AND t.subject_key=s.subject_key " +
                "WHEN NOT MATCHED THEN INSERT(contract_code,contract_revision,package_batch_id,proposal_type,subject_key,scheme_code,version_code,item_code,item_ref,label_ka,label_en,source_system_code,external_code_raw,normalized_code,normalization_rule,state) VALUES(s.contract_code,s.contract_revision,s.package_batch_id,s.proposal_type,s.subject_key,s.scheme_code,s.version_code,s.item_code,s.item_ref,s.label_ka,s.label_en,s.source_system_code,s.external_code_raw,s.normalized_code,s.normalization_rule,'DRAFT');";
        control.update(sql, pack.contractCode(), pack.contractRevision(), batchId, type, subject == null ? "" : subject,
                scheme(version), version == null ? "" : version, itemCode, itemRef, labelKa, labelEn, sourceSystem,
                child, ordinal, normalization);
    }

    private static com.healthmarketscience.jackcess.Table table(Database db, String name) throws java.io.IOException { return db.getTable(name); }
    private static String text(Row row, String name) { Object value = row.get(name); return value == null ? null : String.valueOf(value).trim(); }
    private static String scheme(String version) { if (version == null || version.isBlank()) return "UNKNOWN"; int p=version.indexOf('|'); return p>0 ? version.substring(0,p).toUpperCase(Locale.ROOT) : "UNKNOWN"; }
}
