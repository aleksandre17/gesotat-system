package org.base.api.service.artifact;

import java.util.Locale;
import java.util.regex.Pattern;

/** Object-key rules: checksum-addressed, traversal-free and independent of source file names. */
public final class ArtifactKeys {
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");
    private static final Pattern PREFIX = Pattern.compile("([a-z0-9][a-z0-9_.-]*/)+");
    private static final Pattern EXTENSION = Pattern.compile("[a-z0-9]{1,10}");
    private static final Pattern KEY_SEGMENT = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_.-]*");

    private ArtifactKeys() {}

    public static String requireSha256(String value) {
        if (value == null || !SHA256.matcher(value).matches()) throw new IllegalArgumentException("sha256 must be 64 lowercase hex characters");
        return value;
    }

    public static String requirePrefix(String prefix) {
        if (prefix == null || !PREFIX.matcher(prefix).matches() || prefix.contains("..") || prefix.length() > 512)
            throw new IllegalArgumentException("Object prefix must be lowercase segments ending with '/'");
        return prefix;
    }

    public static String extensionOf(String fileName) {
        int dot = fileName == null ? -1 : fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) throw new IllegalArgumentException("File has no extension: " + fileName);
        String extension = fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!EXTENSION.matcher(extension).matches()) throw new IllegalArgumentException("Unsupported file extension: " + extension);
        return extension;
    }

    public static String contentKey(String prefix, String sha256, String extension) {
        requirePrefix(prefix);
        requireSha256(sha256);
        if (extension == null || !EXTENSION.matcher(extension).matches()) throw new IllegalArgumentException("Invalid extension");
        return prefix + sha256 + "." + extension;
    }

    public static String requireSafeKey(String key) {
        if (key == null || key.isEmpty() || key.length() > 1024 || key.startsWith("/") || key.endsWith("/"))
            throw new IllegalArgumentException("Invalid object key");
        for (String segment : key.split("/", -1)) {
            if (segment.equals(".") || segment.equals("..") || !KEY_SEGMENT.matcher(segment).matches())
                throw new IllegalArgumentException("Unsafe object key segment");
        }
        return key;
    }
}
