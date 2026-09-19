package org.base.api.service.platform.statistical.model;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Exact, immutable registry reference: {@code kind:NAMESPACE:CODE(major.minor.patch)}.
 * Ranges, wildcards and {@code latest} are not part of the grammar, so they cannot resolve (register Q30).
 */
public record Ref(Kind kind, String namespace, String code, SemVer version) implements Comparable<Ref> {
    public static final String ID = "[A-Za-z][A-Za-z0-9_]{0,119}";
    private static final Pattern WIRE =
            Pattern.compile("([a-z]+):(" + ID + "):(" + ID + ")\\((\\d{1,9})\\.(\\d{1,9})\\.(\\d{1,9})\\)");

    /** Closed set of referenceable registry kinds. */
    public enum Kind {
        DSD, CONCEPT, MEASURE, UNIT, CODELIST, POLICY, PROFILE;

        public String wire() { return name().toLowerCase(Locale.ROOT); }

        public static Optional<Kind> fromWire(String value) {
            for (Kind kind : values()) if (kind.wire().equals(value)) return Optional.of(kind);
            return Optional.empty();
        }
    }

    public Ref {
        if (kind == null || namespace == null || code == null || version == null)
            throw new IllegalArgumentException("reference parts are required");
        if (!namespace.matches(ID) || !code.matches(ID))
            throw new IllegalArgumentException("reference identifier is not a valid ID");
    }

    public static Ref parse(String wire) {
        Matcher m = wire == null ? null : WIRE.matcher(wire);
        if (m == null || !m.matches()) throw new IllegalArgumentException("not an exact versioned reference: " + wire);
        Kind kind = Kind.fromWire(m.group(1))
                .orElseThrow(() -> new IllegalArgumentException("unknown reference kind: " + m.group(1)));
        return new Ref(kind, m.group(2), m.group(3),
                new SemVer(Integer.parseInt(m.group(4)), Integer.parseInt(m.group(5)), Integer.parseInt(m.group(6))));
    }

    public String wire() { return kind.wire() + ":" + namespace + ":" + code + "(" + version + ")"; }

    @Override public int compareTo(Ref other) { return wire().compareTo(other.wire()); }

    @Override public String toString() { return wire(); }
}
