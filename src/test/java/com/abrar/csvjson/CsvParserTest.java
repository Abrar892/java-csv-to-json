package com.abrar.csvjson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import org.junit.jupiter.api.Test;

class CsvParserTest {

    private List<List<String>> parse(String csv) throws IOException {
        return new CsvParser().parseAll(new StringReader(csv));
    }

    @Test
    void simpleRows() throws IOException {
        assertEquals(List.of(List.of("a", "b"), List.of("1", "2")), parse("a,b\n1,2\n"));
    }

    @Test
    void lastRowWithoutTrailingNewline() throws IOException {
        assertEquals(List.of(List.of("a", "b"), List.of("1", "2")), parse("a,b\n1,2"));
    }

    @Test
    void quotedFieldContainingDelimiter() throws IOException {
        assertEquals(List.of(List.of("a", "b,c", "d")), parse("a,\"b,c\",d\n"));
    }

    @Test
    void quotedFieldWithEmbeddedNewline() throws IOException {
        assertEquals(List.of(List.of("a", "line1\nline2")), parse("a,\"line1\nline2\"\n"));
    }

    @Test
    void doubledQuotesBecomeOneQuote() throws IOException {
        assertEquals(List.of(List.of("say \"hi\"")), parse("\"say \"\"hi\"\"\"\n"));
    }

    @Test
    void crlfEndings() throws IOException {
        assertEquals(List.of(List.of("a", "b"), List.of("1", "2")), parse("a,b\r\n1,2\r\n"));
    }

    @Test
    void crOnlyEndings() throws IOException {
        assertEquals(List.of(List.of("a", "b"), List.of("1", "2")), parse("a,b\r1,2\r"));
    }

    @Test
    void emptyFieldsAndTrailingDelimiter() throws IOException {
        assertEquals(List.of(List.of("a", "", "c", "")), parse("a,,c,\n"));
    }

    @Test
    void emptyLinesAreSkipped() throws IOException {
        assertEquals(List.of(List.of("a", "b"), List.of("1", "2")), parse("a,b\n\n1,2\n"));
    }

    @Test
    void bomIsStripped() throws IOException {
        assertEquals(List.of(List.of("a", "b")), parse("\uFEFFa,b\n"));
    }

    @Test
    void customDelimiter() throws IOException {
        CsvParser parser = new CsvParser(';', '"');
        assertEquals(List.of(List.of("a", "b")), parser.parseAll(new StringReader("a;b\n")));
    }

    @Test
    void customQuoteCharacter() throws IOException {
        CsvParser parser = new CsvParser(',', '\'');
        assertEquals(List.of(List.of("a", "b,c")), parser.parseAll(new StringReader("a,'b,c'\n")));
    }

    @Test
    void unterminatedQuoteFailsWithLineNumber() {
        CsvException e = assertThrows(CsvException.class,
                () -> parse("a,b\n\"unterminated\n"));
        assertEquals(2, e.line());
        assertTrue(e.getMessage().contains("unterminated"));
    }

    @Test
    void strayTextAfterClosingQuoteFails() {
        assertThrows(CsvException.class, () -> parse("\"a\"x,b\n"));
    }

    @Test
    void quotedEmptyFieldIsKept() throws IOException {
        assertEquals(List.of(List.of("", "b")), parse("\"\",b\n"));
    }

    @Test
    void whitespaceAfterClosingQuoteIsTolerated() throws IOException {
        assertEquals(List.of(List.of("a", "b")), parse("\"a\" ,b\n"));
    }
}
