package org.base.api.controller;

import org.base.api.service.artifact.ArtifactMalwareAdmission;
import org.base.api.service.artifact.ArtifactMalwareDetectedException;
import org.base.api.service.artifact.ArtifactScannerUnavailableException;
import org.base.api.service.platform.PlatformAccessIngestionService;
import org.base.api.service.platform.access.SemanticAccessPackageIngestionService;
import org.base.api.service.platform.access.SemanticAccessPreviewService;
import org.base.api.service.storage.ObjectStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Access package uploads pass malware admission before any storage write or ingestion side effect. */
class AccessAdmissionControllerTest {
    private final ObjectStorageService storage = mock(ObjectStorageService.class);
    private final ArtifactMalwareAdmission admission = mock(ArtifactMalwareAdmission.class);
    private final SemanticAccessPackageIngestionService semanticIngestion = mock(SemanticAccessPackageIngestionService.class);
    private final PlatformAccessIngestionService accessIngestion = mock(PlatformAccessIngestionService.class);

    @SuppressWarnings("unchecked")
    private MockMvc mvc() {
        ObjectProvider<ObjectStorageService> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(storage);
        return MockMvcBuilders.standaloneSetup(
                        new PlatformSemanticAccessController(mock(SemanticAccessPreviewService.class), semanticIngestion, provider, admission),
                        new PlatformAccessIngestionController(accessIngestion, provider, admission, 1024))
                .setControllerAdvice(new AccessAdmissionExceptionHandler()).build();
    }

    private static MockMultipartFile accdb() {
        return new MockMultipartFile("file", "kids.accdb", "application/x-msaccess", new byte[]{1, 2, 3});
    }

    @Test
    void infectedSemanticPackageIsRejectedWithoutStorageOrIngestion() throws Exception {
        doThrow(new ArtifactMalwareDetectedException()).when(admission).admit(any(), any(), anyString(), anyString(), eq("ACCESS_PACKAGE"));
        mvc().perform(multipart("/platform/access/semantic/ingest").file(accdb()))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("artifact-content-rejected"));
        verify(storage, never()).storeOriginal(any(), any(), any(), anyLong());
        verifyNoInteractions(semanticIngestion);
    }

    @Test
    void unavailableScannerFailsClosedForAccessIngest() throws Exception {
        doThrow(new ArtifactScannerUnavailableException("scanner host unreachable")).when(admission).admit(any(), any(), anyString(), anyString(), eq("ACCESS_PACKAGE"));
        mvc().perform(multipart("/platform/access/ingest").file(accdb()).param("contractSourceId", "5"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("artifact-scanner-unavailable"));
        verify(storage, never()).storeOriginal(any(), any(), any(), anyLong());
        verifyNoInteractions(accessIngestion);
    }
}
