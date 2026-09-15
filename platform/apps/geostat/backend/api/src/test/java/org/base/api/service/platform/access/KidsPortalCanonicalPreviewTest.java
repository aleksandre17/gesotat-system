package org.base.api.service.platform.access;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Preview exercises the actual Access artifact plus the exact Control-Plane dataset bindings. */
class KidsPortalCanonicalPreviewTest {
    @Test
    void canonicalArtifactPassesEndToEndPreviewAgainstItsIssuedBindings() throws Exception {
        var artifact = locateArtifact("kids-portal-v1-canonical-r8-final.accdb");
        var reader = new SemanticAccessPackageReader();
        var pack = reader.read(artifact.toFile());
        JdbcTemplate control = mock(JdbcTemplate.class);
        when(control.queryForList(contains("WHERE c.contract_code"), org.mockito.ArgumentMatchers.<Object[]>any())).thenReturn(List.of(Map.of("contract_id", 77L, "contract_revision", 8, "status", "APPROVED", "product_code", "KIDS_PORTAL")));
        List<Map<String, Object>> sources = pack.datasets().stream().map(d -> Map.<String,Object>of("dataset_code", d.datasetCode(), "source_locator", "ACCESS." + d.accessTableName())).toList();
        when(control.queryForList(contains("FROM platform.contract_source"), org.mockito.ArgumentMatchers.<Object[]>any())).thenReturn(sources);
        var service = new SemanticAccessPreviewService(reader, new SemanticAccessPackageValidationService(new ObjectMapper()), new SemanticAccessControlPlaneResolver(control));
        var preview = service.preview(new MockMultipartFile("file", artifact.getFileName().toString(), "application/octet-stream", Files.readAllBytes(artifact)));
        assertTrue(preview.valid(), () -> "Preview issues: " + preview.issues());
    }

    private static Path locateArtifact(String name) {
        Path directory = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (directory != null) {
            for (Path candidate : new Path[]{directory.resolve(name), directory.resolve("samples").resolve(name), directory.resolve("api").resolve(name)}) {
                if (Files.isRegularFile(candidate)) return candidate;
            }
            directory = directory.getParent();
        }
        return Path.of("samples", name);
    }
}
