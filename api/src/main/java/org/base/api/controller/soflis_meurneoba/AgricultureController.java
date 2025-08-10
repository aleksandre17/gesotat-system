package org.base.api.controller.soflis_meurneoba;

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
// 1. Set the main URL for the Agriculture Portal
@RequestMapping("/soflis-statistikis-portali")
@Api
@FolderPrefix
public class AgricultureController {

    // 2. The internal logic is identical. All these components are reused.
    @Autowired
    private AccessFileImporter accessFileImporter;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private SimpUserRegistry simpUserRegistry;

    // 3. Define the specific endpoints for your new feature here.
    // For example, you might have endpoints for crop data, livestock data, etc.
    @PostMapping(value = {
            "/crop-yields",
            "/livestock-numbers",
            "/land-usage-stats"
            // Add more endpoints as needed
    }, consumes = "multipart/form-data")
    public ResponseEntity<String> uploadExcelFile(
            @RequestPart("file") MultipartFile file,
            @RequestPart String payload) {

        // 4. The method body is a direct copy. It's completely reusable.
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("Please upload a valid Excel file.");
            }

            UploadPayload payload1 = mapper.readValue(payload, UploadPayload.class);

            if (payload1.getTaskId() == null || payload1.getTaskId().isEmpty()) {
                return ResponseEntity.badRequest().body("Task ID is required");
            }

            boolean userExists = simpUserRegistry.getUsers().stream().anyMatch(user -> user.getName().equals(payload1.getTaskId()));
            if (!userExists) {
                System.out.println("Warning: No active WebSocket session for taskId: " + payload1.getTaskId());
            }

            accessFileImporter.parseAndSaveAccessFile(file, payload1, (progress, message) -> {
                messagingTemplate.convertAndSendToUser(payload1.getTaskId(), "/topic/progress", new ProgressUpdate(progress, message));
            });

            return ResponseEntity.ok("File uploaded and data saved successfully.");
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Failed to process file: " + e.getMessage());
        }
    }
}
