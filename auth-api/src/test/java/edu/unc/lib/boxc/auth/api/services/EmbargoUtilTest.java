package edu.unc.lib.boxc.auth.api.services;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EmbargoUtilTest {
    public static long ONE_MINUTE = 60000L;
    @Test
    public void isEmbargoActiveTrueTest() {
        var date = new Date(System.currentTimeMillis() + ONE_MINUTE);
        assertTrue(EmbargoUtil.isEmbargoActive(date));
    }

    @Test
    public void isEmbargoActiveFalseTest() {
        var date = new Date(System.currentTimeMillis() - ONE_MINUTE);
        assertFalse(EmbargoUtil.isEmbargoActive(date));
    }

    @Test
    public void isEmbargoActiveEmbargoUntilNullTest() {
        assertFalse(EmbargoUtil.isEmbargoActive(null));
    }
}
