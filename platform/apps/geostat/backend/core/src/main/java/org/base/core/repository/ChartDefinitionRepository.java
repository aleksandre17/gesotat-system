package org.base.core.repository;

import org.base.core.entity.data.ChartDefinition;
import org.base.core.entity.data.PublicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChartDefinitionRepository extends JpaRepository<ChartDefinition, Long> {
    List<ChartDefinition> findByProfileIdAndPublicationStatus(Long profileId, PublicationStatus publicationStatus);
    Optional<ChartDefinition> findTopByProfileIdAndChartCodeOrderByVersionDesc(Long profileId, String chartCode);
}
