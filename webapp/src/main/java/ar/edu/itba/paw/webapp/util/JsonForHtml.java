package ar.edu.itba.paw.webapp.util;

import java.util.Map;

/**
 * Serializes values to JSON safe for embedding in HTML (inline {@code <script type="application/json">}
 * blocks and {@code data-*} attributes after JSP {@code fn:escapeXml} where applicable).
 */
public final class JsonForHtml {

    private JsonForHtml() {}

    public static String serialize(final Object value) {
        final StringBuilder json = new StringBuilder();
        writeJsonValue(json, value);
        return makeScriptEmbeddingSafe(json.toString());
    }

    /**
     * Escapes characters that terminate or alter HTML/JS contexts while remaining valid JSON
     * ({@code JSON.parse} decodes {@code \u003c} to {@code <}).
     */
    static String makeScriptEmbeddingSafe(final String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }
        return json.replace("<", "\\u003c")
                .replace(">", "\\u003e")
                .replace("&", "\\u0026")
                .replace("\u2028", "\\u2028")
                .replace("\u2029", "\\u2029");
    }

    private static void writeJsonValue(final StringBuilder json, final Object value) {
        if (value == null) {
            json.append("null");
            return;
        }
        if (value instanceof final String stringValue) {
            json.append('"').append(escapeJsonString(stringValue)).append('"');
            return;
        }
        if (value instanceof final Boolean booleanValue) {
            json.append(booleanValue);
            return;
        }
        if (value instanceof final Number numberValue) {
            json.append(numberValue);
            return;
        }
        if (value instanceof final Map<?, ?> mapValue) {
            json.append('{');
            boolean first = true;
            for (final Map.Entry<?, ?> entry : mapValue.entrySet()) {
                if (!first) {
                    json.append(',');
                }
                first = false;
                json.append('"')
                        .append(escapeJsonString(String.valueOf(entry.getKey())))
                        .append("\":");
                writeJsonValue(json, entry.getValue());
            }
            json.append('}');
            return;
        }
        if (value instanceof final Iterable<?> iterableValue) {
            json.append('[');
            boolean first = true;
            for (final Object element : iterableValue) {
                if (!first) {
                    json.append(',');
                }
                first = false;
                writeJsonValue(json, element);
            }
            json.append(']');
            return;
        }
        json.append('"').append(escapeJsonString(String.valueOf(value))).append('"');
    }

    private static String escapeJsonString(final String value) {
        final StringBuilder escaped = new StringBuilder(value.length() + 16);
        for (int index = 0; index < value.length(); index++) {
            final char character = value.charAt(index);
            switch (character) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.toString();
    }
}
