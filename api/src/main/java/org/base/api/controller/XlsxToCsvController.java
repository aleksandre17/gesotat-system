package org.base.api.controller;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/xlsx-to-csv")
@CrossOrigin(origins = "*", exposedHeaders = {HttpHeaders.CONTENT_DISPOSITION})
public class XlsxToCsvController {

    static class MergedCellInfo {
        int firstRow, lastRow, firstCol, lastCol;
        String value;

        MergedCellInfo(int firstRow, int lastRow, int firstCol, int lastCol, String value) {
            this.firstRow = firstRow;
            this.lastRow = lastRow;
            this.firstCol = firstCol;
            this.lastCol = lastCol;
            this.value = value;
        }

        boolean contains(int row, int col) {
            return row >= firstRow && row <= lastRow && col >= firstCol && col <= lastCol;
        }

        boolean isTopLeft(int row, int col) {
            return row == firstRow && col == firstCol;
        }
    }

    /**
     * Convert a specific sheet
     * GET /xlsx-to-csv/convert?sheet=0
     * GET /xlsx-to-csv/convert?url=https://example.com/file.xlsx&sheet=0
     */
    @GetMapping(value = "/convert", produces = "text/csv; charset=UTF-8")
    public ResponseEntity<StreamingResponseBody> convertFromUrl(
            @RequestParam(value = "url", required = false) String fileUrl,
            @RequestParam(value = "sheet", required = false) Integer sheetIndex) throws IOException {
    
        Path tempFile;
        String baseName;
        boolean deleteAfter;
    
        if (fileUrl == null || fileUrl.isEmpty()) {
            // Try to load from classpath first (works in JAR)
            tempFile = loadDefaultFile();
            baseName = "პროდუქციის-გამოშვება";
            deleteAfter = false;
        } else {
            tempFile = downloadFile(fileUrl);
            baseName = extractFileNameFromUrl(fileUrl);
            deleteAfter = true;
        }
    
        int sheet = sheetIndex != null ? sheetIndex : 0;
        return convertSpecificSheet(tempFile, baseName, sheet, deleteAfter);
    }
    
    /**
     * Load default file from classpath or filesystem
     */
    private Path loadDefaultFile() throws IOException {
        String resourcePath = "storage/input/Product Release.xlsx";
    
        // Try classpath first (for JAR deployment)
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is != null) {
                Path tempFile = Files.createTempFile("default_xlsx_", ".xlsx");
                Files.copy(is, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                return tempFile;
            }
        }
    
        // Fallback to filesystem paths (for development)
        Path[] possiblePaths = {
            Paths.get("api/src/main/resources/storage/input/Product Release.xlsx").toAbsolutePath(),
            Paths.get("src/main/resources/storage/input/Product Release.xlsx").toAbsolutePath(),
            Paths.get("api/src/main/storage/input/Product Release.xlsx").toAbsolutePath(),
            Paths.get("src/main/storage/input/Product Release.xlsx").toAbsolutePath()
        };
    
        for (Path path : possiblePaths) {
            if (Files.exists(path)) {
                return path;
            }
        }
    
