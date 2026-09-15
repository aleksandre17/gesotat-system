package org.base.core.entity.data;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Per-source-table result belonging to one Access import job. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "import_job_items", uniqueConstraints =
        @UniqueConstraint(name = "uk_import_job_item_table", columnNames = {"import_job_id", "access_table_name"}))
public class ImportJobItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "import_job_id", nullable = false)
    private Long importJobId;

    @Column(name = "profile_id")
    private Long profileId;

    @Column(name = "mapping_id")
    private Long mappingId;

    @Column(name = "access_table_name", nullable = false, length = 128)
    private String accessTableName;

    @Column(name = "target_database", length = 128)
    private String targetDatabase;

    @Column(name = "target_schema", length = 128)
    private String targetSchema;

    @Column(name = "target_table", length = 128)
    private String targetTable;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ImportJobItemStatus status = ImportJobItemStatus.MAPPED;

    @Column(name = "source_row_count")
    private Long sourceRowCount;

    @Column(name = "inserted_row_count")
    private Long insertedRowCount;

    @Column(name = "rejected_row_count")
    private Long rejectedRowCount;

    @Column(name = "error_detail", columnDefinition = "NVARCHAR(MAX)")
    private String errorDetail;
}
