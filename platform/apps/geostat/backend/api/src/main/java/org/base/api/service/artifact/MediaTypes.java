package org.base.api.service.artifact;

import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * File name to IANA media type through the standard registry shipped with Spring (Apache
 * {@code mime.types}). Types that registry lacks are data in {@value #SUPPLEMENT}, not code. Unknown
 * types resolve to octet-stream and are then rejected by any policy that does not explicitly allow them.
 * A media type is part of the manifest checksum, so an existing mapping must never change.
 */
public final class MediaTypes {
    public static final String OCTET_STREAM = MediaType.APPLICATION_OCTET_STREAM_VALUE;
    static final String SUPPLEMENT = "artifact-media-types.properties";
    private static final Map<String, String> SUPPLEMENTAL = load();

    private MediaTypes() {}

    public static String forFileName(String fileName) {
        if (fileName == null) return OCTET_STREAM;
        int dot = fileName.lastIndexOf('.');
        String supplemental = dot < 0 ? null : SUPPLEMENTAL.get(fileName.substring(dot + 1).toLowerCase(Locale.ROOT));
        if (supplemental != null) return supplemental;
        return MediaTypeFactory.getMediaType(fileName).map(MediaType::toString).orElse(OCTET_STREAM);
    }

    private static Map<String, String> load() {
        try (InputStream resource = MediaTypes.class.getClassLoader().getResourceAsStream(SUPPLEMENT)) {
            if (resource == null) return Map.of();
            Properties properties = new Properties();
            properties.load(resource);
            return properties.stringPropertyNames().stream().collect(Collectors.toUnmodifiableMap(
                    extension -> extension.toLowerCase(Locale.ROOT), extension -> MediaType.parseMediaType(properties.getProperty(extension).trim()).toString()));
        } catch (IOException unreadable) {
            throw new UncheckedIOException("Supplemental media type registry is unreadable", unreadable);
        }
    }
}
