package org.base.api.service.artifact;

/** Retention of distributed bytes; mirrors {@code ck_artifact_policy_retention}. See ADR-008 §7. */
public enum RetentionClass { RETAIN_INDEFINITE, RETAIN_WHILE_REFERENCED, ARCHIVE_1Y, ARCHIVE_7Y }
