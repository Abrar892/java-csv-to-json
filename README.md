# csvjson — CSV to JSON Converter CLI

A zero-dependency Java CLI that converts CSV files to JSON. It reads a CSV
file (or stdin) and writes a JSON array — or one JSON object per line
(NDJSON) — to a file (or stdout). Built for pipelines and one-off data
wrangling where you want a correct conversion without pulling in a library.

## Features

- **RFC 4180 CSV parsing** — quoted fields with embedded commas, newlines and
  doubled quotes, CRLF/LF/CR endings, UTF-8 BOM, custom delimiters
- **Streaming** — rows are converted one at a time, so a 10 GB file converts
  in constant memory
- **Type inference** — numbers, booleans and empty cells become real JSON
  values (`36`, `true`, `null`); `--no-infer` keeps everything a string
- **Nested objects** — dotted headers like `address.city` become nested JSON
  objects (`--no-nested` keeps them flat)
- **Strict errors** — a malformed row fails fast with its line number instead
  of silently producing bad JSON

## Quick start

```bash
./build.sh            # compiles and builds out/csvjson.jar
java -jar out/csvjson.jar data.csv
```

## Usage

```
csvjson [options] [input.csv]

  -o, --output <file>   write JSON to <file> instead of stdout
      --ndjson          one JSON object per line (default: JSON array)
      --pretty          pretty-print the JSON array
  -d, --delimiter <c>   field delimiter (default: ,)
      --no-infer        keep every value a string
      --no-nested       keep dotted headers flat
      --no-header       no header row; keys become col0, col1, ...
  -h, --help            show help
```

### Examples

```bash
# Convert people.csv to JSON on stdout
java -jar out/csvjson.jar people.csv

# Stream a huge file to NDJSON, one object per line
cat huge.csv | java -jar out/csvjson.jar --ndjson > huge.jsonl

# Semicolon-delimited export, no type inference, pretty output
java -jar out/csvjson.jar --delimiter ';' --no-infer --pretty export.csv -o export.json
```

Input:

```csv
name,age,address.city,address.zip,active
Ada,36,Lahore,54000,true
Bob,,Karachi,,false
```

Output:

```json
[{"name":"Ada","age":36,"address":{"city":"Lahore","zip":54000},"active":true},{"name":"Bob","age":null,"address":{"city":"Karachi","zip":null},"active":false}]
```

## Design decisions

1. **Hand-rolled state-machine parser, no dependencies.** CSV looks simple
   until a quoted field contains a comma or a newline; a small state machine
   handles all of RFC 4180 correctly and keeps the tool a single jar with
   nothing to install.
2. **Streaming by default.** The parser delivers rows through a callback and
   the writer flushes each object as it goes, so memory use stays flat no
   matter how large the input is. Nothing is ever loaded into a list.
3. **Inference with an escape hatch.** Type inference makes the JSON useful
   immediately (numbers sort and compare as numbers), but it is deliberately
   conservative — leading-zero values such as `01234` stay strings so zip
   codes and IDs are never mangled — and `--no-infer` turns it off entirely.

## Running the tests

```bash
./build.sh test
```

The suite is plain JUnit 5 (the console-standalone jar is downloaded once
from Maven Central on first run) and covers the parser edge cases, type
inference, JSON escaping and nesting, and end-to-end CLI conversions.

## Tech stack

- Java 17, zero runtime dependencies
- JUnit 5 for tests
