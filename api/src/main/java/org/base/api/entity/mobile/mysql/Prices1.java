package org.base.api.entity.mobile.mysql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;


@Data
@Entity
@Table(name = "prices_1")
public class Prices1 {
    @Id
    @Column(name = "ID")
    private Integer id;

    @Column(name = "Species", length = 255)
    private String species;

    @Column(name = "Period")
    private Double period;

    @Column(name = "Unit", length = 255)
    private String unit;

    @Column(name = "Value")
    private Double value;

    @Column(name = "Name_id")
    private Integer nameId;
}