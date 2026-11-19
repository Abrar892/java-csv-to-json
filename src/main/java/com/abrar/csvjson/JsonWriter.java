package com.abrar.csvjson;

import java.io.IOException;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Writes rows as JSON. Two layouts are supported: one JSON object per line
 * (NDJSON, good for streaming pipelines) or a single JSON array (good for
 * loading the whole result at once).
 *
 * <p>When nesting is on, a header like {@code address.city} becomes a nested
 * object instead of a flat key, so dotted CSV headers map naturally onto
 * nested JSON. Values are already type-inferred by {@link TypeInferer}.
 */
public final class JsonWriter {

    private final Writer out;
    private final boolean ndjson;
    private final boolean pretty;
    private final boolean nested;
    private boolean firstRow = true;

    public JsonWriter(Writer out, boolean ndjson, boolean pretty, boolean nested) {
        this.out = out;
        this.ndjson = ndjson;
        this.pretty = pretty;
        this.nested = nested;
    }

    public void begin() throws IOException {
        if (!ndjson) {
            out.write(pretty ? "[\n" : "[");
        }
    }

    public void writeRow(List<String> headers, List<Object> values) throws IOException {
        Map<String, Object> obj = buildObject(headers, values);
        if (!ndjson && !firstRow) {
            out.write(",");
        }
        if (!ndjson && pretty) {
            out.write(firstRow ? "  " : "\n  ");
        }
        writeValue(obj, pretty ? 1 : 0);
        if (ndjson) {
            out.write("\n");
        }
        firstRow = false;
    }

    public void end() throws IOException {
        if (!ndjson) {
            out.write(pretty ? "\n]\n" : "]");
        }
        out.flush();
    }

    /** Builds the JSON object for one row, applying nesting when enabled. */
    static Map<String, Object> toObject(List<String> headers, List<Object> values) {
        Map<String, Object> obj = new LinkedHashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            obj.put(headers.get(i), values.get(i));
        }
        return obj;
    }

    /** Builds the JSON object with dotted headers expanded into nested objects. */
    Map<String, Object> toNestedObject(List<String> headers, List<Object> values) {
        Map<String, Object> root = new LinkedHashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            String[] parts = headers.get(i).split("\\.", -1);
            Map<String, Object> current = root;
            for (int j = 0; j < parts.length - 1; j++) {
                Object child = current.get(parts[j]);
                if (!(child instanceof Map)) {
                    Map<String, Object> next = new LinkedHashMap<>();
                    current.put(parts[j], next);
                    current = next;
                } else {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> existing = (Map<String, Object>) child;
                    current = existing;
                }
            }
            current.put(parts[parts.length - 1], values.get(i));
        }
        return root;
    }

    private void writeValue(Object value, int indent) throws IOException {
        if (value == null) {
            out.write("null");
        } else if (value instanceof Boolean || value instanceof Number) {
            out.write(value.toString());
        } else if (value instanceof String s) {
            writeString(s);
        } else if (value instanceof Map<?, ?> map) {
            writeObject(map, indent);
        } else if (value instanceof List<?> list) {
            out.write("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    out.write(", ");
                }
                writeValue(list.get(i), indent);
            }
            out.write("]");
        } else {
            writeString(value.toString());
        }
    }

    private void writeObject(Map<?, ?> map, int indent) throws IOException {
        out.write("{");
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) {
                out.write(",");
            }
            if (pretty) {
                out.write("\n");
                out.write("  ".repeat(indent + 1));
            }
            writeString(entry.getKey().toString());
            out.write(pretty ? ": " : ":");
            writeValue(entry.getValue(), indent + 1);
            first = false;
        }
        if (pretty && !map.isEmpty()) {
            out.write("\n");
            out.write("  ".repeat(indent));
        }
        out.write("}");
    }

    /** Escapes a string per the JSON spec (quotes, backslash, control chars). */
    static void escapeInto(String s, Appendable out) throws IOException {
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
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
    }

    private void writeString(String s) throws IOException {
        out.write('"');
        escapeInto(s, out);
        out.write('"');
    }

    /** Applies nesting to a row when the writer was built with nesting on. */
    Map<String, Object> buildObject(List<String> headers, List<Object> values) {
        return nested ? toNestedObject(headers, values) : toObject(headers, values);
    }

    /** Test helper: renders one row to a string. */
    String renderRow(List<String> headers, List<Object> values) throws IOException {
        java.io.StringWriter sw = new java.io.StringWriter();
        JsonWriter w = new JsonWriter(sw, true, false, nested);
        w.writeValue(w.buildObject(headers, values), 0);
        return sw.toString();
    }
}
