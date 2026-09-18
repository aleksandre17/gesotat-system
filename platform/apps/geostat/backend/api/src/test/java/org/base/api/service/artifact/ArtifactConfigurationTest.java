package org.base.api.service.artifact;

import org.junit.jupiter.api.Test;

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
    }
}
