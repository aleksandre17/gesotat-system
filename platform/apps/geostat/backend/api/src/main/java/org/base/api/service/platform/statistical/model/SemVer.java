package org.base.api.service.platform.statistical.model;

/** Semantic Versioning 2.0.0 core triple; pre-release tags are not admitted for approved registry entries. */
public record SemVer(int major, int minor, int patch) implements Comparable<SemVer> {
    public SemVer {
        if (major < 0 || minor < 0 || patch < 0) throw new IllegalArgumentException("version parts must be non-negative");
    }

    public static SemVer parse(String value) {
        String[] parts = value == null ? new String[0] : value.split("\\.", -1);
        if (parts.length != 3) throw new IllegalArgumentException("version must be major.minor.patch: " + value);
        try {
            return new SemVer(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("version must be numeric: " + value);
        }
    }

    @Override public int compareTo(SemVer o) {
        int c = Integer.compare(major, o.major);
        if (c == 0) c = Integer.compare(minor, o.minor);
        return c != 0 ? c : Integer.compare(patch, o.patch);
    }

    @Override public String toString() { return major + "." + minor + "." + patch; }
}
