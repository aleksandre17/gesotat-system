package org.base.api.service.catalog;

import lombok.RequiredArgsConstructor;
import org.base.core.entity.data.ChartType;
import org.base.core.entity.data.DataKind;
import org.base.core.entity.data.ImportTableMapping;
import org.base.core.repository.DataProfileRepository;
import org.base.core.repository.ImportTableMappingRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Validates a managed Access package against approved core registry metadata. */
@Service
@RequiredArgsConstructor
public class ManagedPackageValidationService {
    private final DataProfileRepository dataProfileRepository;
    private final ImportTableMappingRepository mappingRepository;

    public PackageValidationResult validate(ManagedAccessPackage accessPackage, AccessCatalog catalog) {
        List<PackageValidationIssue> issues = new ArrayList<>();
        Set<String> accessTables = catalog.tables().stream()
                .map(AccessTableCatalog::name)
                .map(name -> name.toLowerCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toSet());
        Set<String> datasetCodes = new HashSet<>();
        Set<String> chartCodes = new HashSet<>();

        for (ManagedDatasetDefinition dataset : accessPackage.datasets()) {
            String subject = dataset.datasetCode();
            if (!datasetCodes.add(dataset.datasetCode())) {
                issues.add(issue("DUPLICATE_DATASET_CODE", subject, "dataset_code must be unique within the package"));
            }
            if (!accessTables.contains(dataset.accessTableName().toLowerCase(Locale.ROOT))) {
                issues.add(issue("MISSING_ACCESS_TABLE", subject, "Declared Access table does not exist: " + dataset.accessTableName()));
            }
            DataKind declaredKind = parseDataKind(dataset.dataKind(), subject, issues);
            dataProfileRepository.findByProfileCodeAndEnabledTrue(dataset.profileCode()).ifPresentOrElse(profile -> {
                if (declaredKind != null && profile.getDataKind() != declaredKind && profile.getDataKind() != DataKind.BOTH) {
                    issues.add(issue("DATA_KIND_MISMATCH", subject, "Package data_kind does not match approved profile"));
                }
                List<ImportTableMapping> mappings = mappingRepository.findByProfileIdAndEnabledTrue(profile.getId());
                boolean mappingExists = mappings.stream().anyMatch(mapping ->
                        mapping.getAccessTableName().equalsIgnoreCase(dataset.accessTableName()));
                if (!mappingExists) {
                    issues.add(issue("UNMAPPED_TABLE", subject, "No approved mapping for Access table: " + dataset.accessTableName()));
                }
            }, () -> issues.add(issue("UNKNOWN_PROFILE", subject, "No enabled core data profile: " + dataset.profileCode())));
        }

        for (ManagedChartDefinition chart : accessPackage.charts()) {
            String subject = chart.chartCode();
            if (!chartCodes.add(chart.chartCode())) {
                issues.add(issue("DUPLICATE_CHART_CODE", subject, "chart_code must be unique within the package"));
            }
            if (!datasetCodes.contains(chart.datasetCode())) {
                issues.add(issue("UNKNOWN_CHART_DATASET", subject, "Chart references an unknown dataset_code: " + chart.datasetCode()));
            }
            try {
                ChartType.valueOf(chart.chartType().trim().toUpperCase(Locale.ROOT).replace('-', '_'));
            } catch (IllegalArgumentException ex) {
                issues.add(issue("UNSUPPORTED_CHART_TYPE", subject, "Unsupported chart type: " + chart.chartType()));
            }
        }

        for (ManagedChartFilter filter : accessPackage.chartFilters()) {
            if (!chartCodes.contains(filter.chartCode())) {
                issues.add(issue("UNKNOWN_FILTER_CHART", filter.chartCode(), "Filter references an unknown chart_code"));
            }
            if (!Set.of("EQ", "NE", "IS_NULL", "NOT_NULL").contains(filter.operator().toUpperCase(Locale.ROOT))) {
                issues.add(issue("UNSUPPORTED_FILTER_OPERATOR", filter.chartCode(), "Unsupported filter operator: " + filter.operator()));
            }
        }
        return new PackageValidationResult(issues.isEmpty(), List.copyOf(issues));
    }

    private DataKind parseDataKind(String value, String subject, List<PackageValidationIssue> issues) {
        try {
            return DataKind.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            issues.add(issue("UNSUPPORTED_DATA_KIND", subject, "Unsupported data_kind: " + value));
            return null;
        }
    }

    private PackageValidationIssue issue(String code, String subject, String message) {
        return new PackageValidationIssue(code, subject, message);
    }
}
