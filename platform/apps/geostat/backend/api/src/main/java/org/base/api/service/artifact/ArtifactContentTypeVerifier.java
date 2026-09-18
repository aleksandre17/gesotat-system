package org.base.api.service.artifact;

import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

/**
 * Checks that bytes agree with the media type inferred from the package path. The path is used
 * only as a declared semantic hint; Tika receives the stream without a filename or caller MIME
 * metadata so an extension cannot make arbitrary bytes appear to be a trusted document type.
 */
@Component
public final class ArtifactContentTypeVerifier {
    private final Tika detector = new Tika();

    public String verify(String fileName, InputStream content) throws IOException {
        String declared = MediaTypes.forFileName(fileName).toLowerCase(Locale.ROOT);
        String detected = detector.detect(content).toLowerCase(Locale.ROOT);
        boolean compatible = declared.equals(detected)
                // Plain text detectors cannot distinguish CSV from other UTF text. The extension
                // declares CSV semantics; binary data still resolves to a non-text MIME type.
                || (declared.equals("text/csv") && detected.equals("text/plain"));
        if (!compatible) {
            throw new IllegalArgumentException("Package content does not match declared media type " + declared);
        }
        return declared;
    }
}
