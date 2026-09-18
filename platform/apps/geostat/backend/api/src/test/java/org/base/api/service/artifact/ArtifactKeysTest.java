package org.base.api.service.artifact;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ArtifactKeysTest {
    private static final String SHA = "ef3e90d0e211c4998ef0607132d40fd24302d5dede2ec1ef817ad181d39be8cb";

    @Test
    void contentKeyIsChecksumAddressed() {
        assertEquals("kids/r8/resources/kids-files-r8-sanitized/" + SHA + ".xlsx",
                ArtifactKeys.contentKey("kids/r8/resources/kids-files-r8-sanitized/", SHA, "xlsx"));
    }

    @Test
    void extensionIsLowercased() {
        assertEquals("xlsx", ArtifactKeys.extensionOf("goals/1.2.1_Absolute Poverty.XLSX"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"../etc/", "/abs/", "Upper/", "a/../b/", "noslash", "a//"})
    void unsafePrefixesAreRejected(String prefix) {
        assertThrows(IllegalArgumentException.class, () -> ArtifactKeys.requirePrefix(prefix));
    }

    @ParameterizedTest
    @ValueSource(strings = {"a/../b", "../x", "/x", "x/", "a/./b", "a b", "a\\b", ""})
    void unsafeKeysAreRejected(String key) {
        assertThrows(IllegalArgumentException.class, () -> ArtifactKeys.requireSafeKey(key));
    }

    @ParameterizedTest
    @ValueSource(strings = {"EF3E90D0E211C4998EF0607132D40FD24302D5DEDE2EC1EF817AD181D39BE8CB", "abc", ""})
    void checksumMustBeLowercaseSha256(String sha) {
        assertThrows(IllegalArgumentException.class, () -> ArtifactKeys.requireSha256(sha));
    }

    @Test
    void fileWithoutExtensionIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> ArtifactKeys.extensionOf("README"));
    }
}
