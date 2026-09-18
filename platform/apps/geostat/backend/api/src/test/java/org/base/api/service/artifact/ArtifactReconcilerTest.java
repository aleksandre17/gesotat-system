package org.base.api.service.artifact;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ArtifactReconcilerTest {
    private static final String XLSX = ArtifactMatcherTest.XLSX;

    private static ArtifactRelationDefinition definition() throws Exception {
        return ArtifactMatcherTest.definition("{\"type\":\"SOURCE_PATH\",\"bindings\":[{\"language\":\"ka\",\"field\":\"p\"}]}");
    }

    private static ArtifactAttachmentRepository.AttachedObject attached(String key, String sha, VerificationStatus status, String media, long bytes) {
        return new ArtifactAttachmentRepository.AttachedObject(key, "PRIMARY_FILE", ArtifactRole.PRIMARY, "ka", 1, key + ".xlsx", media, bytes, sha, "b", "k/" + sha, status);
    }

    @Test
    void completeVerifiedSetPasses() throws Exception {
        ArtifactReconciler.Result result = ArtifactReconciler.evaluate(List.of(definition()),
                List.of(new ArtifactReconciler.SlotCounts("PRIMARY_FILE", "ka", true, Map.of("a", 1, "b", 1))),
                List.of(attached("a", "1".repeat(64), VerificationStatus.VERIFIED, XLSX, 5), attached("b", "2".repeat(64), VerificationStatus.VERIFIED, XLSX, 5)));
        assertEquals(GateResult.PASS, result.result());
        assertEquals(Map.of("PRIMARY_FILE", Map.of("ka", 2)), result.attachedBySlot());
    }

    @Test
    void missingSlotUnverifiedObjectAndPolicyBreachFail() throws Exception {
        ArtifactReconciler.Result result = ArtifactReconciler.evaluate(List.of(definition()),
                List.of(new ArtifactReconciler.SlotCounts("PRIMARY_FILE", "ka", true, Map.of("a", 0, "b", 2, "c", 1))),
                List.of(attached("b", "2".repeat(64), VerificationStatus.CHECKSUM_MISMATCH, XLSX, 5), attached("c", "3".repeat(64), VerificationStatus.VERIFIED, "application/pdf", 5000)));
        assertEquals(GateResult.FAIL, result.result());
        assertEquals(Set.of(ArtifactIssue.Code.CARDINALITY_VIOLATION, ArtifactIssue.Code.CHECKSUM_MISMATCH, ArtifactIssue.Code.POLICY_MEDIA_TYPE, ArtifactIssue.Code.POLICY_MAX_BYTES),
                Set.copyOf(result.issues().stream().map(ArtifactIssue::code).toList()));
    }

    @Test
    void checksumTracksContentOfTheAttachmentSet() {
        List<ArtifactAttachmentRepository.AttachedObject> set = List.of(attached("a", "1".repeat(64), VerificationStatus.VERIFIED, XLSX, 5));
        assertEquals(ArtifactReconciler.checksum(set), ArtifactReconciler.checksum(List.copyOf(set)));
        assertNotEquals(ArtifactReconciler.checksum(set), ArtifactReconciler.checksum(List.of(attached("a", "9".repeat(64), VerificationStatus.VERIFIED, XLSX, 5))));
    }

    @Test
    void checksumCoversServedMetadataAndIsIndependentOfInputOrder() {
        var first = attached("a", "1".repeat(64), VerificationStatus.VERIFIED, XLSX, 5);
        var second = attached("b", "2".repeat(64), VerificationStatus.VERIFIED, XLSX, 5);
        assertEquals(ArtifactReconciler.checksum(List.of(first, second)), ArtifactReconciler.checksum(List.of(second, first)));
        assertNotEquals(ArtifactReconciler.checksum(List.of(first)), ArtifactReconciler.checksum(List.of(
                new ArtifactAttachmentRepository.AttachedObject("a", "PRIMARY_FILE", ArtifactRole.PRIMARY, "ka", 1,
                        "renamed.xlsx", XLSX, 5, "1".repeat(64), "b", "k/1", VerificationStatus.VERIFIED))));
        assertNotEquals(ArtifactReconciler.checksum(List.of(first)), ArtifactReconciler.checksum(List.of(
                new ArtifactAttachmentRepository.AttachedObject("a", "PRIMARY_FILE", ArtifactRole.PRIMARY, "ka", 1,
                        "a.xlsx", "application/pdf", 5, "1".repeat(64), "b", "k/1", VerificationStatus.VERIFIED))));
    }
}
