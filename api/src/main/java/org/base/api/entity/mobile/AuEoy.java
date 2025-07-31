package org.base.api.entity.mobile;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Data
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "auto_eoes")
public class AuEoy {
    @Id
    private Long id;

    private Integer year;

    private String transport;

    private String body;

    private String brand;

    private String model;

    private Integer yearOfProduction;

    private String age;

    private String color;

    private String fuel;

    private String engine;

    private String region;

    private String quantity;
}
