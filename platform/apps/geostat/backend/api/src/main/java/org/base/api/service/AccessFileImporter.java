package org.base.api.service;

import com.healthmarketscience.jackcess.*;

import org.base.api.model.request.UploadPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.sql.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * High-performance dynamic Access (.mdb/.accdb) file importer.
 * <p>
 * Reads any Access table via Jackcess and batch-inserts into SQL Server / MySQL
 * using a single reusable {@link PreparedStatement}. The batch size is automatically
 * calculated to stay within the database parameter limit (e.g. SQL Server's 2100 limit).
 * <p>
 * Progress phases:
 * <ul>
 *   <li>0–5%    — File transfer to temp</li>
 *   <li>5–10%   — Open Access DB, read metadata, optional DELETE</li>
 *   <li>10–95%  — Row iteration + batch inserts (throttled progress updates)</li>
 *   <li>95–100% — Commit + cleanup</li>
 * </ul>
 */
@Service
public class AccessFileImporter {

    private static final Logger log = LoggerFactory.getLogger(AccessFileImporter.class);

    // ─── Progress phase boundaries ───
    private static final double PHASE_FILE_COPY   = 5.0;
    private static final double PHASE_METADATA    = 10.0;
    private static final double PHASE_INSERT_START = 10.0;
    private static final double PHASE_INSERT_END  = 95.0;
    private static final double PHASE_DONE        = 100.0;

    /** Minimum interval (ms) between WebSocket progress messages to prevent flooding. */
    private static final long PROGRESS_THROTTLE_MS = 250;

    /** Buffer size for file copy. */
    private static final int COPY_BUFFER_SIZE = 8192;
    private static final long MAX_IMPORT_BYTES = 1_073_741_824L;

    /** Excel/Access commonly serialises whole-year values as doubles (for example 2025.0). */
    private static final Pattern WHOLE_YEAR_AS_DECIMAL = Pattern.compile("([+-]?\\d+)\\.0+");

    // ─── Public contract ───

    @FunctionalInterface
    public interface ProgressListener {
        void onProgressUpdate(double progress, String message);
    }

    /**
     * Main entry point — parses an uploaded Access file and bulk-inserts into the target DB.
     *
     * @param file     the uploaded .mdb/.accdb file
     * @param payload  import configuration (DB credentials, years to clear, etc.)
     * @param listener progress callback (typically sends WebSocket messages)
     * @throws IOException if file I/O fails
     */
    public void parseAndSaveAccessFile(MultipartFile file,
                                       UploadPayload payload,
                                       ProgressListener listener) throws IOException {

        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Access file is required");
        String original = file.getOriginalFilename();
        if (original == null || !(original.toLowerCase(Locale.ROOT).endsWith(".accdb") || original.toLowerCase(Locale.ROOT).endsWith(".mdb"))) {
            throw new IllegalArgumentException("Only Access database files are supported");
        }
        if (file.getSize() > MAX_IMPORT_BYTES) throw new IllegalArgumentException("Access file exceeds size limit");
        validatePayload(payload);

        // ── Phase 1: Transfer uploaded file to a temp location (0–5%) ──
        File tempFile = File.createTempFile("access-import-", ".accdb");
        try {
            copyToTemp(file, tempFile, listener);
            notify(listener, PHASE_FILE_COPY, "File copied (" + formatBytes(file.getSize()) + ")");

            // ── Phase 2+3+4: Open DB → insert → commit ──
            DatabaseImportStrategy strategy = ImportStrategyFactory.getStrategy(payload.getMetaDatabaseType());
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            strategy.configureDataSource(payload, dataSource);

            long startTime = System.nanoTime();
            importFromAccessFile(tempFile, payload, strategy, dataSource, null, null, listener, startTime);

        } finally {
            if (tempFile.exists() && !tempFile.delete()) {
                log.warn("Failed to delete temp file: {}", tempFile.getAbsolutePath());
            }
        }
    }

    /** Imports an explicitly selected source table into an approved target table. */
    public void parseAndSaveAccessTable(File accessFile, UploadPayload payload,
                                        String sourceTableName, String targetTableName,
                                        ProgressListener listener) {
        validatePayload(payload);
        Objects.requireNonNull(accessFile, "accessFile must not be null");
        Objects.requireNonNull(sourceTableName, "sourceTableName must not be null");
        Objects.requireNonNull(targetTableName, "targetTableName must not be null");
        DatabaseImportStrategy strategy = ImportStrategyFactory.getStrategy(payload.getMetaDatabaseType());
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        strategy.configureDataSource(payload, dataSource);
        importFromAccessFile(accessFile, payload, strategy, dataSource, sourceTableName, targetTableName,
                listener, System.nanoTime());
    }

    // ────────────────────────────────────────────────────────────────────────────
    //  Core import logic
    // ────────────────────────────────────────────────────────────────────────────

    private void importFromAccessFile(File accessFile,
                                      UploadPayload payload,
                                      DatabaseImportStrategy strategy,
                                      DriverManagerDataSource dataSource,
                                      String sourceTableName,
                                      String targetTableName,
                                      ProgressListener listener,
                                      long startNanos) {
        Database db = null;
        Connection connection = null;

        try {
            db = DatabaseBuilder.open(accessFile);
            connection = Objects.requireNonNull(dataSource.getConnection());
            connection.setAutoCommit(false);

            // ── Phase 2: Read metadata (5–10%) ──
            String rawTableName = sourceTableName != null ? sourceTableName : db.getTableNames().iterator().next();
            String quotedTableName = strategy.quoteTableName(targetTableName != null ? targetTableName : rawTableName);
            Table table = db.getTable(rawTableName);
            if (table == null) {
                throw new IllegalArgumentException("Access table not found: " + rawTableName);
            }

            int totalRows = table.getRowCount();
            log.info("Table [{}]: {} rows, {} columns", rawTableName, totalRows, table.getColumnCount());

            List<String> columnNames = table.getColumns().stream()
                    .map(Column::getName)
                    .collect(Collectors.toList());

            // Delete existing data if requested
            executeDeletes(connection, payload, quotedTableName, strategy, listener);

            notify(listener, PHASE_METADATA, "Starting insert of " + totalRows + " rows…");

            // ── Phase 3: Batch insert (10–95%) ──
            int effectiveBatchSize = computeEffectiveBatchSize(columnNames.size(), strategy);
            log.info("Batch size: {} (columns: {}, maxParams: {})",
                    effectiveBatchSize, columnNames.size(), strategy.getMaxParamsPerBatch());

            executeBatchInsert(connection, quotedTableName, columnNames, table, totalRows,
                    effectiveBatchSize, strategy, listener);

            // ── Phase 4: Commit (95–100%) ──
            notify(listener, PHASE_INSERT_END, "Committing transaction…");
            connection.commit();

            long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
            notify(listener, PHASE_DONE, "Completed: " + totalRows + " rows in " + elapsedMs + "ms");
            log.info("Import completed: {} rows in {}ms → {}", totalRows, elapsedMs, rawTableName);

        } catch (Exception e) {
            rollbackQuietly(connection);
            notify(listener, 0.0, "Upload failed; contact support if the problem persists.");
            log.error("Import failed for file: {}", accessFile.getName(), e);
            throw new RuntimeException("Import failed", e);
        } finally {
            closeQuietly(db);
            closeQuietly(connection);
        }
    }

    // ────────────────────────────────────────────────────────────────────────────
    //  Batch insert
    // ────────────────────────────────────────────────────────────────────────────

    /**
     * Reads rows from Jackcess table and inserts via a single reusable PreparedStatement.
     * Progress updates are throttled to avoid WebSocket flooding.
     */
    private void executeBatchInsert(Connection connection,
                                    String quotedTableName,
                                    List<String> columnNames,
                                    Table table,
                                    int totalRows,
                                    int batchSize,
                                    DatabaseImportStrategy strategy,
                                    ProgressListener listener) throws SQLException {

        String sql = buildInsertSql(quotedTableName, columnNames, strategy);
        int columnCount = columnNames.size();

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            int processedRows = 0;
            int pendingInBatch = 0;
            long lastProgressTime = System.currentTimeMillis();

            for (Row row : table) {
                // Set parameters directly — no intermediate Object[] allocation
                for (int i = 0; i < columnCount; i++) {
                    String columnName = columnNames.get(i);
                    ps.setObject(i + 1, normalizeYearValue(columnName, row.get(columnName)));
                }
                ps.addBatch();
                pendingInBatch++;
                processedRows++;

                if (pendingInBatch >= batchSize) {
                    ps.executeBatch();
                    ps.clearBatch();
                    pendingInBatch = 0;

                    // Throttled progress update — avoids flooding WebSocket
                    long now = System.currentTimeMillis();
                    if (now - lastProgressTime >= PROGRESS_THROTTLE_MS) {
                        reportInsertProgress(listener, processedRows, totalRows);
                        lastProgressTime = now;
                    }
                }
            }

            // Flush remaining rows
            if (pendingInBatch > 0) {
                ps.executeBatch();
                ps.clearBatch();
            }

            // Final progress update (always send)
            reportInsertProgress(listener, processedRows, totalRows);
        }
    }

    // ────────────────────────────────────────────────────────────────────────────
    //  Delete operations
    // ────────────────────────────────────────────────────────────────────────────

    /**
     * Executes optional DELETE statements — either clear all data or specific years.
     * Short-circuits: if clearServerData is true, year-based delete is skipped.
     */
    private void executeDeletes(Connection connection,
                                UploadPayload payload,
                                String quotedTableName,
                                DatabaseImportStrategy strategy,
                                ProgressListener listener) throws SQLException {

        if (payload.isClearServerData()) {
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM " + quotedTableName)) {
                int deleted = ps.executeUpdate();
                notify(listener, 8.0, "Cleared all server data (" + deleted + " rows)");
            }
            return; // No need to also delete by year
        }

        List<String> years = payload.getYears();
        if (years != null && !years.isEmpty()) {
            String yearColumn = strategy.quoteColumn("year");
            // Build parameterized DELETE to prevent SQL injection
            String placeholders = years.stream().map(y -> "?").collect(Collectors.joining(","));
            String sql = "DELETE FROM " + quotedTableName + " WHERE " + yearColumn + " IN (" + placeholders + ")";

            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                for (int i = 0; i < years.size(); i++) {
                    ps.setString(i + 1, years.get(i));
                }
                int deleted = ps.executeUpdate();
                notify(listener, 8.0, "Cleared data for years [" + String.join(", ", years) + "] (" + deleted + " rows)");
            }
        }
    }

    // ────────────────────────────────────────────────────────────────────────────
    //  SQL building
    // ────────────────────────────────────────────────────────────────────────────

    /**
     * Builds a parameterized INSERT statement for the given table and columns.
     */
    private String buildInsertSql(String quotedTableName, List<String> columnNames, DatabaseImportStrategy strategy) {
        String columns = columnNames.stream()
                .map(strategy::quoteColumn)
                .collect(Collectors.joining(", "));
        String placeholders = columnNames.stream()
                .map(c -> "?")
                .collect(Collectors.joining(", "));
        return "INSERT INTO " + quotedTableName + " (" + columns + ") VALUES (" + placeholders + ")";
    }

    /**
     * Computes optimal batch size based on column count and DB parameter limit.
     * SQL Server: hard limit of 2100 parameters per statement.
     * MySQL: much higher; rewriteBatchedStatements handles optimization.
     */
    private int computeEffectiveBatchSize(int columnCount, DatabaseImportStrategy strategy) {
        int maxParams = strategy.getMaxParamsPerBatch();
        return Math.max(1, maxParams / columnCount);
    }

    // ────────────────────────────────────────────────────────────────────────────
    //  File I/O helpers
    // ────────────────────────────────────────────────────────────────────────────

    private void copyToTemp(MultipartFile file, File tempFile, ProgressListener listener) throws IOException {
        long totalBytes = file.getSize();
        long bytesWritten = 0;

        try (InputStream is = file.getInputStream();
             OutputStream os = Files.newOutputStream(tempFile.toPath())) {

            byte[] buffer = new byte[COPY_BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
                bytesWritten += bytesRead;

                if (totalBytes > 0) {
                    double progress = ((double) bytesWritten / totalBytes) * PHASE_FILE_COPY;
                    notify(listener, progress, "Copying file: " + formatBytes(bytesWritten) + " / " + formatBytes(totalBytes));
                }
            }
        }
    }

    // ────────────────────────────────────────────────────────────────────────────
    //  Progress & utility helpers
    // ────────────────────────────────────────────────────────────────────────────

    private void reportInsertProgress(ProgressListener listener, int processedRows, int totalRows) {
        double progress = PHASE_INSERT_START
                + ((double) processedRows / Math.max(totalRows, 1)) * (PHASE_INSERT_END - PHASE_INSERT_START);
        notify(listener,
                Math.min(progress, PHASE_INSERT_END),
                "Inserted " + processedRows + " / " + totalRows + " rows");
    }

    private void notify(ProgressListener listener, double progress, String message) {
        if (listener != null) {
            listener.onProgressUpdate(progress, message);
        }
    }

    /**
     * Access exposes numeric columns as {@link Double}, even when the source value is a whole
     * year.  Passing that value through directly can persist it as "2025.0" in a text or
     * decimal target column.  Only lossless, whole-number values in year-related columns are
     * converted; values such as 2025.5 remain untouched.
     */
    static Object normalizeYearValue(String columnName, Object value) {
        if (value == null || columnName == null || !columnName.toLowerCase(Locale.ROOT).contains("year")) {
            return value;
        }

        if (value instanceof String text) {
            var matcher = WHOLE_YEAR_AS_DECIMAL.matcher(text.trim());
            return matcher.matches() ? matcher.group(1) : value;
        }

        if (value instanceof BigDecimal decimal) {
            try {
                return decimal.longValueExact();
            } catch (ArithmeticException ignored) {
                return value;
            }
        }

        if (value instanceof Number number) {
            double numericValue = number.doubleValue();
            if (Double.isFinite(numericValue) && numericValue == Math.rint(numericValue)
                    && numericValue >= Long.MIN_VALUE && numericValue <= Long.MAX_VALUE) {
                return (long) numericValue;
            }
        }
        return value;
    }

    private void validatePayload(UploadPayload payload) {
        Objects.requireNonNull(payload, "UploadPayload must not be null");
        Objects.requireNonNull(payload.getMetaDatabaseType(), "metaDatabaseType must not be null");
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private static void rollbackQuietly(Connection connection) {
        if (connection != null) {
            try {
                connection.rollback();
            } catch (SQLException ex) {
                log.warn("Rollback failed", ex);
            }
        }
    }

    private static void closeQuietly(AutoCloseable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (Exception e) {
                log.warn("Failed to close resource", e);
            }
        }
    }
}

