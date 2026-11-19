package com.abrar.csvjson;

/**
 * Infers a JSON value type from a raw CSV cell.
 *
 * <p>Rules, in order: an empty cell is {@code null}; {@code true}/{@code false}
 * (any case) are booleans; an integer literal is a long; a decimal or exponent
 * literal is a double; anything else stays a string. Leading zeroes are kept
 * as strings, so values like zip codes ({@code 01234}) and IDs are never
 * mangled into numbers.
 */
public final class TypeInferer {

    private TypeInferer() {
    }

    /** Returns the inferred value: null, Boolean, Long, Double or String. */
    public static Object infer(String cell) {
        if (cell == null || cell.isEmpty()) {
            return null;
        }
        if (cell.equalsIgnoreCase("true")) {
            return Boolean.TRUE;
        }
        if (cell.equalsIgnoreCase("false")) {
            return Boolean.FALSE;
        }
        if (isInteger(cell)) {
            try {
                return Long.parseLong(cell);
            } catch (NumberFormatException e) {
                // Too big for a long: fall through to double.
            }
            try {
                return Double.parseDouble(cell);
            } catch (NumberFormatException e) {
                // Fall through to string.
            }
        }
        if (isDecimal(cell)) {
            try {
                return Double.parseDouble(cell);
            } catch (NumberFormatException e) {
                // Fall through to string.
            }
        }
        return cell;
    }

    private static boolean isInteger(String s) {
        int i = 0;
        if (s.charAt(0) == '-' || s.charAt(0) == '+') {
            i = 1;
        }
        if (i == s.length()) {
            return false;
        }
        // A leading zero means an ID/code, not a number ("01234" stays a string).
        if (s.length() - i > 1 && s.charAt(i) == '0') {
            return false;
        }
        for (; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isDecimal(String s) {
        // One optional sign, digits with at most one dot, optional exponent.
        return s.matches("[+-]?(\\d+\\.\\d*|\\.\\d+|\\d+)([eE][+-]?\\d+)?")
                && (s.contains(".") || s.contains("e") || s.contains("E"));
    }
}
