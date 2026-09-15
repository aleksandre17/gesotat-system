package org.base.api.service.catalog;

import lombok.RequiredArgsConstructor;
import org.base.api.service.AccessFileImporter;
import org.base.core.entity.data.ImportJob;
import org.base.core.entity.data.ImportJobItem;
import org.base.core.entity.data.ImportJobItemStatus;
import org.base.core.entity.data.ImportJobStatus;
import org.base.core.entity.data.ChartDefinition;
import org.base.core.entity.data.ChartType;
import org.base.core.entity.data.PublicationStatus;
import org.base.core.repository.ChartDefinitionRepository;
import org.base.core.repository.ImportJobItemRepository;
import org.base.core.repository.ImportJobRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Executes only validated managed datasets; each source table is independently audited. */
@Service
@RequiredArgsConstructor
public class ManagedImportExecutionService {
    private final AccessCatalogService catalogService;
    private final ManagedAccessPackageReader packageReader;
    private final ManagedPackageValidationService validationService;
    private final ManagedImportTargetResolver targetResolver;
    private final ImportJobAuditService auditService;
    private final ImportJobRepository jobRepository;
    private final ImportJobItemRepository itemRepository;
    private final AccessFileImporter accessFileImporter;
    private final ChartDefinitionRepository chartDefinitionRepository;
    private final ObjectMapper objectMapper;

    public ManagedImportExecutionResult execute(MultipartFile file) throws IOException {
        File tempFile = File.createTempFile("managed-access-import-", ".accdb");
        try {
            file.transferTo(tempFile);
            AccessCatalog catalog = catalogService.catalog(tempFile);
            if (!catalog.managedPackage()) {
                throw new IllegalArgumentException("Only a Managed Access Package can use this endpoint");
            }
            ManagedAccessPackage accessPackage = packageReader.read(tempFile);
            ImportJob job = auditService.open(file, accessPackage);
            auditService.recordCatalog(job, catalog);
            PackageValidationResult validation = validationService.validate(accessPackage, catalog);
            auditService.completeValidation(job, validation);
            if (!validation.valid()) {
                return new ManagedImportExecutionResult(job.getId(), ImportJobStatus.FAILED);
            }

            job.setStatus(ImportJobStatus.IMPORTING);
            jobRepository.save(job);
            Map<String, ResolvedImportTarget> targets = new HashMap<>();
            for (ManagedDatasetDefinition dataset : accessPackage.datasets()) {
                targets.put(dataset.datasetCode(), importDataset(tempFile, job, dataset));
            }

            var items = itemRepository.findByImportJobIdOrderById(job.getId());
            boolean anyFailed = items.stream().anyMatch(item -> item.getStatus() == ImportJobItemStatus.FAILED);
            boolean requiredFailed = accessPackage.datasets().stream().filter(ManagedDatasetDefinition::required)
                    .anyMatch(dataset -> items.stream().anyMatch(item -> item.getAccessTableName().equalsIgnoreCase(dataset.accessTableName())
                            && item.getStatus() == ImportJobItemStatus.FAILED));
            job.setStatus(requiredFailed ? ImportJobStatus.FAILED : anyFailed ? ImportJobStatus.PARTIAL_SUCCESS : ImportJobStatus.SUCCESS);
            if (!requiredFailed) {
                storeChartDrafts(accessPackage, targets);
            }
            job.setCompletedAt(LocalDateTime.now());
            jobRepository.save(job);
            return new ManagedImportExecutionResult(job.getId(), job.getStatus());
        } finally {
            Files.deleteIfExists(tempFile.toPath());
        }
    }

    private ResolvedImportTarget importDataset(File accessFile, ImportJob job, ManagedDatasetDefinition dataset) {
        ImportJobItem item = itemRepository.findByImportJobIdOrderById(job.getId()).stream()
                .filter(candidate -> candidate.getAccessTableName().equalsIgnoreCase(dataset.accessTableName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing import audit item: " + dataset.accessTableName()));
        try {
            ResolvedImportTarget target = targetResolver.resolve(dataset);
            item.setProfileId(target.profileId());
            item.setMappingId(target.mappingId());
            String[] targetParts = target.targetTableName().split("/");
            item.setTargetDatabase(targetParts[0]);
            item.setTargetSchema(targetParts.length == 3 ? targetParts[1] : null);
            item.setTargetTable(targetParts[targetParts.length - 1]);
            item.setStatus(ImportJobItemStatus.IMPORTING);
            itemRepository.save(item);
            accessFileImporter.parseAndSaveAccessTable(accessFile, target.payload(), target.sourceTableName(),
                    target.targetTableName(), null);
            item.setInsertedRowCount(item.getSourceRowCount());
            item.setRejectedRowCount(0L);
            item.setStatus(ImportJobItemStatus.SUCCESS);
            itemRepository.save(item);
            return target;
        } catch (RuntimeException exception) {
            item.setStatus(ImportJobItemStatus.FAILED);
            item.setErrorDetail(exception.getMessage());
            itemRepository.save(item);
            return null;
        }
    }

    private void storeChartDrafts(ManagedAccessPackage accessPackage, Map<String, ResolvedImportTarget> targets) {
        for (ManagedChartDefinition chart : accessPackage.charts()) {
            ResolvedImportTarget target = targets.get(chart.datasetCode());
            if (target == null) continue;
            ChartDefinition definition = new ChartDefinition();
            definition.setProfileId(target.profileId());
            definition.setChartCode(chart.chartCode());
            definition.setChartType(ChartType.valueOf(chart.chartType().trim().toUpperCase().replace('-', '_')));
            definition.setXField(chart.xField());
            definition.setYField(chart.yField());
            definition.setSeriesField(chart.seriesField());
            definition.setAggregation(chart.aggregation());
            try {
                definition.setFiltersJson(objectMapper.writeValueAsString(accessPackage.chartFilters().stream()
                        .filter(filter -> filter.chartCode().equals(chart.chartCode())).toList()));
            } catch (Exception e) {
                throw new IllegalStateException("Unable to serialize chart filters", e);
            }
            definition.setPublicationStatus(PublicationStatus.DRAFT);
            int nextVersion = chartDefinitionRepository.findTopByProfileIdAndChartCodeOrderByVersionDesc(target.profileId(), chart.chartCode())
                    .map(existing -> existing.getVersion() + 1).orElse(1);
            definition.setVersion(nextVersion);
            chartDefinitionRepository.save(definition);
        }
    }
}
