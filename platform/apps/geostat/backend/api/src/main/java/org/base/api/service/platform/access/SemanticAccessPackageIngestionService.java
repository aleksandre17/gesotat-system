package org.base.api.service.platform.access;

import org.base.api.service.platform.PlatformAccessIngestionService;
import org.base.api.service.platform.PlatformPackageIngestReceipt;
import org.base.api.service.storage.StoredArtifact;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;

/** Validates the complete semantic package before one-artifact/multi-dataset staging begins. */
@Service
public class SemanticAccessPackageIngestionService {
    private final SemanticAccessPackageReader reader;
    private final SemanticAccessPackageValidationService validation;
    private final SemanticAccessControlPlaneResolver resolver;
    private final PlatformAccessIngestionService ingestion;
    private final AccessClassifierProposalImportService classifierProposals;

    public SemanticAccessPackageIngestionService(SemanticAccessPackageReader reader, SemanticAccessPackageValidationService validation,
                                                 SemanticAccessControlPlaneResolver resolver, PlatformAccessIngestionService ingestion,
                                                 AccessClassifierProposalImportService classifierProposals) {
        this.reader = reader; this.validation = validation; this.resolver = resolver; this.ingestion = ingestion; this.classifierProposals = classifierProposals;
    }

    public PlatformPackageIngestReceipt ingest(File artifactFile, StoredArtifact artifact) throws Exception {
        SemanticAccessPackage pack = reader.read(artifactFile);
        var issues = new ArrayList<>(validation.validate(artifactFile, pack).issues());
        issues.addAll(resolver.validate(pack));
        if (!issues.isEmpty()) throw new IllegalArgumentException("Semantic Access package failed preview: " + issues);
        PlatformPackageIngestReceipt receipt = ingestion.ingestPackage(artifactFile, pack.contractCode(), pack.contractRevision(), artifact);
        ingestion.materializeMetadata(pack);
        classifierProposals.importEvidence(artifactFile, pack, receipt.batchId());
        return receipt;
    }

    public PlatformPackageIngestReceipt resume(File artifactFile, long batchId) throws Exception {
        SemanticAccessPackage pack = reader.read(artifactFile);
        var issues = new ArrayList<>(validation.validate(artifactFile, pack).issues());
        issues.addAll(resolver.validate(pack));
        if (!issues.isEmpty()) throw new IllegalArgumentException("Stored semantic Access artifact no longer satisfies its contract: " + issues);
        return ingestion.resumePackage(artifactFile, batchId);
    }

    public String artifactUri(long batchId) { return ingestion.packageArtifactUri(batchId); }
}
