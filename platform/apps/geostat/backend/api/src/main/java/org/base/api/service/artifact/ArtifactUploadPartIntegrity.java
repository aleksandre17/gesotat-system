package org.base.api.service.artifact;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;

/** Streaming integrity guard for durable staged upload parts during package assembly. */
final class ArtifactUploadPartIntegrity {
    private ArtifactUploadPartIntegrity() {}

    static InputStream verifyOnRead(InputStream source, String expectedSha256, long expectedSize) {
        ArtifactKeys.requireSha256(expectedSha256);
        if (source == null || expectedSize < 1) throw new IllegalArgumentException("Invalid staged upload checkpoint");
        final MessageDigest digest;
        try { digest = MessageDigest.getInstance("SHA-256"); }
        catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException("SHA-256 is unavailable", impossible); }
        return new FilterInputStream(source) {
            private long bytesRead;
            private boolean verified;

            @Override public int read() throws IOException {
                int value = super.read();
                if (value < 0) verify();
                else { digest.update((byte) value); count(1); }
                return value;
            }

            @Override public int read(byte[] buffer, int offset, int length) throws IOException {
                int read = super.read(buffer, offset, length);
                if (read < 0) verify();
                else if (read > 0) { digest.update(buffer, offset, read); count(read); }
                return read;
            }

            private void count(int amount) throws IOException {
                bytesRead += amount;
                if (bytesRead > expectedSize) throw new IOException("Staged upload part exceeds its checkpointed size");
            }

            private void verify() throws IOException {
                if (verified) return;
                if (bytesRead != expectedSize || !HexFormat.of().formatHex(digest.digest()).equals(expectedSha256))
                    throw new IOException("Staged upload part failed size or SHA-256 verification");
                verified = true;
            }
        };
    }
}
