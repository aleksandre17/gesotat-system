package org.base.api.service.artifact;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

@Component
public class SourcePathMatchRuleParser implements ArtifactMatchRuleParser {
    @Override
    public String type() {
        return SourcePathMatchRule.TYPE;
    }

    @Override
    public ArtifactMatchRule parse(JsonNode rule) {
        if (!"NFC".equals(rule.path("normalization").asText("NFC"))) throw new IllegalArgumentException("Only NFC normalization is supported");
        String strip = rule.path("stripPrefix").asText("");
        String root = rule.path("packageRoot").asText("");
        if (!root.isEmpty()) {
            if (!root.endsWith("/")) throw new IllegalArgumentException("packageRoot must end with '/'");
            ArtifactManifestGenerator.normalizePath(root.substring(0, root.length() - 1));
        }
        return new SourcePathMatchRule(strip, root, ArtifactMatchRuleParser.bindings(rule));
    }
}
