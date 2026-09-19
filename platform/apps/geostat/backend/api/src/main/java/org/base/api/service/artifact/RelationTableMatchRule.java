package org.base.api.service.artifact;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code RELATION_TABLE}: attachments are declared by rows of the package itself (contract §4.2, §4.3), not by
 * columns of the entity row. A relation table links an entity key to an artifact key with an ordinal (and
 * optionally a role and a language); an artifact table gives each artifact key its file name. Many files per
 * row and one file for many rows are both plain data; nothing is guessed.
 *
 * <p>The values a row binds are therefore not in its payload. They are supplied as resolved package paths in
 * the synthetic field {@link #valueField(String)} by {@link DeclaredRowValues}, at admission from the package
 * tables and at snapshot binding from the accepted manifest document, so both use the same edges.
 */
public record RelationTableMatchRule(String relationTable, String entityKeyField, String artifactKeyField, String ordinalField,
                                     String roleField, String role, String languageField, String artifactTable,
                                     String artifactTableKeyField, String fileNameField, String packageRoot,
                                     List<String> languages) implements ArtifactMatchRule, PackageDeclaredValues {
    public static final String TYPE = "RELATION_TABLE";
    private static final String VALUE_FIELD_PREFIX = "__declared_artifacts_";

    public RelationTableMatchRule {
        languages = List.copyOf(languages);
        if ((roleField == null) != (role == null)) throw new IllegalArgumentException("roleField and role are declared together");
        if (languageField == null && languages.size() != 1)
            throw new IllegalArgumentException("Without languageField the rule binds exactly one language");
    }

    @Override
    public String type() {
        return TYPE;
    }

    /** Required, so a row without attachments is an error exactly when the relation declares a minimum. */
    @Override
    public List<Binding> bindings() {
        return languages.stream().map(language -> new Binding(language, valueField(language), true)).toList();
    }

    /** Values are already package paths; only a path inside this rule's root is in scope. */
    @Override
    public String resolve(String sourceValue) {
        String path = ArtifactManifestGenerator.normalizePath(sourceValue);
        return covers(path) ? path : null;
    }

    @Override
    public boolean covers(String packagePath) {
        return packagePath.startsWith(packageRoot);
    }

    @Override
    public String valueField(String language) {
        return VALUE_FIELD_PREFIX + language;
    }

    @Override
    public List<DeclaredEdge> declaredEdges(String relationCode, PackageTableReader tables, File datasetFile) throws IOException {
        Map<String, String> fileNames = new HashMap<>();
        for (Map<String, String> artifact : tables.rows(datasetFile, artifactTable, List.of(artifactTableKeyField, fileNameField))) {
            String key = required(artifact, artifactTableKeyField, artifactTable);
            if (fileNames.put(key, required(artifact, fileNameField, artifactTable)) != null)
                throw new IllegalArgumentException("Artifact key " + key + " is declared twice in " + artifactTable);
        }
        List<String> fields = new ArrayList<>(List.of(entityKeyField, artifactKeyField, ordinalField));
        if (roleField != null) fields.add(roleField);
        if (languageField != null) fields.add(languageField);
        List<DeclaredEdge> edges = new ArrayList<>();
        for (Map<String, String> relation : tables.rows(datasetFile, relationTable, fields)) {
            if (roleField != null && !role.equals(relation.get(roleField))) continue;
            String language = languageField == null ? languages.get(0) : required(relation, languageField, relationTable);
            if (!languages.contains(language)) throw new IllegalArgumentException("Relation " + relationCode + " does not declare language " + language);
            String artifactKey = required(relation, artifactKeyField, relationTable);
            String fileName = fileNames.get(artifactKey);
            if (fileName == null) throw new IllegalArgumentException("Relation " + relationCode + " references unknown artifact key " + artifactKey);
            edges.add(new DeclaredEdge(required(relation, entityKeyField, relationTable), language,
                    ordinal(required(relation, ordinalField, relationTable), relationCode), ArtifactManifestGenerator.normalizePath(packageRoot + fileName)));
        }
        return edges;
    }

    private static String required(Map<String, String> row, String field, String table) {
        String value = row.get(field);
        if (value == null || value.isBlank()) throw new IllegalArgumentException(table + " has a row without " + field);
        return value;
    }

    private static long ordinal(String value, String relationCode) {
        try {
            double parsed = Double.parseDouble(value);
            if (parsed < 1 || parsed != Math.rint(parsed)) throw new NumberFormatException();
            return (long) parsed;
        } catch (NumberFormatException invalid) {
            throw new IllegalArgumentException("Relation " + relationCode + " has an ordinal that is not a positive whole number: " + value);
        }
    }
}
