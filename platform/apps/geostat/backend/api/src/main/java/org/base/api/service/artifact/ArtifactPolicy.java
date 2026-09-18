package org.base.api.service.artifact;

import java.time.Duration;
import java.util.List;
import java.util.Set;

/** Approved control-plane policy for accepting and distributing artifact bytes. */
public record ArtifactPolicy(String policyCode, int revision, AccessMode accessMode, String requiredAuthority,
                             Set<String> allowedMediaTypes, long maxBytes, Duration signedUrlTtl, RetentionClass retentionClass) {

    public enum AccessMode { PUBLIC_WHEN_PUBLISHED, AUTHENTICATED, RESTRICTED }

    public ArtifactPolicy {
        allowedMediaTypes = Set.copyOf(allowedMediaTypes);
        if (accessMode == AccessMode.RESTRICTED && (requiredAuthority == null || requiredAuthority.isBlank()))
            throw new IllegalArgumentException("RESTRICTED policy requires an authority");
        if (maxBytes <= 0) throw new IllegalArgumentException("maxBytes must be positive");
        if (signedUrlTtl.toSeconds() < 30 || signedUrlTtl.toSeconds() > 3600) throw new IllegalArgumentException("Signed URL TTL must be 30..3600 seconds");
    }

    /** Policy findings for one content object; empty means acceptable. */
    public List<ArtifactIssue> evaluate(String subject, String mediaType, long byteSize) {
        List<ArtifactIssue> issues = new java.util.ArrayList<>(2);
        if (!allowedMediaTypes.contains(mediaType))
            issues.add(ArtifactIssue.error(ArtifactIssue.Code.POLICY_MEDIA_TYPE, subject, "Media type " + mediaType + " is not allowed by " + policyCode + " r" + revision));
        if (byteSize > maxBytes)
            issues.add(ArtifactIssue.error(ArtifactIssue.Code.POLICY_MAX_BYTES, subject, byteSize + " bytes exceeds " + maxBytes));
        return issues;
    }

    /** Whether a caller holding {@code authorities} may receive a download for a published attachment. */
    public boolean permits(Set<String> authorities) {
        return switch (accessMode) {
            case PUBLIC_WHEN_PUBLISHED, AUTHENTICATED -> true;
            case RESTRICTED -> authorities.contains(requiredAuthority);
        };
    }
}
