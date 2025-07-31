package org.base.core.runner;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.base.core.entity.Migration;
import org.base.core.model.request.NodeRequest;
import org.base.core.repository.JsonMigrationRepository;
import org.base.core.service.PageStructureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@AllArgsConstructor
public class TreeBasePagesMigrationRunner implements ApplicationRunner  {

    private static final Logger log = LoggerFactory.getLogger(TreeBasePagesMigrationRunner.class);

    private JsonMigrationRepository jsonMigrationRepository;
    private PageStructureService pageStructureService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver().getResources("classpath:migration/*.json");

            Arrays.sort(resources, Comparator.comparing(Resource::getFilename));

            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename == null) continue;

                int version = extractVersionFromFilename(filename);

                // Skip if migration already applied
                if (isMigrationApplied(filename, version)) {
                    log.info("Skipping migration {} as it's already applied", filename);
                    continue;
                }

                // Process migration
                try {
                    processMigration(resource, filename, version);
                    log.info("Successfully applied migration: {}", filename);
                } catch (Exception e) {
                    log.error("Failed to apply migration {}: {}", filename, e.getMessage(), e);
                    throw new MigrationException("Migration failed: " + filename, e);
                }
            }
        } catch (Exception e) {
            log.error("Failed to run migrations: {}", e.getMessage(), e);
        }
    }


    private boolean isMigrationApplied(String filename, int version) {
        return jsonMigrationRepository.findByFilename(filename)
                .map(migration -> migration.getVersion() >= version)
                .orElse(false);
    }

    private void processMigration(Resource resource, String filename, int version) throws IOException {
        try (InputStream is = resource.getInputStream()) {

            List<NodeRequest> nodes = Arrays.asList(objectMapper.readValue(is, NodeRequest[].class));

            pageStructureService.createTreeStructure(nodes);

            Migration migration = Migration.builder()
                    .filename(filename)
                    .version(version)
                    .appliedAt(LocalDateTime.now())
                    .build();

            jsonMigrationRepository.save(migration);
        }
    }

    private int extractVersionFromFilename(String filename) {
        try {
            // V1__description.json -> 1
            Matcher matcher = Pattern.compile("V(\\d+)__.*\\.json").matcher(filename);
            if (matcher.matches()) {
                return Integer.parseInt(matcher.group(1));
            }
            throw new IllegalArgumentException("Invalid migration filename format: " + filename);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid version number in filename: " + filename);
        }
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public static class MigrationException extends RuntimeException {
        public MigrationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}
