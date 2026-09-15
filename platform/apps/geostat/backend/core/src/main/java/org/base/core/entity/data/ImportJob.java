package org.base.core.entity.data;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Immutable audit record for a submitted Access package. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "import_jobs", indexes = {
        @Index(name = "ix_import_jobs_status", columnList = "status"),
        @Index(name = "ix_import_jobs_checksum", columnList = "file_checksum")
})
public class ImportJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "package_code", length = 120)
    private String packageCode;

    @Column(name = "package_version", length = 64)
    private String packageVersion;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "file_checksum", nullable = false, length = 128)
    private String fileChecksum;

    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ImportJobStatus status = ImportJobStatus.RECEIVED;

    @Column(name = "submitted_by_user_id")
    private Long submittedByUserId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "error_summary", columnDefinition = "NVARCHAR(MAX)")
    private String errorSummary;

    @PrePersist
    void initializeCreatedAt() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
