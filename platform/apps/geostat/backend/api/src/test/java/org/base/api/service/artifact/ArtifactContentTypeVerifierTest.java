package org.base.api.service.artifact;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ArtifactContentTypeVerifierTest {
    private final ArtifactContentTypeVerifier verifier = new ArtifactContentTypeVerifier();

    @Test
    void detectsPdfFromBytesWithoutTrustingExtension() throws Exception {
        byte[] pdf = "%PDF-1.7\n%âãÏÓ\n".getBytes(StandardCharsets.ISO_8859_1);
        assertEquals("application/pdf", verify("report.pdf", pdf));
    }

    @Test
    void rejectsTextDisguisedAsPdf() {
        assertThrows(IllegalArgumentException.class, () -> verify("report.pdf",
                "plain text".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void acceptsUtfTextAsCsvBecauseMimeDetectorsClassifyDelimitedTextAsPlainText() throws Exception {
        assertEquals("text/csv", verify("data.csv", "id,value\n1,2\n".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void acceptsActualOfficeOpenXmlWorkbookContent() throws Exception {
        try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
             var bytes = new ByteArrayOutputStream()) {
            workbook.createSheet("data").createRow(0).createCell(0).setCellValue("value");
            workbook.write(bytes);
            assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    verify("data.xlsx", bytes.toByteArray()));
        }
    }

    @Test
    void acceptsActualOle2ExcelWorkbookContent() throws Exception {
        try (var workbook = new org.apache.poi.hssf.usermodel.HSSFWorkbook();
             var bytes = new ByteArrayOutputStream()) {
            workbook.createSheet("data").createRow(0).createCell(0).setCellValue("value");
            workbook.write(bytes);
            assertEquals("application/vnd.ms-excel",
                    verify("data.xls", bytes.toByteArray()));
        }
    }

    @Test
    void rejectsWordDocumentMasqueradingAsSpreadsheetWithinOfficeFamily() throws Exception {
        try (var document = new org.apache.poi.xwpf.usermodel.XWPFDocument();
             var bytes = new ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText("document");
            document.write(bytes);
            assertThrows(IllegalArgumentException.class, () -> verify("data.xlsx", bytes.toByteArray()));
        }
    }

    @Test
    void acceptsOnlyReadableAccessDatabaseForAccdbPath() throws Exception {
        var databaseFile = Files.createTempFile("artifact-access-content-", ".accdb");
        try (var database = com.healthmarketscience.jackcess.DatabaseBuilder.create(
                com.healthmarketscience.jackcess.Database.FileFormat.V2010, databaseFile.toFile())) {
            new com.healthmarketscience.jackcess.TableBuilder("records")
                    .addColumn(new com.healthmarketscience.jackcess.ColumnBuilder("id", com.healthmarketscience.jackcess.DataType.TEXT))
                    .toTable(database);
        }
        try {
            assertEquals("application/x-msaccess", verifier.verify("records.accdb", databaseFile));
            Files.writeString(databaseFile, "not an Access database");
            assertThrows(IllegalArgumentException.class, () -> verifier.verify("records.accdb", databaseFile));
        } finally {
            Files.deleteIfExists(databaseFile);
        }
    }

    private String verify(String fileName, byte[] bytes) throws Exception {
        Path temporary = Files.createTempFile("artifact-content-type-test-", ".bin");
        try {
            Files.write(temporary, bytes);
            return verifier.verify(fileName, temporary);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
