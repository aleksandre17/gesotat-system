package org.base.api.service.contract.approval;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.base.api.service.artifact.GateResult;
import org.base.api.service.platform.ContractCompatibilityService;
import org.base.api.service.publication.gate.GateEvaluation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Governed approval of a site contract revision (completion plan C-01). One transaction evaluates every
 * {@link ContractApprovalCheck} over measured facts, supersedes the currently approved revision, approves
 * the target and appends checksum-bound evidence. Any failing check blocks the whole operation.
 */
@Service
public class SiteContractRevisionApprovalService {
    public static final String EVIDENCE_SCHEMA = "geostat.contract-approval.v1";
    private static final String APPROVED = "APPROVED";
    private static final Logger log = LoggerFactory.getLogger(SiteContractRevisionApprovalService.class);

    private final List<ContractApprovalCheck> checks;
    private final SiteContractRevisionRepository revisions;
    private final ContractCompatibilityService compatibility;
    private final ObjectMapper json;

    /** {@code created=false} marks an idempotent replay of an approval that already exists. */
    public record ApprovalReceipt(String contractCode, int revision, String checksum, String compatibility,
                                  Integer supersededRevision, String approvedBy, boolean created, List<GateEvaluation> checks) {}

    /** Outcome of the checks without any write. */
    public record ApprovalPreview(String contractCode, int revision, String compatibility, Integer currentApprovedRevision,
                                  boolean approvable, List<GateEvaluation> checks) {}

    public SiteContractRevisionApprovalService(List<ContractApprovalCheck> checks, SiteContractRevisionRepository revisions,
                                               ContractCompatibilityService compatibility, ObjectMapper json) {
        this.checks = checks.stream().sorted(Comparator.comparing(ContractApprovalCheck::code)).toList();
        this.revisions = revisions;
        this.compatibility = compatibility;
        this.json = json;
    }

    @Transactional(transactionManager = "primaryJdbcTransactionManager", readOnly = true)
    public ApprovalPreview preview(String contractCode, int revision, String breakingAcknowledgement) {
        List<SiteContractRevisionRepository.Revision> all = revisions.revisions(contractCode);
        RevisionFacts facts = measure(all, target(all, contractCode, revision), breakingAcknowledgement);
        List<GateEvaluation> evaluated = evaluate(facts);
        return new ApprovalPreview(contractCode, revision, facts.compatibility(), facts.currentApprovedRevision(), passes(evaluated), evaluated);
    }

    @Transactional(transactionManager = "primaryJdbcTransactionManager")
    public ApprovalReceipt approve(String contractCode, int revision, String breakingAcknowledgement, String approvedBy) {
        if (approvedBy == null || approvedBy.isBlank()) throw new IllegalArgumentException("Approver identity is required");
        List<SiteContractRevisionRepository.Revision> all = revisions.lockRevisions(contractCode);
        SiteContractRevisionRepository.Revision target = target(all, contractCode, revision);

        if (APPROVED.equals(target.status())) {
            // Replay: the same approved checksum returns its receipt; anything else is a conflict.
            var existing = revisions.approval(target.revisionId())
                    .filter(approval -> approval.checksum().equalsIgnoreCase(target.checksum()))
                    .orElseThrow(() -> new IllegalStateException("Revision is already approved outside the governed workflow"));
            return new ApprovalReceipt(contractCode, revision, target.checksum(), existing.compatibility(), null, existing.approvedBy(), false, List.of());
        }

        RevisionFacts facts = measure(all, target, breakingAcknowledgement);
        List<GateEvaluation> evaluated = evaluate(facts);
        if (!passes(evaluated)) throw new ContractApprovalBlockedException(evaluated);

        SiteContractRevisionRepository.Revision current = currentApproved(all);
        // Supersede first: the Control Plane allows a single APPROVED revision per contract.
        all.stream().filter(candidate -> APPROVED.equals(candidate.status())).forEach(candidate -> revisions.supersede(candidate.revisionId()));
        if (!revisions.approve(target.revisionId())) throw new IllegalStateException("Revision state changed during approval");
        var approval = new SiteContractRevisionRepository.Approval(target.revisionId(), target.checksum(),
                current == null ? null : current.revisionId(), facts.compatibility(), approvedBy);
        revisions.recordApproval(approval, facts.breaking() ? breakingAcknowledgement : null, evidence(facts, evaluated, approvedBy));
        revisions.publishApproved(target.revisionId(), write(Map.of("contractCode", contractCode, "revision", revision, "checksum", target.checksum())));
        log.info("contract.revision approved code={} revision={} compatibility={} superseded={} by={}", contractCode, revision,
                facts.compatibility(), facts.currentApprovedRevision(), approvedBy);
        return new ApprovalReceipt(contractCode, revision, target.checksum(), facts.compatibility(), facts.currentApprovedRevision(), approvedBy, true, evaluated);
    }

    private RevisionFacts measure(List<SiteContractRevisionRepository.Revision> all, SiteContractRevisionRepository.Revision target, String acknowledgement) {
        SiteContractRevisionRepository.Revision current = currentApproved(all);
        List<String> breaking = List.of();
        if (current != null && current.revisionId() != target.revisionId()) {
            Object reported = compatibility.compare(target.contractCode(), current.revision(), target.revision()).get("breakingChanges");
            breaking = reported instanceof List<?> list ? list.stream().map(String::valueOf).toList() : List.of();
        }
        return new RevisionFacts(target.revisionId(), target.contractCode(), target.revision(), target.status(), target.compatibilityMode(),
                target.checksum(), target.checksumMatchesDocument(), revisions.datasetCount(target.revisionId()),
                revisions.foreignProductDatasetCount(target.revisionId()), current == null ? null : current.revision(), breaking, acknowledgement);
    }

    private List<GateEvaluation> evaluate(RevisionFacts facts) {
        return checks.stream().map(check -> check.evaluate(facts)).toList();
    }

    private static boolean passes(List<GateEvaluation> evaluated) {
        return evaluated.stream().noneMatch(check -> check.result() == GateResult.FAIL);
    }

    private static SiteContractRevisionRepository.Revision target(List<SiteContractRevisionRepository.Revision> all, String contractCode, int revision) {
        return all.stream().filter(candidate -> candidate.revision() == revision).findFirst()
                .orElseThrow(() -> new NoSuchElementException("Contract revision " + contractCode + "/" + revision + " not found"));
    }

    private static SiteContractRevisionRepository.Revision currentApproved(List<SiteContractRevisionRepository.Revision> all) {
        return all.stream().filter(candidate -> APPROVED.equals(candidate.status()))
                .max(Comparator.comparingInt(SiteContractRevisionRepository.Revision::revision)).orElse(null);
    }

    private String evidence(RevisionFacts facts, List<GateEvaluation> evaluated, String approvedBy) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("schema", EVIDENCE_SCHEMA);
        evidence.put("contractCode", facts.contractCode());
        evidence.put("revision", facts.revision());
        evidence.put("checksum", facts.checksum());
        evidence.put("compatibility", facts.compatibility());
        evidence.put("comparedWithRevision", facts.currentApprovedRevision());
        evidence.put("breakingChanges", facts.breakingChanges());
        evidence.put("approvedBy", approvedBy);
        evidence.put("checks", evaluated);
        return write(evidence);
    }

    private String write(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException impossible) {
            throw new IllegalStateException("Approval evidence could not be serialized", impossible);
        }
    }
}
