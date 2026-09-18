package org.base.api.service.artifact;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArtifactBindingPlannerTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String RULE = "{\"type\":\"SOURCE_PATH\",\"bindings\":[{\"language\":\"ka\",\"field\":\"p\"}]}";

    private static ArtifactRegistry.StoredEntry stored(long versionId, String path, VerificationStatus status) {
        return new ArtifactRegistry.StoredEntry(versionId, versionId, new ArtifactManifest.Entry(path, String.valueOf(versionId).repeat(64).substring(0, 64), 5,
                ArtifactMatcherTest.XLSX, "b", "k/" + versionId), status);
    }

    private static List<ArtifactMatcher.SourceRow> rows() {
        return List.of(new ArtifactMatcher.SourceRow(11, "r1", 101, JSON.createObjectNode().put("p", "a.xlsx")));
    }

    @Test
    void identicalRebindIsIdempotent() throws Exception {
        ArtifactBindingPlanner.Plan plan = ArtifactBindingPlanner.plan(List.of(ArtifactMatcherTest.definition(RULE)), rows(),
                List.of(stored(1, "a.xlsx", VerificationStatus.VERIFIED)), Map.of("PRIMARY_FILE", Map.of(ArtifactAttachmentRepository.slot(11, "ka", 1), 1L)));
        assertFalse(plan.blocked());
        assertTrue(plan.relations().get(0).edges().get(0).existing());
    }

    @Test
    void differentOrStaleStoredEdgesConflict() throws Exception {
        ArtifactBindingPlanner.Plan changed = ArtifactBindingPlanner.plan(List.of(ArtifactMatcherTest.definition(RULE)), rows(),
                List.of(stored(1, "a.xlsx", VerificationStatus.VERIFIED)), Map.of("PRIMARY_FILE", Map.of(ArtifactAttachmentRepository.slot(11, "ka", 1), 7L)));
        assertEquals(ArtifactIssue.Code.SLOT_CONFLICT, changed.issues().get(0).code());
        ArtifactBindingPlanner.Plan stale = ArtifactBindingPlanner.plan(List.of(ArtifactMatcherTest.definition(RULE)), rows(),
                List.of(stored(1, "a.xlsx", VerificationStatus.VERIFIED)), Map.of("PRIMARY_FILE", Map.of(
                        ArtifactAttachmentRepository.slot(11, "ka", 1), 1L, ArtifactAttachmentRepository.slot(12, "ka", 1), 1L)));
        assertTrue(stale.blocked());
    }

    @Test
    void unverifiedObjectBlocks() throws Exception {
        ArtifactBindingPlanner.Plan plan = ArtifactBindingPlanner.plan(List.of(ArtifactMatcherTest.definition(RULE)), rows(),
                List.of(stored(1, "a.xlsx", VerificationStatus.MISSING)), Map.of());
        assertTrue(plan.blocked());
        assertEquals(ArtifactIssue.Code.OBJECT_MISSING, plan.issues().get(0).code());
    }
}
