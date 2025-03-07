package edu.unc.lib.boxc.auth.api.services;

import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EmbargoUtilTest {
    @Test
    public void isEmbargoActiveTrueTest() {
        // 1900 + 2000 = 3900
        var date = new Date( 2000, Calendar.FEBRUARY,1);
        assertTrue(EmbargoUtil.isEmbargoActive(ldate));
    }

    @Test
    public void isEmbargoActiveFalseTest() {
        // 1900 + 100 = 2000
        var date = new Date( 100, Calendar.FEBRUARY,1);
        assertFalse(EmbargoUtil.isEmbargoActive(date));
    }

    @Test
    public void isEmbargoActiveEmbargoUntilNullTest() {
        assertFalse(EmbargoUtil.isEmbargoActive(null));
    }
}
