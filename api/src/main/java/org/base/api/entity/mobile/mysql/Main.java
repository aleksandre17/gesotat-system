package org.base.api.entity.mobile.mysql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import lombok.Data;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Data
@Entity
@Table(name = "main")
public class Main {
    @Id
    @Column(name = "ID")
    private Long id;

    @Column(name = "Section")
    private Integer section;

    @Column(name = "Indicator", nullable = false)
    private Integer indicator;

    @Column(name = "Unit", nullable = false)
    private Integer unit;

    @Column(name = "Species", length = 10, nullable = false)
    private String species;

    @Column(name = "Species_1", length = 10)
    private String species1;

    @Column(name = "Period", nullable = false)
    private Integer period;

    @Column(name = "Region", length = 10, nullable = false)
    private String region;

    @Column(name = "Value", precision = 18, scale = 1, nullable = false)
    private BigDecimal value;
}