package org.base.api.service.artifact;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/** Parses one declared rule type from control-plane JSON. Implementations are discovered as Spring beans. */
public interface ArtifactMatchRuleParser {
    Pattern FIELD = Pattern.compile("[A-Za-z_][A-Za-z0-9_]{0,127}");
    Pattern LANGUAGE = Pattern.compile("und|[a-z]{2,3}(-[A-Za-z0-9]{2,8})*");

    String type();

    ArtifactMatchRule parse(JsonNode rule);

    /** Shared, validated binding list: field identifiers, BCP-47 language, one binding per language. */
    static List<ArtifactMatchRule.Binding> bindings(JsonNode rule) {
        List<ArtifactMatchRule.Binding> bindings = new ArrayList<>();
        Set<String> languages = new HashSet<>();
        for (JsonNode node : rule.path("bindings")) {
            String language = node.path("language").asText("und");
            String field = node.path("field").asText();
            if (!LANGUAGE.matcher(language).matches()) throw new IllegalArgumentException("Invalid binding language: " + language);
            if (!FIELD.matcher(field).matches()) throw new IllegalArgumentException("Invalid binding field: " + field);
            if (!languages.add(language)) throw new IllegalArgumentException("Duplicate binding language: " + language);
            bindings.add(new ArtifactMatchRule.Binding(language, field, node.path("required").asBoolean(true)));
        }
        if (bindings.isEmpty()) throw new IllegalArgumentException("Artifact match rule declares no bindings");
        return bindings;
    }
}
