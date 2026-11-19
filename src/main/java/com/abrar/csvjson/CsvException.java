package com.abrar.csvjson;

/** A malformed CSV row, reported with its 1-based line number. */
public final class CsvException extends RuntimeException {

    private final int line;

    public CsvException(int line, String message) {
        super(line > 0 ? "line " + line + ": " + message : message);
        this.line = line;
    }

    public int line() {
        return line;
    }
}
