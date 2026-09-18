package org.base.api.service.artifact;

import java.nio.charset.StandardCharsets;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/** Builds a canonical manifest from a package inventory. Pure function: no I/O, deterministic output. */
public final class ArtifactManifestGenerator {
    public static final String GENERATOR_VERSION = "artifact-manifest-generator/1";
    private static final Pattern WINDOWS_DRIVE_ROOT = Pattern.compile("^[A-Za-z]:/.*");

    /** Inventory line as produced by a package scan: original relative path, checksum and size. */
    public record InventoryEntry(String originalPath, String sha256, long byteSize) {}

    private ArtifactManifestGenerator() {}

    public static ArtifactManifest generate(String packageCode, String bucket, String objectPrefix, String sourceReference,
                                            List<InventoryEntry> inventory) {
        if (packageCode == null || !packageCode.matches("[A-Za-z0-9][A-Za-z0-9_.:-]{0,159}")) throw new IllegalArgumentException("Invalid packageCode");
        ArtifactKeys.requirePrefix(objectPrefix);
        if (inventory == null || inventory.isEmpty()) throw new IllegalArgumentException("Inventory must not be empty");
        Set<String> paths = new HashSet<>();
        List<ArtifactManifest.Entry> entries = new ArrayList<>(inventory.size());
        for (InventoryEntry item : inventory) {
            String path = normalizePath(item.originalPath());
            if (!paths.add(path)) throw new IllegalArgumentException("Duplicate original path in inventory: " + path);
            String sha = ArtifactKeys.requireSha256(item.sha256());
            if (item.byteSize() < 0) throw new IllegalArgumentException("Negative byte size: " + path);
            String extension = ArtifactKeys.extensionOf(path);
            entries.add(new ArtifactManifest.Entry(path, sha, item.byteSize(), MediaTypes.forFileName(path), bucket,
                    ArtifactKeys.contentKey(objectPrefix, sha, extension)));
        }
        entries.sort(Comparator.comparing(ArtifactManifest.Entry::originalPath));
        return new ArtifactManifest(ArtifactManifest.SCHEMA, packageCode, checksum(packageCode, entries), GENERATOR_VERSION, sourceReference, entries);
    }

    /** NFC relative path with forward slashes; absolute paths and traversal are rejected. */
    public static String normalizePath(String path) {
        if (path == null || path.isBlank()) throw new IllegalArgumentException("Original path is blank");
        String value = Normalizer.normalize(path.strip(), Normalizer.Form.NFC).replace('\\', '/');
        if (value.startsWith("/") || WINDOWS_DRIVE_ROOT.matcher(value).matches())
            throw new IllegalArgumentException("Original path must be relative: " + path);
        for (String segment : value.split("/", -1)) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) throw new IllegalArgumentException("Unsafe original path: " + path);
        }
        return value;
    }

    private static String checksum(String packageCode, List<ArtifactManifest.Entry> entries) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (DataOutputStream canonical = new DataOutputStream(new DigestOutputStream(OutputStream.nullOutputStream(), digest))) {
                writeString(canonical, ArtifactManifest.SCHEMA);
                writeString(canonical, packageCode);
                canonical.writeInt(entries.size());
                for (ArtifactManifest.Entry entry : entries) {
                    writeString(canonical, entry.originalPath());
                    writeString(canonical, entry.sha256());
                    canonical.writeLong(entry.byteSize());
                    writeString(canonical, entry.mediaType());
                    writeString(canonical, entry.bucket());
                    writeString(canonical, entry.objectKey());
                }
            }
            StringBuilder hex = new StringBuilder(64);
            for (byte b : digest.digest()) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        } catch (IOException impossible) {
            throw new IllegalStateException("Unable to encode canonical artifact manifest", impossible);
        }
    }

    private static void writeString(DataOutputStream out, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        out.writeInt(bytes.length);
        out.write(bytes);
    }
}
