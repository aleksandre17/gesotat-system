import com.healthmarketscience.jackcess.ColumnBuilder;
import com.healthmarketscience.jackcess.DataType;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Table;
import com.healthmarketscience.jackcess.TableBuilder;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Read-only, schema-preserving SQL Server database to Access exporter. */
public final class SqlServerDatabaseToAccessGenerator {
    private SqlServerDatabaseToAccessGenerator() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 5) {
            throw new IllegalArgumentException("Usage: <host> <database> <user> <password> <output.accdb>");
        }
        String host = args[0], database = args[1], user = args[2], password = args[3];
        File output = new File(args[4]);
        if (output.exists()) throw new IllegalStateException("Refusing to overwrite existing file: " + output.getAbsolutePath());
        File parent = output.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) throw new IllegalStateException("Cannot create: " + parent);

        String url = "jdbc:sqlserver://" + host + ";databaseName=" + database + ";encrypt=true;trustServerCertificate=true";
        try (Connection source = DriverManager.getConnection(url, user, password);
             Database access = DatabaseBuilder.create(Database.FileFormat.V2010, output)) {
            for (SourceTable sourceTable : tables(source)) exportTable(source, access, sourceTable);
        }
        System.out.println("Created: " + output.getAbsolutePath());
    }

    private static List<SourceTable> tables(Connection connection) throws Exception {
        List<SourceTable> result = new ArrayList<>();
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(
                "SELECT TABLE_SCHEMA, TABLE_NAME FROM INFORMATION_SCHEMA.TABLES " +
                        "WHERE TABLE_TYPE = 'BASE TABLE' AND TABLE_SCHEMA NOT IN ('sys', 'INFORMATION_SCHEMA') " +
                        "ORDER BY TABLE_SCHEMA, TABLE_NAME")) {
            while (rows.next()) result.add(new SourceTable(rows.getString(1), rows.getString(2)));
        }
        return result;
    }

    private static void exportTable(Connection source, Database access, SourceTable sourceTable) throws Exception {
        String qualified = quote(sourceTable.schema()) + "." + quote(sourceTable.name());
        try (Statement statement = source.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
            statement.setFetchSize(500);
            try (ResultSet rows = statement.executeQuery("SELECT * FROM " + qualified)) {
                ResultSetMetaData metadata = rows.getMetaData();
                TableBuilder builder = new TableBuilder(sourceTable.name());
                for (int i = 1; i <= metadata.getColumnCount(); i++) {
                    builder.addColumn(new ColumnBuilder(metadata.getColumnLabel(i), accessType(metadata, i)));
                }
                Table target = builder.toTable(access);
                List<Map<String, Object>> batch = new ArrayList<>(500);
                while (rows.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= metadata.getColumnCount(); i++) row.put(metadata.getColumnLabel(i), rows.getObject(i));
                    batch.add(row);
                    if (batch.size() == 500) { target.addRowsFromMaps(batch); batch.clear(); }
                }
                if (!batch.isEmpty()) target.addRowsFromMaps(batch);
                System.out.println("Exported " + sourceTable.schema() + "." + sourceTable.name() + " rows=" + target.getRowCount());
            }
        }
    }

    private static DataType accessType(ResultSetMetaData metadata, int column) throws Exception {
        return switch (metadata.getColumnType(column)) {
            case Types.BIT, Types.BOOLEAN -> DataType.BOOLEAN;
            case Types.TINYINT, Types.SMALLINT, Types.INTEGER -> DataType.LONG;
            case Types.BIGINT -> DataType.NUMERIC;
            case Types.FLOAT, Types.REAL, Types.DOUBLE, Types.DECIMAL, Types.NUMERIC -> DataType.DOUBLE;
            case Types.DATE, Types.TIME, Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE -> DataType.SHORT_DATE_TIME;
            case Types.LONGVARCHAR, Types.LONGNVARCHAR, Types.CLOB, Types.NCLOB -> DataType.MEMO;
            case Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY, Types.BLOB -> DataType.OLE;
            default -> metadata.getColumnDisplaySize(column) > 255 ? DataType.MEMO : DataType.TEXT;
        };
    }

    private static String quote(String identifier) { return "[" + identifier.replace("]", "]]" ) + "]"; }
    private record SourceTable(String schema, String name) { }
}