        throw new FileNotFoundException("Default file not found. Ensure the file is placed in the resources/storage/input/ directory");
    }

    /**
     * Download all sheets - HTML page (for iframe display)
     * GET /xlsx-to-csv/download-all
     * GET /xlsx-to-csv/download-all?url=https://example.com/file.xlsx
     */
    @GetMapping(value = "/download-all", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> downloadAllSheets(
            @RequestParam(value = "url", required = false) String fileUrl) throws IOException {
    
        Path filePath;
        String displayFileName;
        boolean isLocalFile;
        String urlForLinks;
        boolean deleteTempFile = false;
    
        if (fileUrl == null || fileUrl.isEmpty()) {
            filePath = loadDefaultFile();
            displayFileName = "Product Release";
            urlForLinks = null;
            isLocalFile = true;
            // Check if it's a temp file (from classpath loading)
            deleteTempFile = filePath.toString().contains("default_xlsx_");
        } else {
            filePath = downloadFile(fileUrl);
            displayFileName = extractFileNameFromUrl(fileUrl);
            urlForLinks = fileUrl;
            isLocalFile = false;
        }
    
        try (InputStream fis = Files.newInputStream(filePath);
             Workbook workbook = WorkbookFactory.create(fis)) {
    
            int sheetCount = workbook.getNumberOfSheets();
            String encodedUrl = urlForLinks != null ? URLEncoder.encode(urlForLinks, StandardCharsets.UTF_8) : null;
    
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
            html.append("<title>CSV Download</title>");
            html.append("<style>");
            html.append("* { margin: 0; padding: 0; box-sizing: border-box; }");
            html.append("body { font-family: 'Segoe UI', Arial, sans-serif;  min-height: 100vh; display: flex; align-items: center; justify-content: center; padding: 20px; }");
            html.append(".popup { background: white; border-radius: 16px; box-shadow: 0 25px 50px -12px rgba(0,0,0,0.25); width: 100%; max-width: 420px; overflow: hidden; }");
            html.append(".popup-header { background: #0080be; color: white; padding: 20px 24px; }");
            html.append(".popup-header h1 { font-size: 20px; font-weight: 600; }");
            html.append(".popup-header p { font-size: 13px; opacity: 0.9; margin-top: 6px; }");
            html.append(".popup-body { padding: 20px 24px; max-height: 350px; overflow-y: auto; }");
            html.append(".sheet { display: flex; align-items: center; justify-content: space-between; padding: 12px 16px; margin: 8px 0; background: #f8f9fa; border-radius: 10px; border-left: 4px solid #9e9e9e; transition: all 0.3s ease; }");
            html.append(".sheet.downloading { border-left-color: #ff9800; background: #fff8e1; }");
            html.append(".sheet.done { border-left-color: #0080be; background: #e8f5e9; }");
            html.append(".sheet-name { font-weight: 500; color: #333; font-size: 14px; }");
            html.append(".sheet-status { font-size: 12px; color: #667; }");
            html.append(".popup-footer { padding: 16px 24px; background: #f8f9fa; border-top: 1px solid #eee; }");
            html.append(".progress-bar { height: 6px; background: #e0e0e0; border-radius: 3px; overflow: hidden; }");
            html.append(".progress-fill { height: 100%; background: linear-gradient(90deg, #0080be, #0080be); width: 0%; transition: width 0.3s ease; }");
            html.append(".status-text { text-align: center; margin-top: 12px; font-size: 13px; color: #667; }");
            html.append(".status-text.complete { color: #0080be; font-weight: 600; }");
            html.append("</style></head><body>");
            html.append("<div class='popup'>");
            html.append("<div class='popup-header'>");
//            html.append("<h1>📥 CSV ექსპორტი</h1>");
            html.append("<p>").append(displayFileName).append(" • ").append(sheetCount).append(" sheets</p>");
            html.append("</div>");
            html.append("<div class='popup-body'>");
            
            for (int i = 0; i < sheetCount; i++) {
                String sheetName = workbook.getSheetAt(i).getSheetName();
                html.append("<div class='sheet' id='sheet-").append(i).append("'>");
                html.append("<span class='sheet-name'>📄 ").append(sheetName).append("</span>");
                html.append("<span class='sheet-status' id='status-").append(i).append("'>Pending</span>");
                html.append("</div>");
            }
            
            html.append("</div>");
            html.append("<div class='popup-footer'>");
            html.append("<div class='progress-bar'><div class='progress-fill' id='progress'></div></div>");
            html.append("<p class='status-text' id='final-status'>⏳ Downloading in progress...</p>");
            html.append("</div></div>");

            html.append("<script>");
            html.append("const sheets = [");
            for (int i = 0; i < sheetCount; i++) {
                String sheetName = workbook.getSheetAt(i).getSheetName();
                html.append("{index:").append(i);
                html.append(",name:'").append(sheetName.replace("'", "\\'")).append("'");
                if (encodedUrl != null) {
                    html.append(",url:'/api/v1/xlsx-to-csv/convert?url=").append(encodedUrl).append("&sheet=").append(i).append("'}");
                } else {
                    html.append(",url:'/api/v1/xlsx-to-csv/convert?sheet=").append(i).append("'}");
                }
                if (i < sheetCount - 1) html.append(",");
            }
            html.append("];");
            html.append("const total = sheets.length;");
            html.append("let currentIndex = 0;");
            html.append("function updateProgress() { document.getElementById('progress').style.width = Math.round((currentIndex / total) * 100) + '%'; }");

            // Fetch-based download function
            html.append("async function downloadFile(sheet) {");
            html.append("  try {");
            html.append("    const response = await fetch(sheet.url);");
            html.append("    if (!response.ok) { console.error('HTTP error:', response.status); return false; }");
            html.append("    const blob = await response.blob();");
            html.append("    const contentDisposition = response.headers.get('Content-Disposition');");
            html.append("    let filename = sheet.name + '.csv';");
            html.append("    if (contentDisposition) {");
            html.append("      const utf8Match = contentDisposition.match(/filename\\*=UTF-8''([^;\\s]+)/i);");
            html.append("      if (utf8Match) { filename = decodeURIComponent(utf8Match[1]); }");
            html.append("      else {");
            html.append("        const match = contentDisposition.match(/filename=\"([^\"]+)\"/i);");
            html.append("        if (match) { filename = match[1]; }");
            html.append("      }");
            html.append("    }");
            html.append("    const blobUrl = window.URL.createObjectURL(blob);");
            html.append("    const link = document.createElement('a');");
            html.append("    link.href = blobUrl;");
            html.append("    link.download = filename;");
            html.append("    document.body.appendChild(link);");
            html.append("    link.click();");
            html.append("    document.body.removeChild(link);");
            html.append("    window.URL.revokeObjectURL(blobUrl);");
            html.append("    return true;");
            html.append("  } catch (e) { console.error('Download error:', e); return false; }");
            html.append("}");

            html.append("async function downloadNext() {");
            html.append("  if (currentIndex >= sheets.length) {");
            html.append("    document.getElementById('final-status').innerHTML = 'All files have been downloaded!';");
            html.append("    document.getElementById('final-status').className = 'status-text complete';");
            html.append("    document.getElementById('progress').style.width = '100%';");
            html.append("    return;");
            html.append("  }");
            html.append("  const sheet = sheets[currentIndex];");
            html.append("  document.getElementById('sheet-' + sheet.index).className = 'sheet downloading';");
            html.append("  document.getElementById('status-' + sheet.index).innerHTML = 'Downloading...';");
            html.append("  document.getElementById('final-status').innerHTML = '⏳ ' + (currentIndex + 1) + '/' + total + ' - ' + sheet.name;");
            html.append("  await downloadFile(sheet);");
            html.append("  document.getElementById('sheet-' + sheet.index).className = 'sheet done';");
            html.append("  document.getElementById('status-' + sheet.index).innerHTML = '✓ Ready';");
            html.append("  currentIndex++;");
            html.append("  updateProgress();");
            html.append("  setTimeout(downloadNext, 800);");
            html.append("}");
            html.append("setTimeout(downloadNext, 500);");
            html.append("</script></body></html>");

            return ResponseEntity.ok()
                    .header("X-Frame-Options", "ALLOWALL")
                    .body(html.toString());
        } finally {
            if (!isLocalFile || deleteTempFile) {
                Files.deleteIfExists(filePath);
            }
        }
    }

    /**
     * ყველა შიტის ZIP არქივად ჩამოტვირთვა
     * GET /xlsx-to-csv/download-zip
     * GET /xlsx-to-csv/download-zip?url=https://example.com/file.xlsx
     */
    @GetMapping(value = "/download-zip", produces = "application/zip")
    public ResponseEntity<StreamingResponseBody> downloadAllSheetsAsZip(
            @RequestParam(value = "url", required = false) String fileUrl) throws IOException {

        Path filePath;
        String baseName;
        boolean deleteAfter;

        if (fileUrl == null || fileUrl.isEmpty()) {
            filePath = loadDefaultFile();
            baseName = "პროდუქციის-გამოშვება";
            deleteAfter = filePath.toString().contains("default_xlsx_");
        } else {
            filePath = downloadFile(fileUrl);
            baseName = extractFileNameFromUrl(fileUrl);
            deleteAfter = true;
        }

        String zipFileName = baseName + "_csv.zip";
        String asciiZipName = transliterate(baseName) + "_csv.zip";
        String encodedZipName = URLEncoder.encode(zipFileName, StandardCharsets.UTF_8).replace("+", "%20");

        final Path finalFilePath = filePath;
        final boolean finalDeleteAfter = deleteAfter;

        StreamingResponseBody stream = outputStream -> {
            try (InputStream fis = Files.newInputStream(finalFilePath);
                 Workbook workbook = WorkbookFactory.create(fis);
                 java.util.zip.ZipOutputStream zipOut = new java.util.zip.ZipOutputStream(outputStream)) {

                zipOut.setLevel(java.util.zip.Deflater.DEFAULT_COMPRESSION);

                int sheetCount = workbook.getNumberOfSheets();
                for (int i = 0; i < sheetCount; i++) {
                    Sheet sheet = workbook.getSheetAt(i);
                    String sheetName = sheet.getSheetName();
                    String csvFileName = sheetCount > 1
                            ? baseName + "_" + sheetName + ".csv"
                            : baseName + ".csv";

                    java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry(csvFileName);
                    zipOut.putNextEntry(entry);

                    // Write BOM for UTF-8
                    zipOut.write(0xEF);
                    zipOut.write(0xBB);
                    zipOut.write(0xBF);

                    // Convert sheet to CSV
                    OutputStreamWriter writer = new OutputStreamWriter(zipOut, StandardCharsets.UTF_8);
                    convertSheetToCsv(sheet, writer, workbook);
                    writer.flush();

                    zipOut.closeEntry();
                }
            } finally {
                if (finalDeleteAfter) {
                    Files.deleteIfExists(finalFilePath);
                }
            }
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + asciiZipName + "\"; filename*=UTF-8''" + encodedZipName)
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(stream);
    }

    /**
     * Upload Excel file and convert to CSV
     */
    @PostMapping("/upload")
    public ResponseEntity<StreamingResponseBody> uploadAndConvert(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "sheet", required = false) Integer sheetIndex) throws IOException {
    
        if (file.isEmpty()) {
            throw new IllegalArgumentException("The file has not been uploaded");
        }
    
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || (!originalFilename.endsWith(".xlsx") && !originalFilename.endsWith(".xls"))) {
            throw new IllegalArgumentException("Only .xlsx or .xls files are allowed");
        }

        String baseName = originalFilename.replaceFirst("[.][^.]+$", "");
        Path tempFile = Files.createTempFile("xlsx_upload_", ".xlsx");
        file.transferTo(tempFile.toFile());

        int sheet = sheetIndex != null ? sheetIndex : 0;
        return convertSpecificSheet(tempFile, baseName, sheet, true);
    }

    private Path downloadFile(String fileUrl) throws IOException {
        try {
            URL url = URI.create(fileUrl).toURL();
            Path tempFile = Files.createTempFile("xlsx_download_", ".xlsx");
            try (InputStream in = url.openStream()) {
                Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }
            return tempFile;
        } catch (Exception e) {
            throw new IOException("Failed to download the file: " + fileUrl, e);
        }
    }

    private String extractFileNameFromUrl(String fileUrl) {
        try {
            String decodedUrl = URLDecoder.decode(fileUrl, StandardCharsets.UTF_8);
            String fileName = decodedUrl.substring(decodedUrl.lastIndexOf('/') + 1);
            if (fileName.contains("?")) fileName = fileName.substring(0, fileName.indexOf('?'));
            if (fileName.contains(".")) fileName = fileName.substring(0, fileName.lastIndexOf('.'));
            return fileName.isEmpty() ? "converted" : fileName;
        } catch (Exception e) {
            return "converted";
        }
    }

    private ResponseEntity<StreamingResponseBody> convertSpecificSheet(
            Path xlsxPath, String baseName, int sheetIndex, boolean deleteTempFile) throws IOException {

        String csvFileName;
        String asciiFileName;

        try (InputStream fis = Files.newInputStream(xlsxPath);
             Workbook workbook = WorkbookFactory.create(fis)) {
            String sheetName = workbook.getSheetAt(sheetIndex).getSheetName();
            int totalSheets = workbook.getNumberOfSheets();
            if (totalSheets > 1) {
                csvFileName = baseName + "_" + sheetName + ".csv";
                asciiFileName = transliterate(baseName + "_" + sheetName) + ".csv";
            } else {
                csvFileName = baseName + ".csv";
                asciiFileName = transliterate(baseName) + ".csv";
            }
        }

        String encodedFileName = URLEncoder.encode(csvFileName, StandardCharsets.UTF_8).replace("+", "%20");

        StreamingResponseBody stream = outputStream -> {
            try (InputStream fis = Files.newInputStream(xlsxPath);
                 Workbook workbook = WorkbookFactory.create(fis);
                 OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {

                outputStream.write(0xEF);
                outputStream.write(0xBB);
                outputStream.write(0xBF);

                Sheet sheet = workbook.getSheetAt(sheetIndex);
                convertSheetToCsv(sheet, writer, workbook);
                writer.flush();
            } finally {
                if (deleteTempFile) Files.deleteIfExists(xlsxPath);
            }
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + asciiFileName + "\"; filename*=UTF-8''" + encodedFileName)
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(stream);
    }

    private void convertSheetToCsv(Sheet sheet, Writer writer, Workbook workbook) throws IOException {
        List<MergedCellInfo> mergedCells = analyzeMergedCells(sheet);
        int[] dataRange = findDataRange(sheet);
        int firstRow = dataRange[0], lastRow = dataRange[1];
        int firstCol = dataRange[2], lastCol = dataRange[3];

        if (firstRow == Integer.MAX_VALUE) return;

        DataFormatter formatter = new DataFormatter();
        FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
        CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT);

        for (int rowNum = firstRow; rowNum <= lastRow; rowNum++) {
            Row row = sheet.getRow(rowNum);
            List<String> rowValues = new ArrayList<>();
            for (int colNum = firstCol; colNum <= lastCol; colNum++) {
                rowValues.add(getCellValueWithMerge(row, rowNum, colNum, mergedCells, formatter, evaluator));
            }
            printer.printRecord(rowValues);
        }
    }

    private List<MergedCellInfo> analyzeMergedCells(Sheet sheet) {
        List<MergedCellInfo> result = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();
        for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
            CellRangeAddress region = sheet.getMergedRegion(i);
            String value = "";
            Row firstRow = sheet.getRow(region.getFirstRow());
            if (firstRow != null) {
                Cell firstCell = firstRow.getCell(region.getFirstColumn());
                if (firstCell != null) value = formatter.formatCellValue(firstCell);
            }
            result.add(new MergedCellInfo(region.getFirstRow(), region.getLastRow(),
                    region.getFirstColumn(), region.getLastColumn(), value));
        }
        return result;
    }

    private int[] findDataRange(Sheet sheet) {
        int firstRow = Integer.MAX_VALUE, lastRow = 0, firstCol = Integer.MAX_VALUE, lastCol = 0;
        for (int rowNum = 0; rowNum <= sheet.getLastRowNum(); rowNum++) {
            Row row = sheet.getRow(rowNum);
            if (row == null) continue;
            for (Cell cell : row) {
                if (!isCellEmpty(cell)) {
                    firstRow = Math.min(firstRow, rowNum);
                    lastRow = Math.max(lastRow, rowNum);
                    firstCol = Math.min(firstCol, cell.getColumnIndex());
                    lastCol = Math.max(lastCol, cell.getColumnIndex());
                }
            }
        }
        for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
            CellRangeAddress region = sheet.getMergedRegion(i);
            if (firstRow != Integer.MAX_VALUE) {
                firstRow = Math.min(firstRow, region.getFirstRow());
                lastRow = Math.max(lastRow, region.getLastRow());
                firstCol = Math.min(firstCol, region.getFirstColumn());
                lastCol = Math.max(lastCol, region.getLastColumn());
            }
        }
        return new int[]{firstRow, lastRow, firstCol, lastCol};
    }

    private String getCellValueWithMerge(Row row, int rowNum, int colNum, List<MergedCellInfo> mergedCells,
                                         DataFormatter formatter, FormulaEvaluator evaluator) {
        for (MergedCellInfo merged : mergedCells) {
            if (merged.contains(rowNum, colNum)) {
                return merged.isTopLeft(rowNum, colNum) ? merged.value : "";
            }
        }
        if (row == null) return "";
        Cell cell = row.getCell(colNum, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        return cell == null ? "" : formatCellValue(cell, formatter, evaluator);
    }

    private String formatCellValue(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator) {
        try {
            CellType cellType = cell.getCellType();
            if (cellType == CellType.FORMULA) {
                CellValue evaluated = evaluator.evaluate(cell);
                if (evaluated == null) return "";
                return switch (evaluated.getCellType()) {
                    case NUMERIC -> formatNumber(evaluated.getNumberValue(), cell);
                    case STRING -> evaluated.getStringValue();
                    case BOOLEAN -> String.valueOf(evaluated.getBooleanValue());
                    default -> "";
                };
            }
            if (cellType == CellType.NUMERIC) {
                return DateUtil.isCellDateFormatted(cell) ? formatter.formatCellValue(cell) : formatNumber(cell.getNumericCellValue(), cell);
            }
            return formatter.formatCellValue(cell);
        } catch (Exception e) {
            return "";
        }
    }

    private String formatNumber(double value, Cell cell) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) return String.valueOf((long) value);
        String format = cell.getCellStyle().getDataFormatString();
        if (format != null && !format.equals("General")) return new DataFormatter().formatCellValue(cell);
        return String.valueOf(value);
    }

    private boolean isCellEmpty(Cell cell) {
        if (cell == null) return true;
        if (cell.getCellType() == CellType.BLANK) return true;
        return cell.getCellType() == CellType.STRING && cell.getStringCellValue().trim().isEmpty();
    }

    private String transliterate(String text) {
        if (text == null) return "";
        Map<Character, String> map = Map.ofEntries(
                Map.entry('ა', "a"), Map.entry('ბ', "b"), Map.entry('გ', "g"), Map.entry('დ', "d"),
                Map.entry('ე', "e"), Map.entry('ვ', "v"), Map.entry('ზ', "z"), Map.entry('თ', "t"),
                Map.entry('ი', "i"), Map.entry('კ', "k"), Map.entry('ლ', "l"), Map.entry('მ', "m"),
                Map.entry('ნ', "n"), Map.entry('ო', "o"), Map.entry('პ', "p"), Map.entry('ჟ', "zh"),
                Map.entry('რ', "r"), Map.entry('ს', "s"), Map.entry('ტ', "t"), Map.entry('უ', "u"),
                Map.entry('ფ', "f"), Map.entry('ქ', "q"), Map.entry('ღ', "gh"), Map.entry('ყ', "y"),
                Map.entry('შ', "sh"), Map.entry('ჩ', "ch"), Map.entry('ც', "ts"), Map.entry('ძ', "dz"),
                Map.entry('წ', "ts"), Map.entry('ჭ', "ch"), Map.entry('ხ', "kh"), Map.entry('ჯ', "j"),
                Map.entry('ჰ', "h")
        );
        StringBuilder result = new StringBuilder();
        for (char c : text.toCharArray()) result.append(map.getOrDefault(c, String.valueOf(c)));
        return result.toString();
    }
}

