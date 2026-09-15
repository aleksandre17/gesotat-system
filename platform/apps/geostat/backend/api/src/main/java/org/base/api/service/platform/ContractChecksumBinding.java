package org.base.api.service.platform;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Immutable contract-payload checksum binding; storage and transport agnostic. */
public final class ContractChecksumBinding {
    private ContractChecksumBinding() { }

    public static String sha256(String payload) {
        if (payload == null) throw new IllegalArgumentException("contract payload is required");
        try {
            var digest = MessageDigest.getInstance("SHA-256").digest(payload.getBytes(StandardCharsets.UTF_8));
            var out = new StringBuilder(64);
            for (byte b : digest) out.append(String.format("%02x", b));
            return out.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    public static boolean matches(String payload, String expectedHex) {
        if (expectedHex == null || !expectedHex.matches("(?i)[0-9a-f]{64}")) return false;
        return MessageDigest.isEqual(sha256(payload).getBytes(StandardCharsets.US_ASCII), expectedHex.toLowerCase().getBytes(StandardCharsets.US_ASCII));
    }
}
