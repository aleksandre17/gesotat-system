package org.base.api.entity.mysql;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "agriculture_stats") // The name of your new table in MySQL
public class AgricultureData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String region;

    private String cropType;

    private Integer year;

    private Double yield; // The harvest amount
}