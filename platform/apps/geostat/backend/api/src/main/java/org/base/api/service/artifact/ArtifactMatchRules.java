package org.base.api.service.artifact;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Registry of rule parsers by declared type. An undeclared type fails closed. */
@Component
public class ArtifactMatchRules {
    private final Map<String, ArtifactMatchRuleParser> parsers;

    public ArtifactMatchRules(List<ArtifactMatchRuleParser> parsers) {
        this.parsers = parsers.stream().collect(Collectors.toUnmodifiableMap(ArtifactMatchRuleParser::type, Function.identity()));
    }

    public ArtifactMatchRule parse(JsonNode rule) {
        ArtifactMatchRuleParser parser = parsers.get(rule.path("type").asText());
        if (parser == null) throw new IllegalArgumentException("Unsupported artifact match rule type: " + rule.path("type").asText() + "; known " + parsers.keySet());
        return parser.parse(rule);
    }
}
