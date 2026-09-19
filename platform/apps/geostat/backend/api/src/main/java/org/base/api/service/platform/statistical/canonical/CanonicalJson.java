package org.base.api.service.platform.statistical.canonical;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * RFC 8785 (JCS) serialisation for the value space a contract needs: objects, arrays, strings, booleans,
 * null and integers inside the IEEE-754 safe range. Decimals travel as strings, so no floating-point
 * number ever reaches a digest (register Q39). Text is NFC-normalised before hashing.
 */
public final class CanonicalJson {
    private static final long SAFE_INTEGER = 9_007_199_254_740_991L;

    private CanonicalJson() { }

    public static String write(Object value) {
        StringBuilder out = new StringBuilder(256);
        append(out, value);
        return out.toString();
    }

    /** SHA-256 over a domain-separation label, a line feed and the canonical form. */
    public static String digest(String domain, Object value) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            sha.update((domain + "\n").getBytes(StandardCharsets.UTF_8));
            byte[] hash = sha.digest(write(value).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(64);
            for (byte b : hash) hex.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private static void append(StringBuilder out, Object value) {
        if (value == null) out.append("null");
        else if (value instanceof Boolean b) out.append(b ? "true" : "false");
        else if (value instanceof String s) string(out, s);
        else if (value instanceof Enum<?> e) string(out, e.name());
        else if (value instanceof Integer || value instanceof Long) integer(out, ((Number) value).longValue());
        else if (value instanceof Map<?, ?> map) object(out, map);
        else if (value instanceof Iterable<?> items) array(out, items);
        else throw new IllegalArgumentException("not canonicalisable: " + value.getClass().getName() + " (encode decimals as strings)");
    }

    private static void integer(StringBuilder out, long value) {
        if (Math.abs(value) > SAFE_INTEGER) throw new IllegalArgumentException("integer outside the interoperable range");
        out.append(value);
    }

    private static void object(StringBuilder out, Map<?, ?> map) {
        List<String> keys = new ArrayList<>(map.size());
        for (Object key : map.keySet()) {
            if (!(key instanceof String s)) throw new IllegalArgumentException("object keys must be strings");
            keys.add(s);
        }
        keys.sort(String::compareTo); // String.compareTo orders by UTF-16 code unit, as RFC 8785 requires
        out.append('{');
        for (int i = 0; i < keys.size(); i++) {
            if (i > 0) out.append(',');
            string(out, keys.get(i));
            out.append(':');
            append(out, map.get(keys.get(i)));
        }
        out.append('}');
    }

    private static void array(StringBuilder out, Iterable<?> items) {
        out.append('[');
        boolean first = true;
        for (Object item : items) {
            if (!first) out.append(',');
            append(out, item);
            first = false;
        }
        out.append(']');
    }

    private static void string(StringBuilder out, String raw) {
        String s = Normalizer.normalize(raw, Normalizer.Form.NFC);
        out.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
                }
            }
        }
        out.append('"');
    }
}
