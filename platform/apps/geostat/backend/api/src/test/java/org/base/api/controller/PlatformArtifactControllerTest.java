package org.base.api.controller;

import org.base.api.service.artifact.ArtifactAccessDeniedException;
import org.base.api.service.artifact.ArtifactAttachmentService;
import org.base.api.service.artifact.ArtifactDistributionService;
import org.base.api.service.artifact.ArtifactNotFoundException;
import org.base.api.service.artifact.ArtifactPackageService;
import org.base.api.service.artifact.ArtifactReconciliationService;
import org.base.api.service.artifact.ArtifactStorageException;
import org.base.api.service.artifact.ArtifactMalwareDetectedException;
import org.base.api.service.artifact.ArtifactScannerUnavailableException;
import org.base.api.service.artifact.ArtifactUploadIdentityResolver;
import org.base.api.service.artifact.ArtifactUploadSessionService;
import org.base.api.service.artifact.BindingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HTTP contract of the artifact boundary: RFC 9457 errors, no storage internals, no caching of signed URLs. */
class PlatformArtifactControllerTest {
    private static final String DOWNLOAD = "/platform/artifacts/entities/KIDS_RESOURCE/128/PRIMARY_FILE/ka/1/download";
    private final ArtifactDistributionService distribution = mock(ArtifactDistributionService.class);
    private final ArtifactAttachmentService attachments = mock(ArtifactAttachmentService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new PlatformArtifactController(mock(ArtifactPackageService.class), attachments,
                        mock(ArtifactReconciliationService.class), distribution, mock(ArtifactUploadSessionService.class),
                        mock(ArtifactUploadIdentityResolver.class)))
                .setControllerAdvice(new ArtifactApiExceptionHandler()).build();
    }

    @Test
    void signedDownloadIsNeverCached() throws Exception {
        when(distribution.download(eq("KIDS_RESOURCE"), eq("128"), eq("PRIMARY_FILE"), eq("ka"), eq(1), any()))
                .thenReturn(new ArtifactDistributionService.SignedDownload(URI.create("https://files.example/x?X-Amz-Signature=s"), Instant.parse("2026-09-18T10:05:00Z"),
                        "a.xlsx", "application/vnd.ms-excel", 10, "a".repeat(64)));
        mvc.perform(get(DOWNLOAD)).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.sha256").value("a".repeat(64)));
    }

    @Test
    void unknownArtifactIsProblem404() throws Exception {
        when(distribution.download(anyString(), anyString(), anyString(), anyString(), anyInt(), any())).thenThrow(new ArtifactNotFoundException("No published KIDS_RESOURCE with key 128"));
        mvc.perform(get(DOWNLOAD)).andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value("artifact-not-found"));
    }

    @Test
    void policyDenialIsProblem403() throws Exception {
        when(distribution.download(anyString(), anyString(), anyString(), anyString(), anyInt(), any())).thenThrow(new ArtifactAccessDeniedException("denied"));
        mvc.perform(get(DOWNLOAD)).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("artifact-access-denied"));
    }

    @Test
    void storageFailureIsProblem503WithoutInternals() throws Exception {
        when(distribution.download(anyString(), anyString(), anyString(), anyString(), anyInt(), any()))
                .thenThrow(new ArtifactStorageException("bucket geostat-ingest key kids/r8/secret unreachable", null));
        mvc.perform(get(DOWNLOAD)).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("artifact-storage-unavailable"))
                .andExpect(content().string(not(containsString("geostat-ingest"))));
    }

    @Test
    void scannerFailureIsProblem503WithoutScannerDetails() throws Exception {
        when(distribution.download(anyString(), anyString(), anyString(), anyString(), anyInt(), any()))
                .thenThrow(new ArtifactScannerUnavailableException("clamd host secret.internal timeout"));
        mvc.perform(get(DOWNLOAD)).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("artifact-scanner-unavailable"))
                .andExpect(content().string(not(containsString("secret.internal"))));
    }

    @Test
    void malwareRejectionIsProblem422() throws Exception {
        when(distribution.download(anyString(), anyString(), anyString(), anyString(), anyInt(), any()))
                .thenThrow(new ArtifactMalwareDetectedException());
        mvc.perform(get(DOWNLOAD)).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("artifact-content-rejected"));
    }

    @Test
    void publishedSnapshotBindingIsConflict409() throws Exception {
        when(attachments.bind(anyLong(), anyLong(), any(Boolean.class))).thenThrow(new IllegalStateException("Snapshot 7 is PUBLISHED; attachments bind only before publication"));
        mvc.perform(post("/platform/artifacts/snapshots/7/attachments").param("manifestId", "1").param("dryRun", "false"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("artifact-state-conflict"));
    }

    @Test
    void blockedBindingIsUnprocessable() throws Exception {
        when(attachments.bind(7L, 1L, true)).thenReturn(new ArtifactAttachmentService.BindingReport(7, 1, true, BindingStatus.BLOCKED, List.of(),
                Map.of("UNMATCHED_ROW", 1), List.of(), false));
        mvc.perform(post("/platform/artifacts/snapshots/7/attachments").param("manifestId", "1"))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.status").value("BLOCKED"));
    }
}
