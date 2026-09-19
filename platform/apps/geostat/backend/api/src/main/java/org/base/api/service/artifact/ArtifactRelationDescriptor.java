package org.base.api.service.artifact;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.Set;

/**
 * Transportable form of one approved artifact relation declaration, exactly as the Control Plane stores
 * it. Both the API (serving) and offline package tooling (consuming) turn it into an
 * {@link ArtifactRelationDefinition} through {@link #toDefinition}, so there is one interpretation path.
 */
public record ArtifactRelationDescriptor(long datasetVersionId, String relationCode, String artifactRole, int minPerRow, Integer maxPerRow,
                                         boolean ordered, JsonNode matchRule, Policy policy) {

    public record Policy(String policyCode, int revision, String accessMode, String requiredAuthority, Set<String> allowedMediaTypes,
                         long maxBytes, long signedUrlTtlSeconds, String retentionClass) {
        public Policy {
            allowedMediaTypes = Set.copyOf(allowedMediaTypes);
        }

        ArtifactPolicy toPolicy() {
            return new ArtifactPolicy(policyCode, revision, ArtifactPolicy.AccessMode.valueOf(accessMode), requiredAuthority, allowedMediaTypes,
                    maxBytes, Duration.ofSeconds(signedUrlTtlSeconds), RetentionClass.valueOf(retentionClass));
        }
    }

    public ArtifactRelationDefinition toDefinition(ArtifactMatchRules rules) {
        return new ArtifactRelationDefinition(datasetVersionId, relationCode, ArtifactRole.valueOf(artifactRole), policy.toPolicy(),
                minPerRow, maxPerRow, ordered, rules.parse(matchRule));
    }
}
