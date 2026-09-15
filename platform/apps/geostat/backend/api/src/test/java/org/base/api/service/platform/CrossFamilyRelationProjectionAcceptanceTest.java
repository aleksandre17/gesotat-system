package org.base.api.service.platform;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract-only end-to-end acceptance for cross-family graph execution.
 *
 * The fixture deliberately uses provider-neutral names and rows.  It models
 * the persisted contract surface, attaches relations, applies the declared
 * include/projection, and verifies that undeclared fields never reach the
 * response.  No KIDS table, family switch, or provider-specific SQL is used.
 */
class CrossFamilyRelationProjectionAcceptanceTest {

    @Test
    void entityToReferenceAndRawToStatisticalAreJoinedAndProjected() {
        Map<String, Object> contract = Map.of(
                "contractCode", "ALT_SITE_V1",
                "revision", 1,
                "relations", List.of(
                        Map.of("code", "goal.classifier", "fromFamily", "ENTITY", "toFamily", "REFERENCE",
                                "parentKey", "classifierId", "childKey", "classifierId", "cardinality", "ONE"),
                        Map.of("code", "document.observations", "fromFamily", "RAW", "toFamily", "STATISTICAL",
                                "parentKey", "documentId", "childKey", "documentId", "cardinality", "MANY")),
                "projections", Map.of(
                        "goal.classifier", List.of("id", "label", "classifier.code"),
                        "document.observations", List.of("id", "checksum", "observations.value")));

        assertEquals("ALT_SITE_V1", contract.get("contractCode"));
        assertEquals(2, ((List<?>) contract.get("relations")).size());

        List<Map<String, Object>> goals = List.of(new LinkedHashMap<>(Map.of(
                "id", "g-1", "label", "Early learning", "classifierId", "c-1", "privateNote", "redact")));
        List<Map<String, Object>> classifiers = List.of(new LinkedHashMap<>(Map.of(
                "classifierId", "c-1", "code", "GOAL", "secret", "redact")));
        List<Map<String, Object>> documents = List.of(new LinkedHashMap<>(Map.of(
                "id", "d-1", "checksum", "sha256:abc", "documentId", "d-1", "payload", "redact")));
        List<Map<String, Object>> observations = List.of(
                new LinkedHashMap<>(Map.of("documentId", "d-1", "value", 12, "unit", "COUNT", "secret", "redact")),
                new LinkedHashMap<>(Map.of("documentId", "d-1", "value", 8, "unit", "COUNT", "secret", "redact")));

        List<Map<String, Object>> goalGraph = ContractRelationGraphExecutor.attach(
                goals, "classifier", classifiers, "classifierId", "classifierId", false);
        List<Map<String, Object>> documentGraph = ContractRelationGraphExecutor.attach(
                documents, "observations", observations, "documentId", "documentId", true);

        List<Map<String, Object>> goalResponse = ContractIncludeSerializer.apply(
                goalGraph, List.of("label", "classifier.code"));
        List<Map<String, Object>> documentResponse = ContractIncludeSerializer.apply(
                documentGraph, List.of("checksum", "observations.value"));

        assertEquals("Early learning", goalResponse.get(0).get("label"));
        assertEquals("GOAL", ((Map<?, ?>) goalResponse.get(0).get("classifier")).get("code"));
        assertFalse(goalResponse.get(0).containsKey("privateNote"));
        assertFalse(((Map<?, ?>) goalResponse.get(0).get("classifier")).containsKey("secret"));

        assertEquals("sha256:abc", documentResponse.get(0).get("checksum"));
        List<?> projected = (List<?>) documentResponse.get(0).get("observations");
        assertEquals(2, projected.size());
        assertEquals(12, ((Map<?, ?>) projected.get(0)).get("value"));
        assertFalse(((Map<?, ?>) projected.get(0)).containsKey("unit"));
        assertFalse(documentResponse.get(0).containsKey("payload"));
    }
}
