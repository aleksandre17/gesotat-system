package org.base.api.entity.mobile.mysql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;


@Data
@Entity
@Table(name = "sliders_data")
public class SlidersData {
    @Id
    @Column(name = "ID")
    private Integer id;

    @Column(name = "Pages", length = 45, nullable = false)
    private String pages;

    @Column(name = "Name_ka", length = 250)
    private String nameKa;

    @Column(name = "Name_en", length = 250)
    private String nameEn;

    @Column(name = "Unit", length = 45)
    private String unit;

    @Column(name = "Value", precision = 18, scale = 1)
    private BigDecimal value;
}