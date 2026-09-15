package org.base.api.service.catalog;

import lombok.RequiredArgsConstructor;
import org.base.core.repository.ImportJobItemRepository;
import org.base.core.repository.ImportJobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class ManagedImportJobQueryService {
    private final ImportJobRepository jobRepository;
    private final ImportJobItemRepository itemRepository;

    @Transactional(readOnly = true)
    public ManagedImportJobDetails findById(Long jobId) {
        var job = jobRepository.findById(jobId)
                .orElseThrow(() -> new NoSuchElementException("Import job not found: " + jobId));
        return new ManagedImportJobDetails(job, itemRepository.findByImportJobIdOrderById(jobId));
    }
}
