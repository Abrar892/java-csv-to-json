package com.abrar.csvjson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class TypeInfererTest {

    @Test
    void emptyIsNull() {
        assertNull(TypeInferer.infer(""));
        assertNull(TypeInferer.infer(null));
    }

    @Test
    void booleansCaseInsensitive() {
        assertEquals(Boolean.TRUE, TypeInferer.infer("true"));
        assertEquals(Boolean.TRUE, TypeInferer.infer("TRUE"));
        assertEquals(Boolean.FALSE, TypeInferer.infer("False"));
    }

    @Test
    void integersBecomeLongs() {
        assertEquals(42L, TypeInferer.infer("42"));
        assertEquals(-7L, TypeInferer.infer("-7"));
        assertEquals(0L, TypeInferer.infer("0"));
    }

    @Test
    void decimalsBecomeDoubles() {
        assertEquals(3.14, TypeInferer.infer("3.14"));
        assertEquals(-0.5, TypeInferer.infer("-0.5"));
        assertEquals(1.0e5, TypeInferer.infer("1e5"));
    }

    @Test
    void leadingZeroStaysAString() {
        assertEquals("01234", TypeInferer.infer("01234"));
        assertEquals("007", TypeInferer.infer("007"));
    }

    @Test
    void plainTextStaysAString() {
        assertEquals("hello", TypeInferer.infer("hello"));
        assertEquals("12a", TypeInferer.infer("12a"));
        assertEquals("1.2.3", TypeInferer.infer("1.2.3"));
    }

    @Test
    void hugeIntegerFallsBackToDouble() {
        Object v = TypeInferer.infer("99999999999999999999999");
        assertEquals(1.0e23, v);
    }
}
