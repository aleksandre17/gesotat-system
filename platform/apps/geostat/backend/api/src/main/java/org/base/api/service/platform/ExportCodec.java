package org.base.api.service.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** Provider-neutral export encoder contract. */
public interface ExportCodec {
    ExportFormatRegistry.Format format();
    byte[] encode(Map<String, Object> response, ObjectMapper mapper) throws Exception;

    static byte[] json(Map<String,Object> response, ObjectMapper mapper) throws Exception {
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(response);
    }
    static String scalar(Object value) {
        if (value == null) return "";
        String s = String.valueOf(value);
        return s.replace("\"", "\"\"");
    }
}
