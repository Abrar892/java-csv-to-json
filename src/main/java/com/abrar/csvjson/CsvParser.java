package com.abrar.csvjson;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A small, dependency-free RFC 4180 CSV parser.
 *
 * <p>Rows are delivered one at a time through a callback, so arbitrarily large
 * files convert in constant memory. The parser handles the parts of CSV that
 * naive split-on-comma code gets wrong: quoted fields containing the delimiter,
 * embedded newlines, doubled quotes, CRLF and LF line endings, and a UTF-8 BOM
 * on the first line. A malformed row fails fast with the offending line number.
 */
public final class CsvParser {

    private final char delimiter;
    private final char quote;

    public CsvParser() {
        this(',', '"');
    }

    public CsvParser(char delimiter, char quote) {
        this.delimiter = delimiter;
        this.quote = quote;
    }

    /** Parses every row, invoking {@code rowHandler} once per row. */
    public void parse(Reader source, Consumer<List<String>> rowHandler) throws IOException {
        // Normalize the input: BufferedReader gives us mark/reset, PushbackReader
        // gives us one-character pushback. Both are used below.
        java.io.PushbackReader in =
                new java.io.PushbackReader(new java.io.BufferedReader(source), 2);
        int ch;
        int line = 1;
        int quoteStartLine = 1;
        boolean atRowStart = true;
        boolean fieldStart = true;
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        boolean fieldQuoted = false;
        boolean afterQuote = false;

        // Strip a UTF-8 BOM if present (push it back otherwise).
        int first = in.read();
        if (first != 0xFEFF && first != -1) {
            in.unread(first);
        }

        while ((ch = in.read()) != -1) {
            char c = (char) ch;

            if (inQuotes) {
                if (c == quote) {
                    int next = in.read();
                    if (next == quote) {
                        field.append(quote); // doubled quote -> literal quote
                    } else {
                        inQuotes = false;
                        afterQuote = true;
                        if (next != -1) {
                            unread(in, next);
                        }
                    }
                } else {
                    if (c == '\n') {
                        line++;
                    }
                    field.append(c);
                }
                continue;
            }

            if (afterQuote) {
                afterQuote = false;
                if (c == delimiter) {
                    endField(row, field);
                    fieldQuoted = false;
                    fieldStart = true;
                } else if (c == '\n') {
                    endField(row, field);
                    line++;
                    endRow(row, rowHandler);
                    atRowStart = true;
                    fieldQuoted = false;
                    fieldStart = true;
                } else if (c == '\r') {
                    consumeLf(in);
                    endField(row, field);
                    line++;
                    endRow(row, rowHandler);
                    atRowStart = true;
                    fieldQuoted = false;
                    fieldStart = true;
                } else if (c == ' ' || c == '\t') {
                    // Tolerate stray whitespace between closing quote and delimiter.
                    afterQuote = true;
                } else {
                    throw new CsvException(line,
                            "unexpected character '" + c + "' after closing quote");
                }
                continue;
            }

            if (fieldStart && c == quote && field.length() == 0) {
                inQuotes = true;
                quoteStartLine = line;
                fieldQuoted = true;
                atRowStart = false;
                fieldStart = false;
                continue;
            }

            if (c == delimiter) {
                endField(row, field);
                atRowStart = false;
                fieldStart = true;
            } else if (c == '\n') {
                endField(row, field);
                line++;
                endRow(row, rowHandler);
                atRowStart = true;
                fieldStart = true;
            } else if (c == '\r') {
                consumeLf(in);
                endField(row, field);
                line++;
                endRow(row, rowHandler);
                atRowStart = true;
                fieldStart = true;
            } else {
                field.append(c);
                atRowStart = false;
                fieldStart = false;
            }
        }

        if (inQuotes) {
            throw new CsvException(quoteStartLine, "unterminated quoted field");
        }
        // Flush a final row that does not end with a newline.
        if (!atRowStart || field.length() > 0 || !row.isEmpty()) {
            endField(row, field);
            endRow(row, rowHandler);
        }
    }

    /** Convenience: parse the whole input into a list of rows (small files only). */
    public List<List<String>> parseAll(Reader in) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        parse(in, rows::add);
        return rows;
    }

    private void endField(List<String> row, StringBuilder field) {
        row.add(field.toString());
        field.setLength(0);
    }

    private void endRow(List<String> row, Consumer<List<String>> rowHandler) {
        // Skip a completely empty line (a lone newline), but keep a row that
        // is a single empty quoted field.
        if (row.size() == 1 && row.get(0).isEmpty()) {
            row.clear();
            return;
        }
        rowHandler.accept(new ArrayList<>(row));
        row.clear();
    }

    private void consumeLf(java.io.PushbackReader in) throws IOException {
        int next = in.read();
        if (next != '\n' && next != -1) {
            in.unread(next);
        }
    }

    /** Pushes one character back so the main loop sees it again. */
    private void unread(java.io.PushbackReader in, int ch) throws IOException {
        in.unread(ch);
    }
}
