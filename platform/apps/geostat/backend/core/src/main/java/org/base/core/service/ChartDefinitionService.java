package org.base.core.service;

import lombok.RequiredArgsConstructor;
import org.base.core.entity.data.ChartDefinition;
import org.base.core.entity.data.PublicationStatus;
import org.base.core.model.request.ChartDefinitionRequest;
import org.base.core.repository.ChartDefinitionRepository;
import org.base.core.repository.DataProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional
public class ChartDefinitionService {
    private final ChartDefinitionRepository chartDefinitionRepository;
    private final DataProfileRepository dataProfileRepository;

    @Transactional(readOnly = true)
    public List<ChartDefinition> findPublishedByProfile(Long profileId) {
        return chartDefinitionRepository.findByProfileIdAndPublicationStatus(profileId, PublicationStatus.PUBLISHED);
    }

    public ChartDefinition create(ChartDefinitionRequest request) {
        assertProfile(request.getProfileId());
        ChartDefinition definition = new ChartDefinition();
        apply(definition, request);
        return chartDefinitionRepository.save(definition);
    }

    public ChartDefinition update(Long id, ChartDefinitionRequest request) {
        ChartDefinition definition = chartDefinitionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Chart definition not found: " + id));
        assertProfile(request.getProfileId());
        apply(definition, request);
        definition.setVersion(definition.getVersion() + 1);
        return chartDefinitionRepository.save(definition);
    }

    private void assertProfile(Long profileId) {
        if (!dataProfileRepository.existsById(profileId)) {
            throw new NoSuchElementException("Data profile not found: " + profileId);
        }
    }

    private void apply(ChartDefinition definition, ChartDefinitionRequest request) {
        definition.setProfileId(request.getProfileId());
        definition.setChartCode(request.getChartCode().trim());
        definition.setChartType(request.getChartType());
        definition.setXField(request.getXField());
        definition.setYField(request.getYField());
        definition.setSeriesField(request.getSeriesField());
        definition.setAggregation(request.getAggregation());
        definition.setFiltersJson(request.getFiltersJson());
        definition.setDisplayJson(request.getDisplayJson());
        definition.setPublicationStatus(request.getPublicationStatus());
    }
}
