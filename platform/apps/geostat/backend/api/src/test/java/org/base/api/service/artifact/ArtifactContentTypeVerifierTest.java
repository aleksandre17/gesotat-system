package org.base.api.service.artifact;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ArtifactContentTypeVerifierTest {
    private final ArtifactContentTypeVerifier verifier = new ArtifactContentTypeVerifier();

    @Test
    void detectsPdfFromBytesWithoutTrustingExtension() throws Exception {
        byte[] pdf = "%PDF-1.7\n%âãÏÓ\n".getBytes(StandardCharsets.ISO_8859_1);
        assertEquals("application/pdf", verifier.verify("report.pdf", new ByteArrayInputStream(pdf)));
    }

    @Test
    void rejectsTextDisguisedAsPdf() {
        assertThrows(IllegalArgumentException.class, () -> verifier.verify("report.pdf",
                new ByteArrayInputStream("plain text".getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    void acceptsUtfTextAsCsvBecauseMimeDetectorsClassifyDelimitedTextAsPlainText() throws Exception {
        assertEquals("text/csv", verifier.verify("data.csv",
                new ByteArrayInputStream("id,value\n1,2\n".getBytes(StandardCharsets.UTF_8))));
    }
}
