package org.base.api.service.contract.approval;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Control Plane persistence of site contract revision state and approval evidence. */
@Repository
public class SiteContractRevisionRepository {
    private final JdbcTemplate controlPlane;

    /** {@code checksumMatchesDocument} is computed by the same function that recorded the checksum. */
    public record Revision(long revisionId, long productId, String contractCode, int revision, String status,
                           String compatibilityMode, String checksum, boolean checksumMatchesDocument) {}

    public record Approval(long revisionId, String checksum, Long parentRevisionId, String compatibility, String approvedBy) {}

    public SiteContractRevisionRepository(@Qualifier("primaryJdbcTemplate") JdbcTemplate controlPlane) {
        this.controlPlane = controlPlane;
    }

    /** Every revision of the contract, in revision order. */
    public List<Revision> revisions(String contractCode) {
        return select(contractCode, "");
    }

    /** As {@link #revisions}, holding a key-range lock so concurrent approvals of one contract serialize. */
    public List<Revision> lockRevisions(String contractCode) {
        return select(contractCode, " WITH (UPDLOCK, HOLDLOCK)");
    }

    private List<Revision> select(String contractCode, String hint) {
        return controlPlane.query("SELECT site_contract_revision_id,product_id,contract_code,revision,status,compatibility_mode,contract_checksum,"
                        + "CASE WHEN contract_checksum=CONVERT(VARCHAR(64),HASHBYTES('SHA2_256',CONVERT(VARBINARY(MAX),contract_document_json)),2) THEN 1 ELSE 0 END"
                        + " FROM platform.site_contract_revision" + hint + " WHERE contract_code=? ORDER BY revision",
                (rs, n) -> new Revision(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getInt(4), rs.getString(5),
                        rs.getString(6), rs.getString(7), rs.getInt(8) == 1), contractCode);
    }

    public int datasetCount(long revisionId) {
        return count("SELECT COUNT(*) FROM platform.site_contract_dataset WHERE site_contract_revision_id=?", revisionId);
    }

    /** Datasets whose dataset version belongs to a product other than the revision's own. */
    public int foreignProductDatasetCount(long revisionId) {
        return count("SELECT COUNT(*) FROM platform.site_contract_dataset cd"
                + " JOIN platform.site_contract_revision r ON r.site_contract_revision_id=cd.site_contract_revision_id"
                + " JOIN platform.dataset_version v ON v.dataset_version_id=cd.dataset_version_id"
                + " JOIN platform.dataset d ON d.dataset_id=v.dataset_id"
                + " WHERE cd.site_contract_revision_id=? AND d.product_id<>r.product_id", revisionId);
    }

    public Optional<Approval> approval(long revisionId) {
        return controlPlane.query("SELECT site_contract_revision_id,contract_checksum,parent_revision_id,compatibility,approved_by"
                        + " FROM platform.site_contract_revision_approval WHERE site_contract_revision_id=?",
                (rs, n) -> new Approval(rs.getLong(1), rs.getString(2), (Long) rs.getObject(3), rs.getString(4), rs.getString(5)),
                revisionId).stream().findFirst();
    }

    public void supersede(long revisionId) {
        controlPlane.update("UPDATE platform.site_contract_revision SET status='SUPERSEDED' WHERE site_contract_revision_id=? AND status='APPROVED'", revisionId);
    }

    /** @return false when the revision was no longer awaiting review (lost race; caller must abort) */
    public boolean approve(long revisionId) {
        return controlPlane.update("UPDATE platform.site_contract_revision SET status='APPROVED',effective_from=COALESCE(effective_from,SYSUTCDATETIME())"
                + " WHERE site_contract_revision_id=? AND status='REVIEW_REQUIRED'", revisionId) == 1;
    }

    public void recordApproval(Approval approval, String breakingAcknowledgement, String evidenceJson) {
        controlPlane.update("INSERT INTO platform.site_contract_revision_approval(site_contract_revision_id,contract_checksum,parent_revision_id,"
                        + "compatibility,breaking_acknowledgement,approved_by,evidence_json) VALUES(?,?,?,?,?,?,?)",
                approval.revisionId(), approval.checksum(), approval.parentRevisionId(), approval.compatibility(),
                breakingAcknowledgement, approval.approvedBy(), evidenceJson);
    }

    public void publishApproved(long revisionId, String payloadJson) {
        controlPlane.update("INSERT INTO platform.outbox_event(aggregate_type,aggregate_id,event_type,payload_json,status)"
                + " VALUES('SITE_CONTRACT_REVISION',?,'SITE_CONTRACT_REVISION_APPROVED',?,'PENDING')", revisionId, payloadJson);
    }

    private int count(String sql, long revisionId) {
        Integer value = controlPlane.queryForObject(sql, Integer.class, revisionId);
        return value == null ? 0 : value;
    }
}
