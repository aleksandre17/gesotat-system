package org.base.api.service.artifact;

import java.nio.charset.StandardCharsets;
import java.io.DataOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Pure evaluation of the {@code ARTIFACT_RECONCILIATION} gate: declared cardinality per row and
 * language slot, object verification and policy conformance of every attachment, plus a canonical
 * checksum of the attachment set.
 */
public final class ArtifactReconciler {

    /** Attachment counts per external key for one relation and language slot. */
    public record SlotCounts(String relationCode, String language, boolean required, Map<String, Integer> perEntity) {}

    public record Result(GateResult result, String attachmentChecksum, Map<String, Map<String, Integer>> attachedBySlot, List<ArtifactIssue> issues) {}

    private ArtifactReconciler() {}

    public static Result evaluate(List<ArtifactRelationDefinition> definitions, List<SlotCounts> counts,
                                  List<ArtifactAttachmentRepository.AttachedObject> attachments) {
        Map<String, ArtifactRelationDefinition> byCode = new TreeMap<>();
        for (ArtifactRelationDefinition definition : definitions) byCode.put(definition.relationCode(), definition);
        List<ArtifactIssue> issues = new ArrayList<>();
        Map<String, Map<String, Integer>> attachedBySlot = new TreeMap<>();
        for (SlotCounts slot : counts) {
            ArtifactRelationDefinition definition = byCode.get(slot.relationCode());
            int attachedEntities = 0;
            for (Map.Entry<String, Integer> entity : new TreeMap<>(slot.perEntity()).entrySet()) {
                int n = entity.getValue();
                if (n > 0) attachedEntities++;
                String subject = entity.getKey() + "/" + slot.relationCode() + "/" + slot.language();
                if (slot.required() && n < definition.minPerRow())
                    issues.add(ArtifactIssue.error(ArtifactIssue.Code.CARDINALITY_VIOLATION, subject, n + " attachments, minimum " + definition.minPerRow()));
                if (definition.maxPerRow() != null && n > definition.maxPerRow())
                    issues.add(ArtifactIssue.error(ArtifactIssue.Code.CARDINALITY_VIOLATION, subject, n + " attachments, maximum " + definition.maxPerRow()));
            }
            attachedBySlot.computeIfAbsent(slot.relationCode(), k -> new TreeMap<>()).put(slot.language(), attachedEntities);
        }
        for (ArtifactAttachmentRepository.AttachedObject attached : attachments) {
            ArtifactRelationDefinition definition = byCode.get(attached.relationCode());
            if (definition == null) {
                issues.add(ArtifactIssue.error(ArtifactIssue.Code.SLOT_CONFLICT, attached.subject(), "Relation is not declared for this dataset version"));
                continue;
            }
            if (attached.verification().issue() != null)
                issues.add(ArtifactIssue.error(attached.verification().issue(), attached.subject(), attached.verification().name()));
            issues.addAll(definition.policy().evaluate(attached.subject(), attached.mediaType(), attached.byteSize()));
        }
        GateResult result = issues.stream().anyMatch(i -> i.severity() == ArtifactIssue.Severity.ERROR) ? GateResult.FAIL : GateResult.PASS;
        return new Result(result, checksum(attachments), attachedBySlot, List.copyOf(issues));
    }

    /**
     * SHA-256 over the complete API-visible attachment set. Length-prefixed UTF-8 values avoid
     * delimiter ambiguity; sorting makes the digest independent of repository/input order.
     */
    public static String checksum(List<ArtifactAttachmentRepository.AttachedObject> attachments) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (DataOutputStream out = new DataOutputStream(new DigestOutputStream(OutputStream.nullOutputStream(), digest))) {
                attachments.stream()
                        .sorted(java.util.Comparator.comparing(ArtifactAttachmentRepository.AttachedObject::externalKey)
                                .thenComparing(ArtifactAttachmentRepository.AttachedObject::relationCode)
                                .thenComparing(ArtifactAttachmentRepository.AttachedObject::language)
                                .thenComparingInt(ArtifactAttachmentRepository.AttachedObject::ordinal)
                                .thenComparing(ArtifactAttachmentRepository.AttachedObject::sha256)
                        .thenComparing(ArtifactAttachmentRepository.AttachedObject::originalName))
                        .forEach(a -> writeCanonical(out, a));
            }
            StringBuilder hex = new StringBuilder(64);
            for (byte b : digest.digest()) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        } catch (IOException impossible) {
            throw new IllegalStateException("Unable to encode artifact reconciliation checksum", impossible);
        }
    }

    private static void writeCanonical(DataOutputStream out, ArtifactAttachmentRepository.AttachedObject a) {
        try {
            writeString(out, a.externalKey());
            writeString(out, a.relationCode());
            writeString(out, a.role().name());
            writeString(out, a.language());
            out.writeInt(a.ordinal());
            writeString(out, a.originalName());
            writeString(out, a.mediaType());
            out.writeLong(a.byteSize());
            writeString(out, a.sha256());
            out.writeByte(a.verification().ordinal());
        } catch (IOException impossible) {
            throw new IllegalStateException("Unable to encode artifact reconciliation checksum", impossible);
        }
    }

    private static void writeString(DataOutputStream out, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        out.writeInt(bytes.length);
        out.write(bytes);
    }
}
