package org.base.api.service.platform.statistical.model;

import java.util.List;

/**
 * One DSD component. A MEASURE carries only {@code measureRef}: its concept, representation and unit are
 * resolved from the measure registry and never serialised a second time (register Q32).
 */
public record Component(String code, Role role, Ref conceptRef, Representation representation,
                        Ref measureRef, Attachment attachment, boolean required) {

    public enum Role { DIMENSION, MEASURE, ATTRIBUTE }

    /** SDMX attribute relationship levels admitted by this profile (register Q21). */
    public enum AttachmentLevel {
        DATASET, DIMENSION_GROUP, OBSERVATION, MEASURE;

        /** Only values that vary per row become authoring columns; the rest is contract metadata. */
        public boolean variesPerRow() { return this == OBSERVATION || this == MEASURE; }
    }

    /** Tagged union: the level decides which target field is legal. */
    public record Attachment(AttachmentLevel level, List<String> dimensions, String measure) {
        public Attachment { dimensions = dimensions == null ? List.of() : List.copyOf(dimensions); }

        public static Attachment dataset() { return new Attachment(AttachmentLevel.DATASET, List.of(), null); }
        public static Attachment observation() { return new Attachment(AttachmentLevel.OBSERVATION, List.of(), null); }
        public static Attachment measure(String code) { return new Attachment(AttachmentLevel.MEASURE, List.of(), code); }
        public static Attachment group(List<String> dimensions) { return new Attachment(AttachmentLevel.DIMENSION_GROUP, dimensions, null); }
    }

    public static Component dimension(String code, Ref conceptRef, Representation representation) {
        return new Component(code, Role.DIMENSION, conceptRef, representation, null, null, true);
    }

    public static Component measure(String code, Ref measureRef, boolean required) {
        return new Component(code, Role.MEASURE, null, null, measureRef, null, required);
    }

    public static Component attribute(String code, Ref conceptRef, Representation representation,
                                      Attachment attachment, boolean required) {
        return new Component(code, Role.ATTRIBUTE, conceptRef, representation, null, attachment, required);
    }
}
