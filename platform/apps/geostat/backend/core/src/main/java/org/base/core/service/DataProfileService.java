package org.base.core.service;

import lombok.RequiredArgsConstructor;
import org.base.core.entity.data.DataProfile;
import org.base.core.entity.page_tree.PageLeafNode;
import org.base.core.model.request.DataProfileRequest;
import org.base.core.repository.DataProfileRepository;
import org.base.core.repository.PageNodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

/** Administrative service for approved, server-side data targets. */
@Service
@RequiredArgsConstructor
@Transactional
public class DataProfileService {

    private final DataProfileRepository dataProfileRepository;
    private final PageNodeRepository pageNodeRepository;

    @Transactional(readOnly = true)
    public List<DataProfile> findAll() {
        return dataProfileRepository.findAll();
    }

    @Transactional(readOnly = true)
    public DataProfile findById(Long id) {
        return dataProfileRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Data profile not found: " + id));
    }

    public DataProfile create(DataProfileRequest request) {
        assertPage(request.getPageId());
        dataProfileRepository.findByProfileCodeAndEnabledTrue(request.getProfileCode())
                .ifPresent(ignored -> { throw new IllegalArgumentException("Profile code already exists: " + request.getProfileCode()); });
        dataProfileRepository.findByPageId(request.getPageId())
                .ifPresent(ignored -> { throw new IllegalArgumentException("Page already has a data profile: " + request.getPageId()); });

        DataProfile profile = new DataProfile();
        apply(profile, request);
        return dataProfileRepository.save(profile);
    }

    public DataProfile update(Long id, DataProfileRequest request) {
        DataProfile profile = findById(id);
        assertPage(request.getPageId());
        dataProfileRepository.findByPageId(request.getPageId())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(ignored -> { throw new IllegalArgumentException("Page already has a data profile: " + request.getPageId()); });
        apply(profile, request);
        profile.setVersion(profile.getVersion() + 1);
        return dataProfileRepository.save(profile);
    }

    private void assertPage(Long pageId) {
        var node = pageNodeRepository.findById(pageId)
                .orElseThrow(() -> new NoSuchElementException("Page node not found: " + pageId));
        if (!(node instanceof PageLeafNode)) {
            throw new IllegalArgumentException("Data profile can be assigned only to a PAGE node: " + pageId);
        }
    }

    private void apply(DataProfile profile, DataProfileRequest request) {
        profile.setPageId(request.getPageId());
        profile.setProfileCode(request.getProfileCode().trim());
        profile.setDataMode(request.getDataMode());
        profile.setDataKind(request.getDataKind());
        profile.setTargetDatabase(request.getTargetDatabase().trim());
        profile.setTargetSchema(request.getTargetSchema().trim());
        profile.setTargetTable(request.getTargetTable().trim());
        profile.setRowKeyJson(request.getRowKeyJson());
        profile.setDisplayColumnsJson(request.getDisplayColumnsJson());
        profile.setAllowedQueryJson(request.getAllowedQueryJson());
        profile.setEnabled(Boolean.TRUE.equals(request.getEnabled()));
    }
}
