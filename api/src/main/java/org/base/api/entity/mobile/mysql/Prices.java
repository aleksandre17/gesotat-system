package org.base.api.entity.mobile.mysql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;


@Data
@Entity
@Table(name = "prices")
public class Prices {
    @Id
    @Column(name = "ID")
    private Integer id;

    @Column(name = "Section", nullable = false)
    private Integer section;

    @Column(name = "Indicator", nullable = false)
    private Integer indicator;

    @Column(name = "Species", nullable = false)
    private Integer species;

    @Column(name = "Year", nullable = false)
    private Integer year;

    @Column(name = "Quarter", nullable = false)
    private Integer quarter;

    @Column(name = "Value")
    private Double value;
}