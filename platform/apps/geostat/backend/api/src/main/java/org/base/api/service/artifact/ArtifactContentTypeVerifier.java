package org.base.api.service.artifact;

import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Locale;
import java.util.zip.ZipFile;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import com.healthmarketscience.jackcess.DatabaseBuilder;

/**
 * Checks that bytes agree with the media type inferred from the package path. The path is used
 * only as a declared semantic hint; Tika receives the stream without a filename or caller MIME
 * metadata so an extension cannot make arbitrary bytes appear to be a trusted document type.
 */
@Component
public final class ArtifactContentTypeVerifier {
    private final Tika detector = new Tika();

    public String verify(String fileName, Path contentFile) throws IOException {
        String declared = MediaTypes.forFileName(fileName).toLowerCase(Locale.ROOT);
        String detected;
        try (InputStream content = java.nio.file.Files.newInputStream(contentFile)) {
            detected = detector.detect(content).toLowerCase(Locale.ROOT);
        }
        boolean xlsx = declared.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                && detected.equals("application/x-tika-ooxml") && hasSpreadsheetWorkbookPart(contentFile);
        boolean xls = declared.equals("application/vnd.ms-excel")
                && detected.equals("application/x-tika-msoffice") && hasLegacyExcelWorkbookStream(contentFile);
        boolean access = declared.equals("application/x-msaccess") && isReadableAccessDatabase(contentFile);
        boolean compatible = declared.equals(detected)
                // Plain text detectors cannot distinguish CSV from other UTF text. The extension
                // declares CSV semantics; binary data still resolves to a non-text MIME type.
                || (declared.equals("text/csv") && detected.equals("text/plain"))
                // tika-core deliberately reports Office containers at their container-family
                // MIME. The extension supplies the narrower, contract-facing subtype only after
                // those Office container bytes have independently been detected.
                || xlsx || xls || access;
        if (!compatible) {
            throw new IllegalArgumentException("Package content does not match declared media type " + declared + "; detected " + detected);
        }
        return declared;
    }

    private static boolean isReadableAccessDatabase(Path path) {
        try (var database = new DatabaseBuilder(path.toFile()).setReadOnly(true).open()) {
            return !database.getTableNames().isEmpty();
        } catch (IOException | RuntimeException invalidAccess) {
            return false;
        }
    }

    private static boolean hasSpreadsheetWorkbookPart(Path path) throws IOException {
        try (ZipFile zip = new ZipFile(path.toFile())) {
            var contentTypes = zip.getEntry("[Content_Types].xml");
            if (contentTypes == null || zip.getEntry("xl/workbook.xml") == null) return false;
            try (InputStream input = zip.getInputStream(contentTypes)) {
                byte[] xml = input.readNBytes(1_048_577);
                if (xml.length > 1_048_576) return false;
                String manifest = new String(xml, StandardCharsets.UTF_8);
                return manifest.contains("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml");
            }
        } catch (java.util.zip.ZipException invalidZip) {
            return false;
        }
    }

    private static boolean hasLegacyExcelWorkbookStream(Path path) {
        try (POIFSFileSystem ole2 = new POIFSFileSystem(path.toFile(), true)) {
            return ole2.getRoot().hasEntry("Workbook") || ole2.getRoot().hasEntry("Book");
        } catch (IOException invalidOle2) {
            return false;
        }
    }
}
