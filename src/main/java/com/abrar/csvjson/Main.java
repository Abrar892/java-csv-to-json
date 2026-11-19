package com.abrar.csvjson;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * csvjson — convert CSV to JSON from the command line.
 *
 * <p>Reads a CSV file (or stdin) and writes JSON to a file (or stdout).
 * Zero dependencies and streaming, so files of any size convert in constant
 * memory. The first row is the header unless {@code --no-header} is given.
 */
public final class Main {

    private static final String USAGE = """
            Usage: csvjson [options] [input.csv]

              Reads CSV from <input.csv> (or stdin when omitted) and writes JSON
              to stdout (or --output). The first row is the header row.

            Options:
              -o, --output <file>   write JSON to <file> instead of stdout
                  --ndjson          one JSON object per line (default: JSON array)
                  --pretty          pretty-print the JSON array
              -d, --delimiter <c>   field delimiter (default: ,)
                  --no-infer        keep every value a string (no type inference)
                  --no-nested       keep dotted headers flat (no nested objects)
                  --no-header       no header row; keys become col0, col1, ...
              -h, --help            show this help
            """;

    public static void main(String[] args) {
        try {
            run(args);
        } catch (CsvException e) {
            System.err.println("csvjson: " + e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            System.err.println("csvjson: " + e.getMessage());
            System.exit(1);
        }
    }

    static void run(String[] args) throws IOException {
        Options opts = Options.parse(args);
        if (opts.help) {
            System.out.print(USAGE);
            return;
        }
        try (Reader in = openInput(opts.input);
             Writer out = openOutput(opts.output)) {
            new Converter(opts, in, out).convert();
        }
    }

    /** Holds the per-run conversion state: headers, writer and row count. */
    static final class Converter {
        private final Options opts;
        private final Reader in;
        private final Writer out;
        private List<String> headers;
        private int rowNumber;

        Converter(Options opts, Reader in, Writer out) {
            this.opts = opts;
            this.in = in;
            this.out = out;
        }

        void convert() throws IOException {
            CsvParser parser = new CsvParser(opts.delimiter, '"');
            JsonWriter writer = new JsonWriter(out, opts.ndjson, opts.pretty, opts.nested);
            writer.begin();
            parser.parse(in, row -> {
                rowNumber++;
                try {
                    if (headers == null) {
                        headers = resolveHeaders(row);
                        if (opts.noHeader) {
                            // The first row is data, not a header: write it too.
                            writer.writeRow(headers, valuesFor(row));
                        }
                        return;
                    }
                    writer.writeRow(headers, valuesFor(row));
                } catch (IOException e) {
                    throw new CsvException(rowNumber, "cannot write output: " + e.getMessage());
                }
            });
            writer.end();
        }

        private List<String> resolveHeaders(List<String> firstRow) {
            if (!opts.noHeader) {
                return new ArrayList<>(firstRow);
            }
            List<String> generated = new ArrayList<>();
            for (int i = 0; i < firstRow.size(); i++) {
                generated.add("col" + i);
            }
            return generated;
        }

        private List<Object> valuesFor(List<String> row) {
            if (row.size() > headers.size()) {
                throw new CsvException(rowNumber, "row has " + row.size()
                        + " fields but the header has " + headers.size());
            }
            List<Object> values = new ArrayList<>();
            for (int i = 0; i < headers.size(); i++) {
                String cell = i < row.size() ? row.get(i) : "";
                values.add(opts.infer ? TypeInferer.infer(cell) : cell);
            }
            return values;
        }
    }

    private static Reader openInput(Path input) throws IOException {
        if (input == null) {
            return new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        }
        return Files.newBufferedReader(input, StandardCharsets.UTF_8);
    }

    private static Writer openOutput(Path output) throws IOException {
        if (output == null) {
            return new BufferedWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8));
        }
        return Files.newBufferedWriter(output, StandardCharsets.UTF_8);
    }

    /** Parsed command-line options. */
    static final class Options {
        Path input;
        Path output;
        boolean ndjson;
        boolean pretty;
        char delimiter = ',';
        boolean infer = true;
        boolean nested = true;
        boolean noHeader;
        boolean help;

        static Options parse(String[] args) {
            Options o = new Options();
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "-h", "--help" -> o.help = true;
                    case "-o", "--output" -> o.output = Path.of(requireValue(args, ++i, "--output"));
                    case "--ndjson" -> o.ndjson = true;
                    case "--pretty" -> o.pretty = true;
                    case "-d", "--delimiter" -> {
                        String v = requireValue(args, ++i, "--delimiter");
                        if (v.length() != 1) {
                            throw new CsvException(0, "--delimiter takes a single character");
                        }
                        o.delimiter = v.charAt(0);
                    }
                    case "--no-infer" -> o.infer = false;
                    case "--no-nested" -> o.nested = false;
                    case "--no-header" -> o.noHeader = true;
                    default -> {
                        if (args[i].startsWith("-")) {
                            throw new CsvException(0, "unknown option: " + args[i]);
                        }
                        if (o.input != null) {
                            throw new CsvException(0, "only one input file is allowed");
                        }
                        o.input = Path.of(args[i]);
                    }
                }
            }
            return o;
        }

        private static String requireValue(String[] args, int i, String flag) {
            if (i >= args.length) {
                throw new CsvException(0, flag + " needs a value");
            }
            return args[i];
        }
    }
}
