package org.base.api.service.catalog;

import lombok.RequiredArgsConstructor;
import org.base.api.model.request.UploadPayload;
import org.base.core.entity.data.DataProfile;
import org.base.core.entity.data.ImportTableMapping;
import org.base.core.entity.page_tree.PageLeafNode;
import org.base.core.repository.DataProfileRepository;
import org.base.core.repository.ImportTableMappingRepository;
import org.base.core.repository.PageNodeRepository;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

/** Resolves an Access dataset only through an enabled core profile and mapping. */
@Service
@RequiredArgsConstructor
public class ManagedImportTargetResolver {
    private final DataProfileRepository dataProfileRepository;
    private final ImportTableMappingRepository mappingRepository;
    private final PageNodeRepository pageNodeRepository;

    public ResolvedImportTarget resolve(ManagedDatasetDefinition dataset) {
        DataProfile profile = dataProfileRepository.findByProfileCodeAndEnabledTrue(dataset.profileCode())
                .orElseThrow(() -> new NoSuchElementException("Enabled data profile not found: " + dataset.profileCode()));
        ImportTableMapping mapping = mappingRepository.findByProfileIdAndEnabledTrue(profile.getId()).stream()
                .filter(candidate -> candidate.getAccessTableName().equalsIgnoreCase(dataset.accessTableName()))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Enabled mapping not found for table: " + dataset.accessTableName()));
        PageLeafNode page = pageNodeRepository.findById(profile.getPageId())
                .filter(PageLeafNode.class::isInstance)
                .map(PageLeafNode.class::cast)
                .orElseThrow(() -> new IllegalStateException("Profile page is not a PAGE node: " + profile.getPageId()));

        UploadPayload payload = new UploadPayload();
        payload.setMetaDatabaseType(required(page.getMetaDatabaseType(), "database type"));
        payload.setMetaDatabaseUrl(required(page.getMetaDatabaseUrl(), "database URL"));
        payload.setMetaDatabaseUser(required(page.getMetaDatabaseUser(), "database user"));
        payload.setMetaDatabasePassword(required(page.getMetaDatabasePassword(), "database password"));
        // Import strategies require database-table syntax to select a connection database.
        payload.setMetaDatabaseName(profile.getTargetDatabase() + "-managed_import");
        payload.setMetaTargetDatabase(profile.getTargetDatabase());

        String target = page.getMetaDatabaseType().equalsIgnoreCase("mssql")
                ? profile.getTargetDatabase() + "/" + profile.getTargetSchema() + "/" + profile.getTargetTable()
                : profile.getTargetDatabase() + "/" + profile.getTargetTable();
        return new ResolvedImportTarget(profile.getId(), mapping.getId(), dataset.accessTableName(), target, payload);
    }

    private String required(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Profile PAGE is missing " + label);
        }
        return value;
    }
}
