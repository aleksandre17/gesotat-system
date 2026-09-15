package org.base.api.controller;

import org.base.core.anotation.Api;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/** Authenticated, non-sensitive build identity used by release/provenance acceptance. */
@Api
@RestController
public final class BuildInfoController {
    private final String revision;
    private final String source;
    private final String buildTimestamp;

    public BuildInfoController(
            @Value("${build.revision:${BUILD_REVISION:unknown}}") String revision,
            @Value("${build.source:geostat-system}") String source,
            @Value("${build.timestamp:unknown}") String buildTimestamp) {
        this.revision = safe(revision);
        this.source = safe(source);
        this.buildTimestamp = safe(buildTimestamp);
    }

    @GetMapping("/build-info")
    public ResponseEntity<Map<String, String>> info() {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("revision", revision);
        body.put("source", source);
        body.put("buildTimestamp", buildTimestamp);
        return ResponseEntity.ok(body);
    }

    private static String safe(String value) {
        if (value == null || value.isBlank() || value.length() > 200 || !value.matches("[A-Za-z0-9._:+@-]+")) return "unknown";
        return value;
    }
}
