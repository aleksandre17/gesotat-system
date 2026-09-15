package org.base.api.service.platform.access;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Resolves a package binding against the authoritative Control Plane; it never creates metadata. */
@Service
public class SemanticAccessControlPlaneResolver {
    private final JdbcTemplate control;

    public SemanticAccessControlPlaneResolver(@Qualifier("primaryJdbcTemplate") JdbcTemplate control) { this.control=control; }

    public List<SemanticAccessIssue> validate(SemanticAccessPackage pack) {
        List<SemanticAccessIssue> issues=new ArrayList<>();
        List<Map<String,Object>> contracts=control.queryForList("SELECT c.contract_id,c.contract_revision,c.status,p.product_code FROM platform.ingestion_contract c JOIN platform.dataset d ON d.dataset_id=c.dataset_id JOIN platform.data_product p ON p.product_id=d.product_id WHERE c.contract_code=?",pack.contractCode());
        if(contracts.isEmpty()) { issues.add(new SemanticAccessIssue("UNKNOWN_CONTROL_CONTRACT",pack.contractCode(),"contract_code is not registered in the Control Plane")); return issues; }
        Map<String,Object> contract=contracts.get(0);
        if(!String.valueOf(contract.get("product_code")).equalsIgnoreCase(pack.productCode())) issues.add(new SemanticAccessIssue("CONTRACT_PRODUCT_MISMATCH",pack.contractCode(),"package product_code does not own this contract"));
        int currentRevision=((Number)contract.get("contract_revision")).intValue();
        List<Map<String,Object>> sources;
        /* A package revision is resolved against its immutable revision registry
           first.  Surrogate IDs in the mutable current contract are not a
           substitute for a historical revision, even when revision numbers
           happen to match. */
        List<Map<String,Object>> revisions=control.queryForList("SELECT ingestion_contract_revision_id,lifecycle_status FROM platform.ingestion_contract_revision WHERE contract_id=? AND revision=? ORDER BY ingestion_contract_revision_id DESC",contract.get("contract_id"),pack.contractRevision());
        if(!revisions.isEmpty()) {
            sources=control.queryForList("SELECT d.dataset_code,rs.source_locator FROM platform.contract_revision_source rs JOIN platform.dataset_version dv ON dv.dataset_version_id=rs.target_dataset_version_id JOIN platform.dataset d ON d.dataset_id=dv.dataset_id WHERE rs.ingestion_contract_revision_id=?",revisions.get(0).get("ingestion_contract_revision_id"));
        } else if(currentRevision==pack.contractRevision()) {
            sources=control.queryForList("SELECT d.dataset_code,cs.source_locator FROM platform.contract_source cs JOIN platform.dataset_version dv ON dv.dataset_version_id=cs.target_dataset_version_id JOIN platform.dataset d ON d.dataset_id=dv.dataset_id WHERE cs.contract_id=? AND cs.active=1",contract.get("contract_id"));
        } else {
            if(revisions.isEmpty()) {
                issues.add(new SemanticAccessIssue("CONTRACT_REVISION_MISMATCH",pack.contractCode(),"package contract_revision is neither current nor preserved in the Control Plane"));
                return issues;
            }
            sources=control.queryForList("SELECT d.dataset_code,rs.source_locator FROM platform.contract_revision_source rs JOIN platform.dataset_version dv ON dv.dataset_version_id=rs.target_dataset_version_id JOIN platform.dataset d ON d.dataset_id=dv.dataset_id WHERE rs.ingestion_contract_revision_id=?",revisions.get(0).get("ingestion_contract_revision_id"));
        }
        for(SemanticAccessDataset dataset:pack.datasets()) {
            boolean matched=sources.stream().anyMatch(source->String.valueOf(source.get("dataset_code")).equalsIgnoreCase(dataset.datasetCode())&&tableEquivalent(tableName(String.valueOf(source.get("source_locator"))),dataset.accessTableName()));
            if(!matched) issues.add(new SemanticAccessIssue("UNBOUND_PACKAGE_DATASET",dataset.datasetCode(),"No active Control-Plane source binding matches dataset_code and access_table_name"));
        }
        return issues;
    }
    private static String tableName(String locator) { int dot=locator.lastIndexOf('.'); return dot<0?locator:locator.substring(dot+1); }
    /** Control bindings may use the logical legacy table name while an issued
     * Access package uses its family namespace prefix. Both resolve to the
     * same governed dataset; no site-specific exception is needed. */
    private static boolean tableEquivalent(String left,String right) {
        if(left.equalsIgnoreCase(right)) return true;
        return unprefix(left).equalsIgnoreCase(unprefix(right));
    }
    private static String unprefix(String name) {
        String n=name;
        for(String prefix:new String[]{"__ent_","__rel_","__raw_","__stat_"}) if(n.regionMatches(true,0,prefix,0,prefix.length())) return n.substring(prefix.length());
        return n;
    }
}
