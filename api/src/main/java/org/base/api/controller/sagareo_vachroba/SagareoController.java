package org.base.api.controller.sagareo_vachroba;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.base.api.model.request.UploadPayload;
import org.base.api.model.response.ProgressUpdate;
import org.base.api.service.AccessFileImporter;
import org.base.core.anotation.Api;
import org.base.core.anotation.FolderPrefix;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RequiredArgsConstructor
@RestController
@RequestMapping("/sagareo-vachrobis-portali")
@Api
@FolderPrefix
public class SagareoController {

    @Autowired
    private AccessFileImporter accessFileImporter;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;


    @Autowired
    private SimpUserRegistry simpUserRegistry;

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<String> uploadExcelFile(
            @RequestPart("file") MultipartFile file,
            @RequestPart String payload) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("Please upload a valid Excel file.");
            }

            UploadPayload payload1 = mapper.readValue(payload, UploadPayload.class);

            if (payload1.getTaskId() == null || payload1.getTaskId().isEmpty()) {
                System.out.println("Error: Task ID is null or empty");
                return ResponseEntity.badRequest().body("Task ID is required");
            }

            // Check session
            boolean userExists = simpUserRegistry.getUsers().stream().anyMatch(user -> user.getName().equals(payload1.getTaskId()));
            System.out.println("User session exists for taskId " + payload1.getTaskId() + ": " + userExists);
            if (!userExists) {
                System.out.println("Warning: No active WebSocket session for taskId: " + payload1.getTaskId());
            }

            long startTime = System.currentTimeMillis();
            accessFileImporter.parseAndSaveAccessFile(file, payload1, (progress, message) -> {
                messagingTemplate.convertAndSendToUser(payload1.getTaskId(), "/topic/progress", new ProgressUpdate(progress, message));
            });
            long endTime = System.currentTimeMillis();

            return ResponseEntity.ok("File uploaded and data saved successfully in " + (endTime - startTime) + " ms.");
        } catch (IOException e) {
            System.out.println("Failed to process file: " + e.getMessage());
            return ResponseEntity.status(500).body("Failed to process file: " + e.getMessage());
        }
    }

}
