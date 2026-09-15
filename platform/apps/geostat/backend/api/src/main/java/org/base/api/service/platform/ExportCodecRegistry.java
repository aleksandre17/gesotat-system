package org.base.api.service.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Immutable registry of approved export codecs. */
public final class ExportCodecRegistry {
    private final Map<String, ExportCodec> codecs;

    public ExportCodecRegistry(ObjectMapper mapper) {
        Objects.requireNonNull(mapper, "mapper");
        Map<String, ExportCodec> values = new LinkedHashMap<>();
        register(values, new BasicCodec(new ExportFormatRegistry.Format("JSON", "application/json", "json", false),
                (response, objectMapper) -> ExportCodec.json(response, objectMapper)));
        register(values, new BasicCodec(new ExportFormatRegistry.Format("NDJSON", "application/x-ndjson", "ndjson", true),
                (response, objectMapper) -> {
                    Object rows = response.get("data");
                    StringBuilder out = new StringBuilder();
                    if (rows instanceof Iterable<?> iterable) for (Object row : iterable)
                        out.append(objectMapper.writeValueAsString(row)).append('\n');
                    else out.append(objectMapper.writeValueAsString(response)).append('\n');
                    return out.toString().getBytes(StandardCharsets.UTF_8);
                }));
        register(values, new BasicCodec(new ExportFormatRegistry.Format("CSV", "text/csv", "csv", false),
                (response, objectMapper) -> {
                    Object rows = response.get("data");
                    if (!(rows instanceof Iterable<?> iterable)) return ("value\n\"" + ExportCodec.scalar(response) + "\"\n").getBytes(StandardCharsets.UTF_8);
                    List<Map<String,Object>> records = new ArrayList<>();
                    for (Object row : iterable) if (row instanceof Map<?,?> map) {
                        Map<String,Object> normalized = new LinkedHashMap<>();
                        map.forEach((k,v) -> normalized.put(String.valueOf(k), v)); records.add(normalized);
                    }
                    if (records.isEmpty()) return new byte[0];
                    List<String> headers = new ArrayList<>(records.get(0).keySet());
                    StringBuilder out = new StringBuilder(String.join(",", headers)).append('\n');
                    for (Map<String,Object> record : records) {
                        for (int i=0;i<headers.size();i++) { if (i>0) out.append(','); out.append('"').append(ExportCodec.scalar(record.get(headers.get(i)))).append('"'); }
                        out.append('\n');
                    }
                    return out.toString().getBytes(StandardCharsets.UTF_8);
                }));
        register(values, new BasicCodec(new ExportFormatRegistry.Format("ZIP_JSON", "application/zip", "zip", false),
                (response, objectMapper) -> {
                    byte[] payload = ExportCodec.json(response, objectMapper);
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
                        zip.putNextEntry(new ZipEntry("response.json")); zip.write(payload); zip.closeEntry();
                    }
                    return bytes.toByteArray();
                }));
        register(values, new BasicCodec(new ExportFormatRegistry.Format("SDMX_JSON", "application/vnd.sdmx.data+json;version=2.0.0", "json", false),
                (response, objectMapper) -> {
                    Map<String,Object> sdmx = new LinkedHashMap<>();
                    sdmx.put("header", Map.of("id", "geostat-export"));
                    sdmx.put("data", response.getOrDefault("data", List.of()));
                    sdmx.put("structure", response.getOrDefault("schema", Map.of()));
                    return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(sdmx);
                }));
        register(values, new BasicCodec(new ExportFormatRegistry.Format("SDMX_XML", "application/vnd.sdmx.genericdata+xml;version=2.1", "xml", false),
                (response, objectMapper) -> {
                    String payload = objectMapper.writeValueAsString(response.getOrDefault("data", List.of()))
                            .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
                    String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                            "<message:GenericData xmlns:message=\"http://www.sdmx.org/resources/sdmxml/schemas/v2_1/message\" " +
                            "xmlns:generic=\"http://www.sdmx.org/resources/sdmxml/schemas/v2_1/data/generic\"><message:DataSet><generic:Attributes>" +
                            payload + "</generic:Attributes></message:DataSet></message:GenericData>";
                    return xml.getBytes(StandardCharsets.UTF_8);
                }));
        codecs = Collections.unmodifiableMap(values);
    }

    public ExportCodec require(String code) {
        String normalized = code == null || code.isBlank() ? "JSON" : code.trim().toUpperCase(Locale.ROOT);
        ExportCodec codec = codecs.get(normalized);
        if (codec == null) throw new IllegalArgumentException("Unsupported export codec: " + code);
        return codec;
    }

    public Set<String> supportedCodes() { return codecs.keySet(); }

    private static void register(Map<String,ExportCodec> target, ExportCodec codec) {
        if (target.putIfAbsent(codec.format().code(), codec) != null) throw new IllegalStateException("Duplicate export codec");
    }
    private record BasicCodec(ExportFormatRegistry.Format format, Encoder encoder) implements ExportCodec {
        @Override public byte[] encode(Map<String,Object> response, ObjectMapper mapper) throws Exception { return encoder.encode(response, mapper); }
    }
    @FunctionalInterface private interface Encoder { byte[] encode(Map<String,Object> response, ObjectMapper mapper) throws Exception; }
}
