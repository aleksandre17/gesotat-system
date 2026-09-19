package org.base.api.service.contract.approval;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.base.api.service.artifact.GateResult;
import org.base.api.service.contract.approval.SiteContractRevisionRepository.Approval;
import org.base.api.service.contract.approval.SiteContractRevisionRepository.Revision;
import org.base.api.service.platform.ContractCompatibilityService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SiteContractRevisionApprovalServiceTest {
    private static final String CODE = "GENERIC_SITE";
    private static final String SUM = "a".repeat(64);

    private final SiteContractRevisionRepository revisions = mock(SiteContractRevisionRepository.class);
    private final ContractCompatibilityService compatibility = mock(ContractCompatibilityService.class);
    private final SiteContractRevisionApprovalService service = new SiteContractRevisionApprovalService(
            List.of(new LifecycleTransitionCheck(), new ChecksumIntegrityCheck(), new StructuralBindingCheck(), new BreakingChangeAcknowledgementCheck()),
            revisions, compatibility, new ObjectMapper());

    @Test
    void firstApprovalNeedsNoComparisonAndRecordsChecksumBoundEvidence() {
        given(revision(10, 1, "REVIEW_REQUIRED", "BACKWARD_COMPATIBLE", true));

        var receipt = service.approve(CODE, 1, null, "steward");

        assertTrue(receipt.created());
        assertEquals("INITIAL", receipt.compatibility());
        assertNull(receipt.supersededRevision());
        verify(compatibility, never()).compare(anyString(), any(Integer.class), any(Integer.class));
        ArgumentCaptor<String> evidence = ArgumentCaptor.forClass(String.class);
        verify(revisions).recordApproval(eq(new Approval(10, SUM, null, "INITIAL", "steward")), isNull(), evidence.capture());
        assertTrue(evidence.getValue().contains("\"schema\":\"" + SiteContractRevisionApprovalService.EVIDENCE_SCHEMA + "\""));
        assertTrue(evidence.getValue().contains("\"checksum\":\"" + SUM + "\""));
        verify(revisions).publishApproved(eq(10L), anyString());
    }

    @Test
    void compatibleRevisionSupersedesTheCurrentOneBeforeBecomingApproved() {
        given(revision(10, 1, "APPROVED", "BACKWARD_COMPATIBLE", true), revision(11, 2, "REVIEW_REQUIRED", "BACKWARD_COMPATIBLE", true));
        when(compatibility.compare(CODE, 1, 2)).thenReturn(Map.of("breakingChanges", List.of()));

        var receipt = service.approve(CODE, 2, null, "steward");

        assertEquals("BACKWARD_COMPATIBLE", receipt.compatibility());
        assertEquals(1, receipt.supersededRevision());
        InOrder order = inOrder(revisions);
        order.verify(revisions).supersede(10);
        order.verify(revisions).approve(11);
        order.verify(revisions).recordApproval(eq(new Approval(11, SUM, 10L, "BACKWARD_COMPATIBLE", "steward")), isNull(), anyString());
    }

    @Test
    void undeclaredOrUnacknowledgedBreakingChangeBlocksWithoutAnyWrite() {
        given(revision(10, 1, "APPROVED", "BACKWARD_COMPATIBLE", true), revision(11, 2, "REVIEW_REQUIRED", "BACKWARD_COMPATIBLE", true));
        when(compatibility.compare(CODE, 1, 2)).thenReturn(Map.of("breakingChanges", List.of("REMOVED_FIELD:ITEM.id")));

        var blocked = assertThrows(ContractApprovalBlockedException.class, () -> service.approve(CODE, 2, "accepted by consumers", "steward"));

        var check = blocked.checks().stream().filter(c -> c.gateCode().equals("BREAKING_CHANGE_ACKNOWLEDGEMENT")).findFirst().orElseThrow();
        assertEquals(GateResult.FAIL, check.result());
        assertTrue(check.findings().contains("REMOVED_FIELD:ITEM.id"));
        verifyNothingWritten();
    }

    @Test
    void declaredAndAcknowledgedBreakingChangeIsApprovedWithItsReason() {
        given(revision(10, 1, "APPROVED", "BACKWARD_COMPATIBLE", true), revision(11, 2, "REVIEW_REQUIRED", "BREAKING_NEW_REVISION", true));
        when(compatibility.compare(CODE, 1, 2)).thenReturn(Map.of("breakingChanges", List.of("REMOVED_FIELD:ITEM.id")));

        assertThrows(ContractApprovalBlockedException.class, () -> service.approve(CODE, 2, " ", "steward"));
        var receipt = service.approve(CODE, 2, "consumers migrated", "steward");

        assertEquals("BREAKING", receipt.compatibility());
        verify(revisions).recordApproval(eq(new Approval(11, SUM, 10L, "BREAKING", "steward")), eq("consumers migrated"), anyString());
    }

    @Test
    void tamperedDocumentEmptyStructureAndWrongLifecycleStateEachBlock() {
        given(revision(10, 1, "REVIEW_REQUIRED", "BACKWARD_COMPATIBLE", false));
        assertFailed("CHECKSUM_INTEGRITY", () -> service.approve(CODE, 1, null, "steward"));

        given(revision(10, 1, "REVIEW_REQUIRED", "BACKWARD_COMPATIBLE", true));
        when(revisions.datasetCount(10)).thenReturn(0);
        assertFailed("STRUCTURAL_BINDING", () -> service.approve(CODE, 1, null, "steward"));

        when(revisions.datasetCount(10)).thenReturn(2);
        when(revisions.foreignProductDatasetCount(10)).thenReturn(1);
        assertFailed("STRUCTURAL_BINDING", () -> service.approve(CODE, 1, null, "steward"));

        for (String status : List.of("DRAFT", "SUPERSEDED", "UNKNOWN")) {
            given(revision(10, 1, status, "BACKWARD_COMPATIBLE", true));
            assertFailed("LIFECYCLE_TRANSITION", () -> service.approve(CODE, 1, null, "steward"));
        }
        verifyNothingWritten();
    }

    @Test
    void replayReturnsTheExistingApprovalAndUngovernedApprovalIsAConflict() {
        given(revision(10, 1, "APPROVED", "BACKWARD_COMPATIBLE", true));
        when(revisions.approval(10)).thenReturn(Optional.of(new Approval(10, SUM, null, "INITIAL", "steward")));

        var replay = service.approve(CODE, 1, null, "someone-else");
        assertFalse(replay.created());
        assertEquals("steward", replay.approvedBy());

        when(revisions.approval(10)).thenReturn(Optional.empty());
        assertThrows(IllegalStateException.class, () -> service.approve(CODE, 1, null, "steward"));
        when(revisions.approval(10)).thenReturn(Optional.of(new Approval(10, "b".repeat(64), null, "INITIAL", "steward")));
        assertThrows(IllegalStateException.class, () -> service.approve(CODE, 1, null, "steward"));
        verifyNothingWritten();
    }

    @Test
    void lostRaceMissingRevisionAndMissingApproverFailClosed() {
        given(revision(10, 1, "REVIEW_REQUIRED", "BACKWARD_COMPATIBLE", true));
        when(revisions.approve(10)).thenReturn(false);
        assertThrows(IllegalStateException.class, () -> service.approve(CODE, 1, null, "steward"));
        verify(revisions, never()).recordApproval(any(), any(), anyString());

        assertThrows(NoSuchElementException.class, () -> service.approve(CODE, 9, null, "steward"));
        assertThrows(IllegalArgumentException.class, () -> service.approve(CODE, 1, null, " "));
    }

    @Test
    void previewEvaluatesWithoutLockingOrWriting() {
        when(revisions.revisions(CODE)).thenReturn(List.of(revision(10, 1, "APPROVED", "BACKWARD_COMPATIBLE", true),
                revision(11, 2, "REVIEW_REQUIRED", "BACKWARD_COMPATIBLE", true)));
        when(revisions.datasetCount(anyLong())).thenReturn(1);
        when(compatibility.compare(CODE, 1, 2)).thenReturn(Map.of("breakingChanges", List.of("REMOVED_DATASET:ITEM")));

        var preview = service.preview(CODE, 2, null);

        assertFalse(preview.approvable());
        assertEquals("BREAKING", preview.compatibility());
        assertEquals(1, preview.currentApprovedRevision());
        verify(revisions, never()).lockRevisions(anyString());
        verifyNothingWritten();
    }

    private void given(Revision... all) {
        when(revisions.lockRevisions(CODE)).thenReturn(List.of(all));
        when(revisions.datasetCount(anyLong())).thenReturn(1);
        when(revisions.foreignProductDatasetCount(anyLong())).thenReturn(0);
        when(revisions.approve(anyLong())).thenReturn(true);
    }

    private static Revision revision(long id, int revision, String status, String mode, boolean checksumMatches) {
        return new Revision(id, 1, CODE, revision, status, mode, SUM, checksumMatches);
    }

    private static void assertFailed(String checkCode, org.junit.jupiter.api.function.Executable approval) {
        var blocked = assertThrows(ContractApprovalBlockedException.class, approval);
        assertTrue(blocked.checks().stream().anyMatch(c -> c.gateCode().equals(checkCode) && c.result() == GateResult.FAIL), checkCode);
    }

    private void verifyNothingWritten() {
        verify(revisions, never()).approve(anyLong());
        verify(revisions, never()).supersede(anyLong());
        verify(revisions, never()).recordApproval(any(), any(), anyString());
        verify(revisions, never()).publishApproved(anyLong(), anyString());
    }
}
