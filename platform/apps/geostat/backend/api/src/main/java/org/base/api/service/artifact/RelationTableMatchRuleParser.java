package org.base.api.service.artifact;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class RelationTableMatchRuleParser implements ArtifactMatchRuleParser {
    private static final Pattern TABLE = Pattern.compile("[A-Za-z_][A-Za-z0-9_]{0,127}");
    private static final Pattern ROLE = Pattern.compile("[A-Z][A-Z0-9_]{0,63}");

    @Override
    public String type() {
        return RelationTableMatchRule.TYPE;
    }

    @Override
    public ArtifactMatchRule parse(JsonNode rule) {
        String root = rule.path("packageRoot").asText("");
        if (root.isEmpty() || !root.endsWith("/")) throw new IllegalArgumentException("packageRoot must be declared and end with '/'");
        ArtifactManifestGenerator.normalizePath(root.substring(0, root.length() - 1));
        List<String> languages = new ArrayList<>();
        for (JsonNode language : rule.path("languages")) {
            if (!LANGUAGE.matcher(language.asText()).matches()) throw new IllegalArgumentException("Invalid language: " + language.asText());
            if (languages.contains(language.asText())) throw new IllegalArgumentException("Duplicate language: " + language.asText());
            languages.add(language.asText());
        }
        if (languages.isEmpty()) throw new IllegalArgumentException("RELATION_TABLE declares no languages");
        String role = optional(rule, "role", ROLE);
        return new RelationTableMatchRule(required(rule, "relationTable", TABLE), required(rule, "entityKeyField", FIELD),
                required(rule, "artifactKeyField", FIELD), required(rule, "ordinalField", FIELD), optional(rule, "roleField", FIELD), role,
                optional(rule, "languageField", FIELD), required(rule, "artifactTable", TABLE), required(rule, "artifactTableKeyField", FIELD),
                required(rule, "fileNameField", FIELD), root, languages);
    }

    private static String required(JsonNode rule, String name, Pattern pattern) {
        String value = optional(rule, name, pattern);
        if (value == null) throw new IllegalArgumentException("RELATION_TABLE requires " + name);
        return value;
    }

    private static String optional(JsonNode rule, String name, Pattern pattern) {
        JsonNode node = rule.path(name);
        if (node.isMissingNode() || node.isNull() || node.asText().isEmpty()) return null;
        if (!pattern.matcher(node.asText()).matches()) throw new IllegalArgumentException("Invalid " + name + ": " + node.asText());
        return node.asText();
    }
}
