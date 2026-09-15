package org.base.core.entity.data;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

/** A versioned, declarative chart definition. It contains no raw SQL. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "chart_definitions", uniqueConstraints =
        @UniqueConstraint(name = "uk_chart_definition_profile_code", columnNames = {"profile_id", "chart_code", "version"}))
public class ChartDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "profile_id", nullable = false)
    private Long profileId;

    @Column(name = "chart_code", nullable = false, length = 120)
    private String chartCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "chart_type", nullable = false, length = 32)
    private ChartType chartType;

    @Column(name = "x_field", length = 128)
    private String xField;

    @Column(name = "y_field", length = 128)
    private String yField;

    @Column(name = "series_field", length = 128)
    private String seriesField;

    @Column(name = "aggregation", length = 16)
    private String aggregation;

    @Nationalized
    @Column(name = "filters_json", columnDefinition = "NVARCHAR(MAX)")
    private String filtersJson;

    @Nationalized
    @Column(name = "display_json", columnDefinition = "NVARCHAR(MAX)")
    private String displayJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "publication_status", nullable = false, length = 16)
    private PublicationStatus publicationStatus = PublicationStatus.DRAFT;

    @Column(nullable = false)
    private int version = 1;
}
