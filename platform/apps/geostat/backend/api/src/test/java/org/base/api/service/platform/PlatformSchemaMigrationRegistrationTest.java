package org.base.api.service.platform;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Every packaged platform migration must be executed by the runner, exactly once and in numeric order. */
class PlatformSchemaMigrationRegistrationTest {
    private static final Path MIGRATIONS = Path.of("core", "src", "main", "resources", "db", "platform");
    private static final Path RUNNER = Path.of("api", "src", "main", "java", "org", "base", "api", "service", "platform", "PlatformSchemaMigrationRunner.java");

    /** Gradle's test working directory differs between invocations; locate the backend root explicitly. */
    private static Path backendRoot() {
        for (Path dir = Path.of("").toAbsolutePath(); dir != null; dir = dir.getParent()) {
            if (Files.isDirectory(dir.resolve(MIGRATIONS)) && Files.isRegularFile(dir.resolve(RUNNER))) return dir;
        }
        throw new IllegalStateException("backend root not found from " + Path.of("").toAbsolutePath());
    }

    /** Pre-existing files outside the runner. Do not extend: register new migrations instead. */
    private static final Set<String> KNOWN_UNREGISTERED = Set.of(
            "000_create_platform_databases.sql",
            "037_prefixed_access_source_locators.sql",
            "039_kids_r7_source_locator_alignment.sql",
            "045_kids_r7_classifier_assignment_projection.sql",
            "047_kids_r7_classifier_owner_approval.sql",
            "054_contract_page_binding.sql",
            "055_kids_final_page_contract_revision.sql",
            "056_activate_kids_r8_supersede_legacy.sql");

    @Test
    void everyMigrationFileIsRegisteredOnceInOrder() throws Exception {
        Path root = backendRoot();
        Set<String> files = new TreeSet<>();
        try (Stream<Path> stream = Files.list(root.resolve(MIGRATIONS))) {
            stream.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(".sql")).forEach(files::add);
        }
        List<String> registered = new ArrayList<>();
        Matcher matcher = Pattern.compile("\"db/platform/([^\"]+\\.sql)\"").matcher(Files.readString(root.resolve(RUNNER)));
        while (matcher.find()) registered.add(matcher.group(1));

        assertEquals(new TreeSet<>(registered).size(), registered.size(), "a migration is registered more than once");
        assertEquals(registered.stream().sorted().toList(), registered, "migrations must run in numeric order");
        for (String name : registered) assertTrue(files.contains(name), "registered migration file is missing: " + name);
        for (String name : files) {
            if (KNOWN_UNREGISTERED.contains(name)) continue;
            assertTrue(registered.contains(name), "migration is not registered in PlatformSchemaMigrationRunner: " + name);
        }
    }
}
