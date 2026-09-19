package org.base.api.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The application ObjectMapper must treat untyped values as plain JSON; PageNode handling must not capture them. */
class JacksonConfigTest {

    @Test
    void untypedValuesRoundTripThroughTheApplicationObjectMapper() throws Exception {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        new JacksonConfig().jacksonCustomizer().customize(builder);
        ObjectMapper mapper = builder.build();

        Map<String, Object> read = mapper.readValue("{\"count\":1,\"nested\":{\"items\":[1,\"a\"]}}", new TypeReference<>() {});

        assertEquals(1, read.get("count"));
        assertEquals(Map.of("items", List.of(1, "a")), read.get("nested"));
        assertEquals("{\"value\":{\"count\":1}}", mapper.writeValueAsString(Map.of("value", (Object) Map.of("count", 1))));
    }
}
