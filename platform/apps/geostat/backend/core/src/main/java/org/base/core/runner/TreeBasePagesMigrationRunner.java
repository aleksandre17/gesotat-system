package org.base.core.runner;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.base.core.entity.Migration;
import org.base.core.entity.page_tree.PageNode;
import org.base.core.model.request.MigrationNodeRequest;
import org.base.core.model.request.ParentRef;
import org.base.core.repository.JsonMigrationRepository;
import org.base.core.service.PageStructureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@AllArgsConstructor
public class TreeBasePagesMigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TreeBasePagesMigrationRunner.class);

    private final JsonMigrationRepository jsonMigrationRepository;
    private final PageStructureService pageStructureService;
    private final ObjectMapper objectMapper;

    @Override
    public void run(ApplicationArguments args) {
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources("classpath:migration/*.json");

            Arrays.sort(resources, Comparator.comparing(r -> r.getFilename() != null ? r.getFilename() : ""));

            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename == null) continue;

                int version = extractVersionFromFilename(filename);

                if (isMigrationApplied(filename, version)) {
                    log.info("Skipping migration {} (already applied)", filename);
                    continue;
                }

                try {
                    processMigration(resource, filename, version);
                    log.info("Applied migration: {}", filename);
                } catch (Exception e) {
                    log.error("Failed to apply migration {}: {}", filename, e.getMessage(), e);
                    throw new MigrationException("Migration failed: " + filename, e);
                }
            }
        } catch (MigrationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Migration runner error: {}", e.getMessage(), e);
        }
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private void processMigration(Resource resource, String filename, int version) throws IOException {
        try (InputStream is = resource.getInputStream()) {
            List<MigrationNodeRequest> nodes = Arrays.asList(
                    objectMapper.readValue(is, MigrationNodeRequest[].class));

            for (int i = 0; i < nodes.size(); i++) {
                MigrationNodeRequest node = nodes.get(i);
                if (node.getOrderIndex() == null) node.setOrderIndex(i);
                createRecursively(node, null);
            }

            jsonMigrationRepository.save(Migration.builder()
                    .filename(filename)
                    .version(version)
                    .appliedAt(LocalDateTime.now())
                    .build());
        }
    }

    private void createRecursively(MigrationNodeRequest request, Long parentId) {
        if (parentId != null) {
            ParentRef ref = new ParentRef();
            ref.setId(parentId);
            request.setParent(ref);
        }
        PageNode saved = pageStructureService.create(request);

        List<MigrationNodeRequest> children = request.getChildren();
        if (children == null || children.isEmpty()) return;

        for (int i = 0; i < children.size(); i++) {
            MigrationNodeRequest child = children.get(i);
            if (child.getOrderIndex() == null) child.setOrderIndex(i);
            createRecursively(child, saved.getId());
        }
    }

    private boolean isMigrationApplied(String filename, int version) {
        return jsonMigrationRepository.findByFilename(filename)
                .map(m -> m.getVersion() >= version)
                .orElse(false);
    }

    private int extractVersionFromFilename(String filename) {
        Matcher matcher = Pattern.compile("V(\\d+)__.*\\.json").matcher(filename);
        if (matcher.matches()) {
            return Integer.parseInt(matcher.group(1));
        }
        throw new IllegalArgumentException("Invalid migration filename format: " + filename);
    }

    // ── Exception ──────────────────────────────────────────────────────────────

    public static class MigrationException extends RuntimeException {
        public MigrationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}