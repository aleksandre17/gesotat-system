package org.base.api.controller;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildInfoControllerTest {
    @Test
    void exposesOnlySafeBuildIdentityAndRedactsInvalidValues() {
        BuildInfoController controller = new BuildInfoController("abc123", "geostat-system", "2026-09-14T10:00:00Z");
        Map<String, String> body = controller.info().getBody();
        assertEquals("abc123", body.get("revision"));
        assertEquals("geostat-system", body.get("source"));
        assertEquals("2026-09-14T10:00:00Z", body.get("buildTimestamp"));
        assertEquals(3, body.size());

        Map<String, String> redacted = new BuildInfoController("secret value", "../etc", "<script>").info().getBody();
        assertTrue(redacted.values().stream().allMatch("unknown"::equals));
    }
}
