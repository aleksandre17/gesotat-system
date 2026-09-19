package org.base.api.service.artifact;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** The single SHA-256 implementation of the artifact line: lowercase hex, streaming, no provider lookups elsewhere. */
public final class Sha256 {
    private static final int BUFFER_BYTES = 64 * 1024;

    private Sha256() {}

    public static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    public static String hex(MessageDigest digest) {
        return HexFormat.of().formatHex(digest.digest());
    }

    public static String ofUtf8(String value) {
        return HexFormat.of().formatHex(newDigest().digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    public static String of(InputStream content) throws IOException {
        MessageDigest digest = newDigest();
        byte[] buffer = new byte[BUFFER_BYTES];
        for (int read; (read = content.read(buffer)) > 0; ) digest.update(buffer, 0, read);
        return hex(digest);
    }

    public static String of(Path file) throws IOException {
        try (InputStream content = Files.newInputStream(file)) {
            return of(content);
        }
    }
}
