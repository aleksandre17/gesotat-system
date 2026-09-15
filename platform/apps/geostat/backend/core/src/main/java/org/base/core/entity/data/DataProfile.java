package org.base.core.entity.data;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

/**
 * Approved, server-side definition of how a PAGE reads and writes a child table.
 * A package may reference only {@code profileCode}; it never contains a DB secret.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "data_profiles", uniqueConstraints = {
        @UniqueConstraint(name = "uk_data_profiles_page", columnNames = "page_id"),
        @UniqueConstraint(name = "uk_data_profiles_code", columnNames = "profile_code")
})
public class DataProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "page_id", nullable = false)
    private Long pageId;

    @Column(name = "profile_code", nullable = false, length = 120)
    private String profileCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_mode", nullable = false, length = 16)
    private DataMode dataMode = DataMode.LEGACY;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_kind", nullable = false, length = 16)
    private DataKind dataKind = DataKind.TABLE;

    @Column(name = "target_database", nullable = false, length = 128)
    private String targetDatabase;

    @Column(name = "target_schema", nullable = false, length = 128)
    private String targetSchema;

    @Column(name = "target_table", nullable = false, length = 128)
    private String targetTable;

    @Nationalized
    @Column(name = "row_key_json", columnDefinition = "NVARCHAR(MAX)")
    private String rowKeyJson;

    @Nationalized
    @Column(name = "display_columns_json", columnDefinition = "NVARCHAR(MAX)")
    private String displayColumnsJson;

    @Nationalized
    @Column(name = "allowed_query_json", columnDefinition = "NVARCHAR(MAX)")
    private String allowedQueryJson;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "version", nullable = false)
    private int version = 1;
}
