package org.base.mobile.arcitecture;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class SelectorBuilder {
    private final ObjectMapper objectMapper;

    public SelectorBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public JsonNode buildSelector(String type, String placeholder, List<Map<String, Object>> data) {
        ObjectNode selector = objectMapper.createObjectNode();
        selector.put("type", type);
        selector.put("placeholder", placeholder != null ? placeholder : "");
        ArrayNode selectValues = objectMapper.createArrayNode();
        for (Map<String, Object> row : data) {
            selectValues.add(createNode((String) row.get("name"), row.get("code")));
        }
        selector.set("selectValues", selectValues);
        return selector;
    }

    public JsonNode buildStaticSelector(String type, String placeholder, JsonNode values) {
        ObjectNode selector = objectMapper.createObjectNode();
        selector.put("type", type);
        selector.put("placeholder", placeholder != null ? placeholder : "");
        selector.set("selectValues", values);
        return selector;
    }

    private JsonNode createNode(String name, Object code) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("name", name);
        if (code == null) {
            node.putNull("code");
        } else if (code instanceof String) {
            node.put("code", (String) code);
        } else {
            node.put("code", (Integer) code);
        }
        return node;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
