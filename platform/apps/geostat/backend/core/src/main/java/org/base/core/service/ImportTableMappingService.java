package org.base.core.service;

import lombok.RequiredArgsConstructor;
import org.base.core.entity.data.ImportTableMapping;
import org.base.core.model.request.ImportTableMappingRequest;
import org.base.core.repository.DataProfileRepository;
import org.base.core.repository.ImportTableMappingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional
public class ImportTableMappingService {
    private final ImportTableMappingRepository mappingRepository;
    private final DataProfileRepository dataProfileRepository;

    @Transactional(readOnly = true)
    public List<ImportTableMapping> findByProfile(Long profileId) {
        return mappingRepository.findByProfileIdAndEnabledTrue(profileId);
    }

    public ImportTableMapping create(ImportTableMappingRequest request) {
        assertProfile(request.getProfileId());
        ImportTableMapping mapping = new ImportTableMapping();
        apply(mapping, request);
        return mappingRepository.save(mapping);
    }

    public ImportTableMapping update(Long id, ImportTableMappingRequest request) {
        ImportTableMapping mapping = mappingRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Import mapping not found: " + id));
        assertProfile(request.getProfileId());
        apply(mapping, request);
        return mappingRepository.save(mapping);
    }

    private void assertProfile(Long profileId) {
        if (!dataProfileRepository.existsById(profileId)) {
            throw new NoSuchElementException("Data profile not found: " + profileId);
        }
    }

    private void apply(ImportTableMapping mapping, ImportTableMappingRequest request) {
        mapping.setProfileId(request.getProfileId());
        mapping.setAccessTableName(request.getAccessTableName().trim());
        mapping.setTableRole(request.getTableRole());
        mapping.setImportMode(request.getImportMode());
        mapping.setColumnMappingJson(request.getColumnMappingJson());
        mapping.setValidationRulesJson(request.getValidationRulesJson());
        mapping.setEnabled(Boolean.TRUE.equals(request.getEnabled()));
    }
}
