package edu.unc.lib.boxc.fcrepo.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author bbpennel
 */
public class RangeNotSatisfiableExceptionTest {
    @Test
    public void testConstructorWithMessage() {
        var ex = new RangeNotSatisfiableException("Bad range");
        assertEquals("Bad range", ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    public void testConstructorWithThrowable() {
        var causeEx = new RuntimeException();
        var ex = new RangeNotSatisfiableException(causeEx);
        assertEquals(causeEx, ex.getCause());
    }

    @Test
    public void testConstructorWithMessageThrowable() {
        var causeEx = new RuntimeException();
        var ex = new RangeNotSatisfiableException("Bad range", causeEx);
        assertEquals("Bad range", ex.getMessage());
        assertEquals(causeEx, ex.getCause());
    }
}
