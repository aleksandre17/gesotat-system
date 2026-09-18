package org.base.api.service.artifact;

import com.healthmarketscience.jackcess.*;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ArtifactAccessPackageValidatorTest {
    @Test
    void actualAccessSchemaMustMatchApprovedDatasetTableAndFields() throws Exception {
        var file = Files.createTempFile("contract-package-", ".accdb").toFile();
        try (Database database = DatabaseBuilder.create(Database.FileFormat.V2010, file)) {
            new TableBuilder("records").addColumn(new ColumnBuilder("record_id", DataType.TEXT))
                    .addColumn(new ColumnBuilder("title", DataType.TEXT)).toTable(database);
        }
        var validator = new ArtifactAccessPackageValidator();
        var valid = new ArtifactPackageContractResolver.DatasetContract("SITE_A", 2, "a".repeat(64), 17,
                "RECORDS", "records", List.of("record_id", "title"));
        var wrongFields = new ArtifactPackageContractResolver.DatasetContract("SITE_A", 2, "a".repeat(64), 17,
                "RECORDS", "records", List.of("record_id", "unapproved_field"));
        assertDoesNotThrow(() -> validator.validate(file, valid));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(file, wrongFields));
        Files.deleteIfExists(file.toPath());
    }
}
