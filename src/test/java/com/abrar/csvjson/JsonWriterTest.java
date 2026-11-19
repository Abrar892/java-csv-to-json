package com.abrar.csvjson;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class JsonWriterTest {

    private String render(boolean nested, List<String> headers, List<Object> values)
            throws IOException {
        StringWriter sw = new StringWriter();
        JsonWriter w = new JsonWriter(sw, true, false, nested);
        w.writeRow(headers, values);
        return sw.toString().trim();
    }

    @Test
    void flatObject() throws IOException {
        assertEquals("{\"name\":\"Ada\",\"age\":36,\"active\":true}",
                render(false, List.of("name", "age", "active"),
                        List.of("Ada", 36L, true)));
    }

    @Test
    void nullsAndStrings() throws IOException {
        assertEquals("{\"a\":null,\"b\":\"x\"}",
                render(false, List.of("a", "b"), Arrays.asList(null, "x")));
    }

    @Test
    void dottedHeadersNest() throws IOException {
        assertEquals("{\"address\":{\"city\":\"Lahore\",\"zip\":\"54000\"}}",
                render(true, List.of("address.city", "address.zip"),
                        List.of("Lahore", "54000")));
    }

    @Test
    void dottedHeadersStayFlatWhenNestingOff() throws IOException {
        assertEquals("{\"address.city\":\"Lahore\"}",
                render(false, List.of("address.city"), List.of("Lahore")));
    }

    @Test
    void stringsAreEscaped() throws IOException {
        assertEquals("{\"q\":\"say \\\"hi\\\"\\nline2\"}",
                render(false, List.of("q"), List.of("say \"hi\"\nline2")));
    }

    @Test
    void backslashAndTabEscaped() throws IOException {
        assertEquals("{\"p\":\"C:\\\\temp\\tx\"}",
                render(false, List.of("p"), List.of("C:\\temp\tx")));
    }

    @Test
    void arrayLayout() throws IOException {
        StringWriter sw = new StringWriter();
        JsonWriter w = new JsonWriter(sw, false, false, false);
        w.begin();
        w.writeRow(List.of("a"), List.of(1L));
        w.writeRow(List.of("a"), List.of(2L));
        w.end();
        assertEquals("[{\"a\":1},{\"a\":2}]", sw.toString());
    }

    @Test
    void ndjsonLayoutOneObjectPerLine() throws IOException {
        StringWriter sw = new StringWriter();
        JsonWriter w = new JsonWriter(sw, true, false, false);
        w.begin();
        w.writeRow(List.of("a"), List.of(1L));
        w.writeRow(List.of("a"), List.of(2L));
        w.end();
        assertEquals("{\"a\":1}\n{\"a\":2}\n", sw.toString());
    }

    @Test
    void prettyArray() throws IOException {
        StringWriter sw = new StringWriter();
        JsonWriter w = new JsonWriter(sw, false, true, false);
        w.begin();
        w.writeRow(List.of("a", "b"), List.of(1L, "x"));
        w.end();
        assertEquals("[\n  {\n    \"a\": 1,\n    \"b\": \"x\"\n  }\n]\n", sw.toString());
    }
}
