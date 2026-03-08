package org.base.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.base.api.model.request.UploadPayload;
import org.base.api.model.response.ProgressUpdate;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Centralized service for handling Access file uploads.
 * <p>
 * Encapsulates the full upload lifecycle: validation → deserialization →
 * WebSocket session check → import with progress → response building.
 * <p>
 * Controllers delegate to this service, keeping themselves thin routing shells
 * (Single Responsibility Principle).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FileUploadService {

    private final AccessFileImporter accessFileImporter;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final SimpUserRegistry simpUserRegistry;

    /**
     * Handles the full file-upload workflow.
     *
     * @param file    the uploaded .mdb/.accdb multipart file
     * @param payload JSON string representing {@link UploadPayload}
     * @return appropriate HTTP response (200 on success, 400/500 on failure)
     */
    public ResponseEntity<String> handleUpload(MultipartFile file, String payload) {
        try {
            // ── Validate file ──
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("Please upload a valid Excel file.");
            }

            // ── Deserialize payload ──
            UploadPayload uploadPayload = objectMapper.readValue(payload, UploadPayload.class);

            // ── Validate taskId ──
            String taskId = uploadPayload.getTaskId();
            if (taskId == null || taskId.isEmpty()) {
                log.warn("Upload rejected: taskId is null or empty");
                return ResponseEntity.badRequest().body("Task ID is required");
            }

            // ── Check WebSocket session (non-blocking warning) ──
            boolean sessionExists = simpUserRegistry.getUsers().stream()
                    .anyMatch(user -> user.getName().equals(taskId));
            if (!sessionExists) {
                log.warn("No active WebSocket session for taskId: {}", taskId);
            }

            // ── Execute import with progress tracking ──
            long startTime = System.currentTimeMillis();

            accessFileImporter.parseAndSaveAccessFile(file, uploadPayload, (progress, message) ->
                    messagingTemplate.convertAndSendToUser(
                            taskId, "/topic/progress", new ProgressUpdate(progress, message))
            );

            long elapsed = System.currentTimeMillis() - startTime;
            return ResponseEntity.ok("File uploaded and data saved successfully in " + elapsed + " ms.");

        } catch (Exception e) {
            log.error("Failed to process file upload", e);
            return ResponseEntity.internalServerError()
                    .body("Failed to process file: " + e.getMessage());
        }
    }
}

