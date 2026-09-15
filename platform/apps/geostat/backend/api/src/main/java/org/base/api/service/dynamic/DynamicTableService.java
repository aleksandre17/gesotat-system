package org.base.api.service.dynamic;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.base.core.entity.data.DataMode;
import org.base.core.entity.page_tree.PageLeafNode;
import org.base.core.repository.DataProfileRepository;
import org.base.core.repository.PageNodeRepository;
import org.springframework.stereotype.Service;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** Safe v1 dynamic table reader. Columns and target identifiers originate only from core metadata. */
@Service
@RequiredArgsConstructor
public class DynamicTableService {
    private static final java.util.regex.Pattern ENDPOINT = java.util.regex.Pattern.compile("[A-Za-z0-9._-]+(?::[0-9]{1,5})?");
    private static final java.util.regex.Pattern DATABASE = java.util.regex.Pattern.compile("[A-Za-z_][A-Za-z0-9_-]{0,127}");
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
    private static final int MAX_PAGE = 100_000;
    private final DataProfileRepository profileRepository;
    private final PageNodeRepository pageNodeRepository;
    private final ObjectMapper objectMapper;

    public DynamicTableResponse read(Long pageId, int page, int limit) {
        if (page < 1 || page > MAX_PAGE || limit < 1 || limit > 500) throw new IllegalArgumentException("page/limit is invalid");
        var profile = profileRepository.findByPageId(pageId)
                .orElseThrow(() -> new IllegalArgumentException("No data profile for page: " + pageId));
        if (!profile.isEnabled() || profile.getDataMode() == DataMode.LEGACY) {
            throw new IllegalStateException("Dynamic data is not enabled for page: " + pageId);
        }
        List<String> columns = columns(profile.getDisplayColumnsJson());
        PageLeafNode node = pageNodeRepository.findById(pageId).filter(PageLeafNode.class::isInstance)
                .map(PageLeafNode.class::cast).orElseThrow(() -> new IllegalArgumentException("PAGE not found: " + pageId));
        String type = node.getMetaDatabaseType();
        String sql = query(type, profile.getTargetSchema(), profile.getTargetTable(), columns, page, limit);
        try (var connection = DriverManager.getConnection(url(type, node.getMetaDatabaseUrl(), profile.getTargetDatabase()), node.getMetaDatabaseUser(), node.getMetaDatabasePassword());
             var statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            List<Map<String, Object>> rows = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (String column : columns) row.put(column, rs.getObject(column));
                rows.add(row);
            }
            return new DynamicTableResponse(columns, rows, page, limit);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to read dynamic table data", e);
        }
    }

    private List<String> columns(String json) {
        try {
            List<String> columns = objectMapper.readValue(json, new TypeReference<List<String>>() {});
            if (columns.isEmpty() || columns.stream().anyMatch(column -> !IDENTIFIER.matcher(column).matches()))
                throw new IllegalArgumentException("Profile displayColumns must contain safe column names");
            return columns;
        } catch (Exception e) {
            throw new IllegalStateException("Profile has no valid displayColumns configuration", e);
        }
    }

    private String query(String type, String schema, String table, List<String> columns, int page, int limit) {
        if ("mssql".equalsIgnoreCase(type)) {
            String select = String.join(", ", columns.stream().map(this::quote).toList());
            long offset = ((long) page - 1L) * limit;
            return "SELECT " + select + " FROM " + quote(schema) + "." + quote(table)
                    + " ORDER BY (SELECT NULL) OFFSET " + offset + " ROWS FETCH NEXT " + limit + " ROWS ONLY";
        }
        if (!IDENTIFIER.matcher(table == null ? "" : table).matches()) throw new IllegalStateException("Unsafe profile identifier");
        String select = String.join(", ", columns.stream().map(column -> "`" + column + "`").toList());
        long offset = ((long) page - 1L) * limit;
            return "SELECT " + select + " FROM `" + table + "` LIMIT " + limit + " OFFSET " + offset;
    }

    private String url(String type, String host, String database) {
        String cleanHost = host.replaceFirst("^jdbc:(sqlserver|mysql)://", "").split(";")[0];
        if (!ENDPOINT.matcher(cleanHost).matches() || !DATABASE.matcher(database == null ? "" : database).matches()) throw new IllegalStateException("Invalid governed database endpoint");
        return "mssql".equalsIgnoreCase(type)
                ? "jdbc:sqlserver://" + cleanHost + ";databaseName=" + database + ";encrypt=true;trustServerCertificate=false"
                : "jdbc:mysql://" + cleanHost + "/" + database + "?useSSL=true&requireSSL=true&verifyServerCertificate=true&allowPublicKeyRetrieval=false";
    }

    private String quote(String identifier) {
        if (!IDENTIFIER.matcher(identifier).matches()) throw new IllegalStateException("Unsafe profile identifier");
        return "[" + identifier + "]";
    }
}
