package com.abrar.csvjson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MainTest {

    @TempDir
    Path tmp;

    private String convert(String csv, String... extraArgs) throws IOException {
        Path in = tmp.resolve("in.csv");
        Path out = tmp.resolve("out.json");
        Files.writeString(in, csv, StandardCharsets.UTF_8);
        String[] args = new String[extraArgs.length + 3];
        System.arraycopy(extraArgs, 0, args, 0, extraArgs.length);
        args[extraArgs.length] = "--output";
        args[extraArgs.length + 1] = out.toString();
        args[extraArgs.length + 2] = in.toString();
        Main.run(args);
        return Files.readString(out, StandardCharsets.UTF_8);
    }

    @Test
    void basicConversionInfersTypes() throws IOException {
        String json = convert("name,age,active\nAda,36,true\nBob,,false\n");
        assertEquals("[{\"name\":\"Ada\",\"age\":36,\"active\":true},"
                + "{\"name\":\"Bob\",\"age\":null,\"active\":false}]", json);
    }

    @Test
    void ndjsonMode() throws IOException {
        String json = convert("a,b\n1,2\n3,4\n", "--ndjson");
        assertEquals("{\"a\":1,\"b\":2}\n{\"a\":3,\"b\":4}\n", json);
    }

    @Test
    void noInferKeepsStrings() throws IOException {
        String json = convert("a,b\n1,true\n", "--no-infer");
        assertEquals("[{\"a\":\"1\",\"b\":\"true\"}]", json);
    }

    @Test
    void nestedHeadersByDefault() throws IOException {
        String json = convert("address.city,address.zip\nLahore,54000\n");
        assertEquals("[{\"address\":{\"city\":\"Lahore\",\"zip\":54000}}]", json);
    }

    @Test
    void noNestedKeepsFlatKeys() throws IOException {
        String json = convert("address.city\nLahore\n", "--no-nested");
        assertEquals("[{\"address.city\":\"Lahore\"}]", json);
    }

    @Test
    void noHeaderGeneratesColKeys() throws IOException {
        String json = convert("Ada,36\nBob,41\n", "--no-header");
        assertEquals("[{\"col0\":\"Ada\",\"col1\":36},{\"col0\":\"Bob\",\"col1\":41}]", json);
    }

    @Test
    void customDelimiter() throws IOException {
        String json = convert("a;b\n1;2\n", "--delimiter", ";");
        assertEquals("[{\"a\":1,\"b\":2}]", json);
    }

    @Test
    void shortRowGetsNullForMissingCells() throws IOException {
        String json = convert("a,b,c\n1,2\n");
        assertEquals("[{\"a\":1,\"b\":2,\"c\":null}]", json);
    }

    @Test
    void longRowFails() {
        assertThrows(CsvException.class,
                () -> convert("a,b\n1,2,3\n"));
    }

    @Test
    void quotedCommasAndNewlinesSurvive() throws IOException {
        String json = convert("name,note\nAda,\"hello, world\"\nBob,\"line1\nline2\"\n");
        assertEquals("[{\"name\":\"Ada\",\"note\":\"hello, world\"},"
                + "{\"name\":\"Bob\",\"note\":\"line1\\nline2\"}]", json);
    }

    @Test
    void unknownOptionFails() {
        assertThrows(CsvException.class,
                () -> convert("a\n1\n", "--bogus"));
    }
}
