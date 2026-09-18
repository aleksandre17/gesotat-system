package org.base.api.service.artifact;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.util.unit.DataSize;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ArtifactConfigurationTest {
    @Test
    void mediaTypesComeFromTheStandardRegistry() {
        assertEquals(ArtifactMatcherTest.XLSX, MediaTypes.forFileName("a.xlsx"));
        assertEquals("application/vnd.ms-excel", MediaTypes.forFileName("a.XLS"));
        assertEquals(MediaTypes.OCTET_STREAM, MediaTypes.forFileName("a.unknownext"));
    }

    @Test
    void invalidBoundsFailFast() {
        ArtifactProperties properties = new ArtifactProperties();
        properties.validate();
        properties.setUploadPrefix("../outside/");
        assertThrows(IllegalArgumentException.class, properties::validate);
        ArtifactProperties bounds = new ArtifactProperties();
        bounds.setMaxPackageBytes(1);
        assertThrows(IllegalStateException.class, bounds::validate);
        ArtifactProperties upload = new ArtifactProperties();
        upload.setMaxUploadBytes(0);
        assertThrows(IllegalStateException.class, upload::validate);
        ArtifactProperties sessions = new ArtifactProperties();
        sessions.setMaxTenantActiveUploadSessions(0);
        assertThrows(IllegalStateException.class, sessions::validate);
        ArtifactProperties lease = new ArtifactProperties();
        lease.setUploadProcessingLeaseSeconds(0);
        assertThrows(IllegalStateException.class, lease::validate);
        ArtifactProperties unrepresentable = new ArtifactProperties();
        unrepresentable.setUploadPartBytes(1);
        unrepresentable.setMaxUploadBytes((long) Integer.MAX_VALUE + 1);
        assertThrows(IllegalStateException.class, unrepresentable::validate);
        ArtifactProperties audit = new ArtifactProperties();
        audit.setIntegrityAuditMaxBytesPerRun(1);
        assertThrows(IllegalStateException.class, audit::validate);
    }

    @Test
    void multipartFileBoundMustMatchAndRequestMustAllowBoundaryOverhead() {
        ArtifactProperties artifacts = new ArtifactProperties();
        MultipartProperties multipart = new MultipartProperties();
        multipart.setMaxFileSize(DataSize.ofBytes(artifacts.getMaxUploadBytes()));
        multipart.setMaxRequestSize(DataSize.ofBytes(artifacts.getMaxUploadBytes() + 1024));
        artifacts.setMultipartProperties(multipart);
        artifacts.validate();

        multipart.setMaxFileSize(DataSize.ofBytes(artifacts.getMaxUploadBytes() - 1));
        assertThrows(IllegalStateException.class, artifacts::validate);
        multipart.setMaxFileSize(DataSize.ofBytes(artifacts.getMaxUploadBytes()));
        multipart.setMaxRequestSize(DataSize.ofBytes(artifacts.getMaxUploadBytes() - 1));
        assertThrows(IllegalStateException.class, artifacts::validate);
    }
}
