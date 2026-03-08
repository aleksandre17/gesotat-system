package org.base.api.controller;

import com.healthmarketscience.jackcess.*;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.base.core.anotation.Api;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/import")
@Api
public class MSSQLToAccess {

    @Value("${storage.export-dir}")
    private String exportDir;

    private Path getExportDir(boolean empty) {
        return Paths.get(exportDir, empty ? "empty" : "data");
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

    // Helper to get the correct file path
    private Path getAccessFilePath(String fileName, boolean empty) {
        String[] dbAndTable = parseDatabaseAndTableName(fileName);
        String baseName = dbAndTable[0] + "-" + dbAndTable[1] + ".accdb";
        return getExportDir(empty).resolve(fileName.endsWith(".accdb") ? fileName : fileName + ".accdb");
    }

    @GetMapping("/mssql-to-access")
    public ResponseEntity<byte[]> importAccess(
            @RequestParam()
            @Pattern(regexp = "^[^-]+-.*$", message = "fileName must be in the format 'database-tablename' where tablename")
            String fileName,
            String metaDatabaseType,
            String metaDatabaseUrl,
            String metaDatabaseUser,
            String metaDatabasePassword,
            @RequestParam(defaultValue = "false") boolean empty) throws IOException {

        Files.createDirectories(getExportDir(empty));
        Path accessFilePath = getAccessFilePath(fileName, empty);

        if (!Files.exists(accessFilePath)) {
            generateAccess(metaDatabaseType, fileName, metaDatabaseUrl, metaDatabaseUser, metaDatabasePassword, empty);
        }

        byte[] fileContent = Files.readAllBytes(accessFilePath);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + accessFilePath.getFileName().toString() + "\"")
                .header("Content-Type", "application/octet-stream")
                .body(fileContent);
    }

    private String buildSelectQuery(String metaDatabaseType, String databaseName, String tableName, String metaDatabaseUrl) {
        if (metaDatabaseType.toLowerCase().contains("mssql")) {
            // SQL Server uses square brackets and explicit schema (dbo)
            return "SELECT * FROM [" + databaseName + "].dbo.[" + tableName + "]";
        } else if (metaDatabaseType.toLowerCase().contains("mysql")) {
            // MySQL uses backticks and database.table format
            return "SELECT * FROM `" + databaseName + "`.`" + tableName + "`";
        } else {
            throw new IllegalArgumentException("Unsupported database type");
        }
    }


    private String buildConnectionUrl(String metaDatabaseType, String metaDatabaseUrl, String databaseName) {
        String type = metaDatabaseType.toLowerCase();

        if (type.contains("mssql") || type.contains("sqlserver")) {
            return "jdbc:sqlserver://" + metaDatabaseUrl +
                    ";databaseName=" + databaseName +
                    ";encrypt=true;useBulkCopyForBatchInsert=true;" +
                    "trustServerCertificate=true;" +
                    "serverTimezone=Asia/Tbilisi;" +
                    "cachePrepStmts=true;reWriteBatchedInserts=true";
        } else if (type.contains("mysql")) {
            return "jdbc:mysql://" + metaDatabaseUrl +
                    "/" + databaseName +
                    "?useSSL=false&serverTimezone=Asia/Tbilisi" +
                    "&cachePrepStmts=true&reWriteBatchedInserts=true";
        } else {
            throw new IllegalArgumentException("Unsupported database type: " + metaDatabaseType);
        }
    }

    @PostMapping("/mssql-to-access")
    public void generateAccess(String metaDatabaseType,
                               @RequestParam String fileName,
                               String metaDatabaseUrl,
                               String metaDatabaseUser,
                               String metaDatabasePassword,  @RequestParam(defaultValue = "false") boolean empty) {

        //String mssqlDatabaseName = "auto";
        //String mssqlTableName = fileName;

        String[] dbAndTable = parseDatabaseAndTableName(fileName);
        String mssqlDatabaseName = dbAndTable[0];
        String mssqlTableName = dbAndTable[1];

        if (mssqlDatabaseName.equalsIgnoreCase("international_ratings")) {
            mssqlDatabaseName = "international-ratings";
        }

        String mssqlUrl = buildConnectionUrl(metaDatabaseType, metaDatabaseUrl, mssqlDatabaseName);
        String mssqlUser = metaDatabaseUser;
        String mssqlPassword = metaDatabasePassword;
        String accessTableName = mssqlDatabaseName.replace("-", "_") + "/" + mssqlTableName;

        try {
            Files.createDirectories(getExportDir(empty));
            Path accessFilePath = getAccessFilePath(fileName, empty);

            if (Files.exists(accessFilePath)) {
                Files.delete(accessFilePath);
            }

            Connection mssqlConn = DriverManager.getConnection(mssqlUrl, mssqlUser, mssqlPassword);
            DatabaseMetaData metaData = mssqlConn.getMetaData();
            ResultSet columns = metaData.getColumns(null, "dbo", mssqlTableName, null);

            List<ColumnBuilder> columnBuilders = new ArrayList<>();
            while (columns.next()) {
                String columnName = columns.getString("COLUMN_NAME");
                int sqlType = columns.getInt("DATA_TYPE");
                if (columnName.equalsIgnoreCase("id")) continue;
                DataType accessType = mapSqlTypeToAccessType(sqlType);
                columnBuilders.add(new ColumnBuilder(columnName).setType(accessType));
            }

            Database db = new DatabaseBuilder(accessFilePath.toFile())
                    .setFileFormat(Database.FileFormat.V2010)
                    .setAutoSync(false)
                    .create();

            TableBuilder tableBuilder = new TableBuilder(accessTableName);
            for (ColumnBuilder col : columnBuilders) {
                tableBuilder.addColumn(col);
            }
            Table newTable = tableBuilder.toTable(db);

            if (!empty) {
                Statement stmt = mssqlConn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
                stmt.setFetchSize(500);
                ResultSet rs = stmt.executeQuery(buildSelectQuery(metaDatabaseType, mssqlDatabaseName, mssqlTableName, metaDatabaseUrl));
                final int BATCH_SIZE = 1000;
                List<Map<String, Object>> batch = new ArrayList<>(BATCH_SIZE);
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (ColumnBuilder col : columnBuilders) {
                        row.put(col.getName(), rs.getObject(col.getName()));
                    }
                    batch.add(row);
                    if (batch.size() == BATCH_SIZE) {
                        newTable.addRowsFromMaps(batch);
                        batch.clear();
                    }
                }
                if (!batch.isEmpty()) {
                    newTable.addRowsFromMaps(batch);
                }
                rs.close();
                stmt.close();
            }

            mssqlConn.close();
            db.close();

            System.out.println("MS Access file created: " + accessFilePath);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static DataType mapSqlTypeToAccessType(int sqlType) {
        return switch (sqlType) {
            case Types.VARCHAR, Types.NVARCHAR, Types.CHAR, Types.NCHAR -> DataType.TEXT;
            case Types.INTEGER -> DataType.LONG;
            case Types.BIGINT -> DataType.NUMERIC;      // ← Changed
            case Types.FLOAT, Types.DOUBLE, Types.REAL, Types.DECIMAL, Types.NUMERIC ->      // ← Changed
                    DataType.DOUBLE;  // Use DOUBLE for all decimal types
            case Types.DATE, Types.TIMESTAMP -> DataType.SHORT_DATE_TIME;
            case Types.BIT, Types.BOOLEAN -> DataType.BOOLEAN;
            default -> DataType.TEXT;
        };
    }
}