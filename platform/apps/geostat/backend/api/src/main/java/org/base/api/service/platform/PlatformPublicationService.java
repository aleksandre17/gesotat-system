package org.base.api.service.platform;

import org.base.api.service.artifact.ArtifactReconciliationService;
import org.base.api.service.publication.gate.ReleaseGateService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Controlled release boundary. It publishes only a prepared snapshot and emits an archive/outbox event. */
@Service
public class PlatformPublicationService {
    private final JdbcTemplate dataPlane;
    private final JdbcTemplate controlPlane;
    private final DataPlanePublicationWriter dataPlaneWriter;
    private final PublicationControlStore controlStore;
    private final PlatformClassificationMirrorService classifications;
    private final ArtifactReconciliationService artifactGate;
    private final ReleaseGateService releaseGates;

    public PlatformPublicationService(@Qualifier("dataPlaneJdbcTemplate") JdbcTemplate dataPlane,
                                      @Qualifier("primaryJdbcTemplate") JdbcTemplate controlPlane,
                                      DataPlanePublicationWriter dataPlaneWriter, PublicationControlStore controlStore, PlatformClassificationMirrorService classifications,
                                      ArtifactReconciliationService artifactGate, ReleaseGateService releaseGates) {
        this.dataPlane = dataPlane;
        this.controlPlane = controlPlane;
        this.dataPlaneWriter = dataPlaneWriter;
        this.controlStore = controlStore;
        this.classifications = classifications;
        this.artifactGate = artifactGate;
        this.releaseGates = releaseGates;
    }

    public PublicationReceipt publish(PublishSnapshotRequest request) {
        validate(request);
        assertApprovedContract(request.productId());
        Long rowCount = dataPlane.query("SELECT row_count FROM publication.dataset_snapshot WHERE dataset_snapshot_id=? AND dataset_version_id=? AND status='REVIEW_REQUIRED'",
                rs -> rs.next() ? rs.getLong(1) : null, request.datasetSnapshotId(), request.datasetVersionId());
        if (rowCount == null) throw new IllegalStateException("Snapshot is not in REVIEW_REQUIRED state");
        releaseGates.requireReleasable(request.datasetSnapshotId());
        artifactGate.requirePassIfDeclared(request.datasetSnapshotId(), request.datasetVersionId());
        long releaseId = controlStore.createIntent(request);
        PublicationReceipt receipt = dataPlaneWriter.publish(request, releaseId, rowCount);
        classifications.mirror(receipt.publicationSnapshotId());
        controlStore.complete(releaseId);
        if (receipt.previousPublicationSnapshotId() > 0) controlStore.enqueueArchive(request.productId(), receipt.previousPublicationSnapshotId());
        return receipt;
    }

    private void assertApprovedContract(long productId) {
        Integer ok = controlPlane.query("SELECT COUNT(*) FROM platform.ingestion_contract c " +
                "JOIN platform.dataset d ON d.dataset_id=c.dataset_id " +
                "JOIN platform.site_contract_revision s ON s.product_id=d.product_id AND s.contract_code=c.contract_code AND s.revision=c.contract_revision " +
                "JOIN platform.ingestion_contract_revision r ON r.contract_id=c.contract_id AND r.revision=c.contract_revision " +
                "WHERE d.product_id=? AND c.status='ACTIVE' " +
                "AND s.status='APPROVED' AND r.lifecycle_status='APPROVED'", rs -> rs.next() ? rs.getInt(1) : 0, productId);
        if (ok == null || ok == 0) throw new IllegalStateException("Publication requires an ACTIVE ingestion contract and APPROVED site/ingestion revision");
    }

    public PublicationReceipt rollback(RollbackPublicationRequest request) {
        if (request.productId() <= 0 || request.targetPublicationSnapshotId() <= 0)
            throw new IllegalArgumentException("Rollback identifiers must be positive");
        PublicationReceipt receipt = dataPlaneWriter.rollback(request);
        return receipt;
    }

    private static void validate(PublishSnapshotRequest request) {
        if (request.productId() <= 0 || request.datasetVersionId() <= 0 || request.datasetSnapshotId() <= 0)
            throw new IllegalArgumentException("Publication identifiers must be positive");
        if (request.checksum() == null || !request.checksum().matches("[A-Fa-f0-9]{64}"))
            throw new IllegalArgumentException("checksum must be SHA-256 hex");
    }
}
