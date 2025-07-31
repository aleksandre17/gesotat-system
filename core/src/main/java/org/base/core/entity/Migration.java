package org.base.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "migrations")
public class Migration {

    @Id
    @Column(name = "filename", length = 255)
    private String filename;

    @Column(name = "version")
    private int version;

    @Column(name = "applied_at")
    private LocalDateTime appliedAt;
}
