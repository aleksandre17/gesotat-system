package org.base.api.service.platform;

import java.util.*;

/** Canonical, provider-neutral export format registry. Unknown formats fail closed. */
public final class ExportFormatRegistry {
    public record Format(String code, String mediaType, String extension, boolean streaming) { }
    private static final Map<String, Format> FORMATS = Map.of(
            "JSON", new Format("JSON", "application/json", "json", false),
            "CSV", new Format("CSV", "text/csv", "csv", false),
            "NDJSON", new Format("NDJSON", "application/x-ndjson", "ndjson", true));
    private ExportFormatRegistry() { }
    public static Format require(String requested) {
        String code = requested == null || requested.isBlank() ? "JSON" : requested.trim().toUpperCase(Locale.ROOT);
        Format format = FORMATS.get(code);
        if (format == null) throw new IllegalArgumentException("Unsupported export format: " + requested);
        return format;
    }
    public static Set<String> supportedCodes() { return FORMATS.keySet(); }
}
