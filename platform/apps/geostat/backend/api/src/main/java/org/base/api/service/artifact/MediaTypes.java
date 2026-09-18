package org.base.api.service.artifact;

import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;

/**
 * File name to IANA media type through the standard registry shipped with Spring (Apache
 * {@code mime.types}); no local table to maintain. Unknown types resolve to octet-stream and are
 * then rejected by any policy that does not explicitly allow them.
 */
public final class MediaTypes {
    public static final String OCTET_STREAM = MediaType.APPLICATION_OCTET_STREAM_VALUE;

    private MediaTypes() {}

    public static String forFileName(String fileName) {
        return MediaTypeFactory.getMediaType(fileName).map(MediaType::toString).orElse(OCTET_STREAM);
    }
}
