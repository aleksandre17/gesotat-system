package org.base.api.service.artifact;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ArtifactUploadPartIntegrityTest {
    private static final String ABC_SHA256 = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";

    @Test
    void verifiesTheCheckpointedBytesWhileStreaming() throws Exception {
        try (var input = ArtifactUploadPartIntegrity.verifyOnRead(new ByteArrayInputStream("abc".getBytes(StandardCharsets.UTF_8)), ABC_SHA256, 3)) {
            assertArrayEquals("abc".getBytes(StandardCharsets.UTF_8), input.readAllBytes());
        }
    }

    @Test
    void rejectsChangedOrShortenedStagedBytesAtEndOfStream() {
        var wrongDigest = ArtifactUploadPartIntegrity.verifyOnRead(new ByteArrayInputStream("abd".getBytes(StandardCharsets.UTF_8)), ABC_SHA256, 3);
        assertThrows(IOException.class, wrongDigest::readAllBytes);
        var wrongSize = ArtifactUploadPartIntegrity.verifyOnRead(new ByteArrayInputStream("ab".getBytes(StandardCharsets.UTF_8)), ABC_SHA256, 3);
        assertThrows(IOException.class, wrongSize::readAllBytes);
    }

    @Test
    void rejectsAStagedObjectLongerThanItsCheckpoint() {
        var input = ArtifactUploadPartIntegrity.verifyOnRead(new ByteArrayInputStream("abcd".getBytes(StandardCharsets.UTF_8)), ABC_SHA256, 3);
        assertThrows(IOException.class, input::readAllBytes);
    }
}
