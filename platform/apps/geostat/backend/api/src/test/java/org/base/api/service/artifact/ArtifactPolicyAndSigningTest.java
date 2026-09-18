package org.base.api.service.artifact;

import org.base.api.service.storage.ObjectStorageService;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArtifactPolicyAndSigningTest {
    private static final String SHA = "ef3e90d0e211c4998ef0607132d40fd24302d5dede2ec1ef817ad181d39be8cb";
    private static final ArtifactObjectStore.ObjectLocation LOCATION = new ArtifactObjectStore.ObjectLocation("geostat-ingest", "kids/r8/resources/" + SHA + ".xlsx");

    private static ObjectStorageService storage(String publicEndpoint) {
        return new ObjectStorageService("http://127.0.0.1:1", "test-access", "test-secret", "geostat-ingest", "geostat-quarantine",
                "geostat-archive", "geostat-export", publicEndpoint, "us-east-1");
    }

    @Test
    void restrictedPolicyRequiresItsAuthority() {
        ArtifactPolicy restricted = new ArtifactPolicy("R", 1, ArtifactPolicy.AccessMode.RESTRICTED, "CONFIDENTIAL_READ", Set.of("x"), 1, Duration.ofSeconds(60), RetentionClass.ARCHIVE_7Y);
        assertFalse(restricted.permits(Set.of("READ_RESOURCE")));
        assertTrue(restricted.permits(Set.of("READ_RESOURCE", "CONFIDENTIAL_READ")));
        assertThrows(IllegalArgumentException.class, () -> new ArtifactPolicy("R", 1, ArtifactPolicy.AccessMode.RESTRICTED, null, Set.of(), 1, Duration.ofSeconds(60), RetentionClass.ARCHIVE_7Y));
        assertThrows(IllegalArgumentException.class, () -> new ArtifactPolicy("R", 1, ArtifactPolicy.AccessMode.AUTHENTICATED, null, Set.of(), 1, Duration.ofSeconds(7200), RetentionClass.ARCHIVE_7Y));
    }

    @Test
    void signedUrlTargetsPublicEndpointAndCarriesTtlAndFileName() {
        URI url = storage("https://files.geostat.internal").presignGet(LOCATION, Duration.ofSeconds(300), "1.2.1_Absolute Poverty.xlsx", ArtifactMatcherTest.XLSX);
        assertEquals("files.geostat.internal", url.getHost());
        String query = url.getRawQuery();
        assertTrue(query.contains("X-Amz-Expires=300"), query);
        assertTrue(query.contains("X-Amz-Signature="), query);
        assertTrue(query.contains("response-content-disposition="), query);
        assertFalse(query.contains("test-secret"), "secret must never appear in a URL");
    }

    @Test
    void distributionFailsClosedWithoutPublicEndpoint() {
        assertThrows(ArtifactStorageException.class, () -> storage("").presignGet(LOCATION, Duration.ofSeconds(300), "a.xlsx", ArtifactMatcherTest.XLSX));
    }
}
