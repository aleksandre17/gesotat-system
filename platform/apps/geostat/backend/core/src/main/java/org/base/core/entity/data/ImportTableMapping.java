package org.base.core.entity.data;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

/** Mapping from a table contained in an Access package to an approved data profile. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "import_table_mappings", uniqueConstraints =
        @UniqueConstraint(name = "uk_import_mapping_profile_table", columnNames = {"profile_id", "access_table_name"}))
public class ImportTableMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "profile_id", nullable = false)
    private Long profileId;

    @Column(name = "access_table_name", nullable = false, length = 128)
    private String accessTableName;

    @Enumerated(EnumType.STRING)
    @Column(name = "table_role", nullable = false, length = 16)
    private TableRole tableRole = TableRole.DATA;

    @Enumerated(EnumType.STRING)
    @Column(name = "import_mode", nullable = false, length = 16)
    private ImportMode importMode = ImportMode.APPEND;

    @Nationalized
    @Column(name = "column_mapping_json", columnDefinition = "NVARCHAR(MAX)")
    private String columnMappingJson;

    @Nationalized
    @Column(name = "validation_rules_json", columnDefinition = "NVARCHAR(MAX)")
    private String validationRulesJson;

    @Column(nullable = false)
    private boolean enabled = true;
}
