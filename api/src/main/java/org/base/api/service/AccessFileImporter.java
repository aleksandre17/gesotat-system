package org.base.api.service;

import com.healthmarketscience.jackcess.*;

import org.base.api.model.request.UploadPayload;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AccessFileImporter {

    private final JdbcTemplate jdbcTemplate;

    public AccessFileImporter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public interface ProgressListener {
        void onProgressUpdate(double progress, String message);
    }


    public static void main(String[] args) {

    }


    private static DataType mapSqlTypeToAccessType(int sqlType) {
        switch (sqlType) {
            case Types.VARCHAR:
            case Types.CHAR:
                return DataType.TEXT;
            case Types.INTEGER:
                return DataType.LONG;
            case Types.BIGINT:
                return DataType.NUMERIC;
            case Types.DOUBLE:
            case Types.FLOAT:
                return DataType.DOUBLE;
            case Types.DATE:
                return DataType.SHORT_DATE_TIME;
            case Types.TIMESTAMP:
                return DataType.SHORT_DATE_TIME;
            case Types.BOOLEAN:
                return DataType.BOOLEAN;
            default:
                return DataType.TEXT; // ნაგულისხმევი
        }
    }

    private String[] parseDatabaseAndTableName(String fileName) {
        if (fileName == null || !fileName.contains("-")) {
            throw new IllegalArgumentException("fileName must be in the format 'database-table'");
        }
        String[] parts = fileName.split("-", 2);
        if (parts.length != 2 || parts[0].isEmpty() || parts[1].isEmpty()) {
            throw new IllegalArgumentException("fileName must be in the format 'database-table'");
        }
        return parts;
    }

    public void parseAndSaveAccessFile(MultipartFile file, UploadPayload payload, ProgressListener listener) throws IOException {

        File tempFile = File.createTempFile("upload-", ".mdb");

        DatabaseImportStrategy strategy = ImportStrategyFactory.getStrategy(payload.getMetaDatabaseType());
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        strategy.configureDataSource(payload, dataSource);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        Connection connection = null;

        try (InputStream is = file.getInputStream();
             FileOutputStream fos = new FileOutputStream(tempFile)) {

            byte[] buffer = new byte[8192];
            long totalBytes = file.getSize();
            long bytesWritten = 0;
            int bytesRead;

            while ((bytesRead = is.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
                bytesWritten += bytesRead;

                if (listener != null && totalBytes > 0) {
                    double progress = ((double) bytesWritten / totalBytes) * 10.0;
                    listener.onProgressUpdate(progress, "Copying file: " + bytesWritten + " of " + totalBytes + " bytes");
                }
            }
        }

        try (Database db = DatabaseBuilder.open(tempFile)) {
            connection = Objects.requireNonNull(jdbcTemplate.getDataSource()).getConnection();
            connection.setAutoCommit(false);

            String rawTableName = db.getTableNames().iterator().next();
            String quotedTableName = strategy.quoteTableName(rawTableName);
            Table table = db.getTable(rawTableName);

            int totalRows = getRowCount(table);
            int processedRows = 0;

            List<String> columnNames = table.getColumns().stream()
                    .map(Column::getName)
                    .collect(Collectors.toList());

            // Clear server data
            if (payload.isClearServerData()) {
                try (PreparedStatement ps = connection.prepareStatement("DELETE FROM " + quotedTableName)) {
                    ps.executeUpdate();
                }
                notify(listener, 10.0, "Cleared all existing server data");
            }

            // Clear data for specific years
            if (payload.getYears() != null && !payload.getYears().isEmpty()) {
                String yearList = payload.getYears().stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(","));
                try (PreparedStatement ps = connection.prepareStatement("DELETE FROM " + quotedTableName + " WHERE [year] IN (" + yearList + ")")) {
                    ps.executeUpdate();
                }
                notify(listener, 10.0, "Cleared data for years: " + yearList);
            }

            final int batchSize = 1000;
            List<Object[]> batch = new ArrayList<>(batchSize);

            for (Row row : table) {
                Object[] values = columnNames.stream().map(row::get).toArray();
                batch.add(values);
                processedRows++;

                if (batch.size() >= batchSize) {
                    batchInsert(connection, quotedTableName, columnNames, batch, listener, totalRows, processedRows, strategy);
                    batch.clear();
                }
            }

            if (!batch.isEmpty()) {
                batchInsert(connection, quotedTableName, columnNames, batch, listener, totalRows, processedRows, strategy);
            }

            connection.commit();
            notify(listener, 100.0, "Upload and insert completed successfully");
        } catch (Exception e) {
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException ex) {
                    notify(listener, 0.0, "Rollback failed: " + ex.getCause().getMessage());
                }
            }
            notify(listener, 0.0, "Upload failed and rolled back: " + e.getCause().getMessage());
            throw new RuntimeException("Upload failed", e);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException ignore) {}
            }
            if (tempFile.exists()) tempFile.delete();
            notify(listener, 100.0, "Temporary file deleted");
        }
    }

    private void notify(ProgressListener listener, double progress, String message) {
        if (listener != null) listener.onProgressUpdate(progress, message);
    }

    private int getRowCount(Table table) {
        int count = 0;
        for (Row ignored : table) {
            count++;
        }
        table.reset();
        return count;
    }

    private String convertToSqlSafeName(String rawName) {
        String[] parts = rawName.split("/");
        StringBuilder sb = new StringBuilder();

        if (parts.length == 2) {
            // For 2 parts: insert [dbo] in the middle
            sb.append("[").append(parts[0]).append("].");
            sb.append("[dbo].");
            sb.append("[").append(parts[1]).append("]");
        } else {
            // For 1 part or 3+ parts: just wrap all parts
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) sb.append(".");
                sb.append("[").append(parts[i]).append("]");
            }
        }
        return sb.toString();
    }

    private void batchInsert(
            Connection connection,
            String tableName,
            List<String> columnNames,
            List<Object[]> batchArgs,
            ProgressListener listener,
            int totalRows,
            int processedRows,
            DatabaseImportStrategy strategy
    ) {

        int batchSize = 500;

        String columns = columnNames.stream()
                .map(strategy::quoteColumn)
                .collect(Collectors.joining(", "));
        String placeholders = columnNames.stream().map(col -> "?").collect(Collectors.joining(", "));
        String sql = "INSERT INTO " + tableName + " (" + columns + ") VALUES (" + placeholders + ")";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            int count = 0;
            connection.setAutoCommit(false);

            for (Object[] args : batchArgs) {
                for (int i = 0; i < args.length; i++) {
                    ps.setObject(i + 1, args[i]);
                }
                ps.addBatch();
                count++;
            }

            if (count % batchSize == 0) {
                count = 0;
                ps.executeBatch();
                ps.clearBatch();
            }

            if (count > 0) {
                ps.executeBatch();
            }

            if (listener != null) {
                double progress = Math.min(10.0 + ((double) processedRows / totalRows) * 90.0, 100.0);
                listener.onProgressUpdate(progress, "Inserted " + batchArgs.size() + " rows (Total: " + processedRows + "/" + totalRows + ")");
            }

        } catch (SQLException e) {
            notify(listener, 10.0, "Batch insert failed: " + e.getMessage());
            throw new RuntimeException("Insert error", e);
        }
    }


    private String getStringValue(Row row, String columnName) {
        if (row == null || columnName == null) return "";
        Object val = row.get(columnName);
        if (val == null) return "";
        return val.toString();
    }

    private int getIntValue(Row row, String columnName) {
        if (row == null || columnName == null) return 0;
        Object val = row.get(columnName);
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        try {
            return Integer.parseInt(val.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private double getDoubleValue(Row row, String columnName) {
        if (row == null || columnName == null) return 0.0;
        Object val = row.get(columnName);
        if (val instanceof Number) {
            return ((Number) val).doubleValue();
        }
        try {
            return Double.parseDouble(val.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }


//    private void batchInsert(String tableName, List<String> columnNames, List<Object[]> batchArgs) {
//        if (batchArgs.isEmpty()) return;
//
//        String columns = columnNames.stream()
//                .map(col -> "[" + col + "]")
//                .collect(Collectors.joining(", "));
//
//        try {
//            // Calculate optimal batch size based on column count to stay under the 2100 parameter limit
//            // Adding a safety margin by using 2000 instead of 2100
//            int maxParamsPerBatch = 2000;
//            int rowsPerBatch = Math.max(1, maxParamsPerBatch / columnNames.size());
//
//            // Use the smaller of the calculated size or the requested batch size
//            int effectiveBatchSize = Math.min(rowsPerBatch, 1000);
//
//            System.out.println("Using batch size: " + effectiveBatchSize + " for " + columnNames.size() + " columns");
//
//            for (int i = 0; i < batchArgs.size(); i += effectiveBatchSize) {
//                List<Object[]> subBatch = batchArgs.subList(i, Math.min(i + effectiveBatchSize, batchArgs.size()));
//                String placeholders = IntStream.range(0, subBatch.size())
//                        .mapToObj(j -> "(" + "?,".repeat(columnNames.size()).replaceAll(",$", "") + ")")
//                        .collect(Collectors.joining(", "));
//                String sql = "INSERT INTO " + tableName + " (" + columns + ") VALUES " + placeholders;
//
//                Object[] flatArgs = subBatch.stream()
//                        .flatMap(Arrays::stream)
//                        .toArray();
//
//                // Debug info, but be careful with large parameter lists
//                System.out.println("Executing SQL with " + subBatch.size() + " rows and " +
//                        flatArgs.length + " parameters");
//
//                jdbcTemplate.update(sql, flatArgs);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            throw new RuntimeException("Failed to insert batch", e);
//        }
//    }



//    private void batchInsertUsingBatchUpdate(String tableName, List<String> columnNames, List<Object[]> batchArgs) {
//        String columns = columnNames.stream()
//                .map(col -> "[" + col + "]")
//                .collect(Collectors.joining(", "));
//        String sql = "INSERT INTO " + tableName + " (" + columns + ") VALUES (" +
//                columnNames.stream().map(c -> "?").collect(Collectors.joining(", ")) + ")";
//
//        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
//            @Override
//            public void setValues(PreparedStatement ps, int i) throws SQLException {
//                Object[] row = batchArgs.get(i);
//                for (int j = 0; j < row.length; j++) {
//                    ps.setObject(j + 1, row[j]);
//                }
//            }
//
//            @Override
//            public int getBatchSize() {
//                return batchArgs.size();
//            }
//        });
//    }
}
