/**
 *
 */
package edu.unc.lib.boxc.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Date;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Gregory Jansen
 *
 */
public class DateTimeUtilTest {
    private final DateTimeZone defaultTimeZone = DateTimeZone.getDefault();

    @BeforeEach
    public void init() {
        DateTimeZone.setDefault(DateTimeZone.forID("EST"));
    }

    @AfterEach
    public void after() {
        DateTimeZone.setDefault(defaultTimeZone);
    }

    @Test
    public void utcToDateTimeTest() throws Exception {
        DateTime date = DateTimeUtil.parseUTCToDateTime("2001");
        assertEquals(2001, date.getYear());

        date = DateTimeUtil.parseUTCToDateTime("2001-05-08");
        assertEquals(2001, date.getYear());
        assertEquals(5, date.getMonthOfYear());
        assertEquals(8, date.getDayOfMonth());

        date = DateTimeUtil.parseUTCToDateTime("2002-02-01T12:13:14");
        assertEquals(2002, date.getYear());
        assertEquals(2, date.getMonthOfYear());
        assertEquals(1, date.getDayOfMonth());
        assertEquals(17, date.getHourOfDay());
        assertEquals(13, date.getMinuteOfHour());
        assertEquals(14, date.getSecondOfMinute());

        date = DateTimeUtil.parseUTCToDateTime("2004-02-04T12:13:14.005");
        assertEquals(2004, date.getYear());
        assertEquals(2, date.getMonthOfYear());
        assertEquals(4, date.getDayOfMonth());
        assertEquals(17, date.getHourOfDay());
        assertEquals(13, date.getMinuteOfHour());
        assertEquals(14, date.getSecondOfMinute());
        assertEquals(5, date.getMillisOfSecond());

        date = DateTimeUtil.parseUTCToDateTime("2004-02-04T12:13:14+04:00");
        date = date.withZone(DateTimeZone.forID("UTC"));
        assertEquals(2004, date.getYear());
        assertEquals(2, date.getMonthOfYear());
        assertEquals(4, date.getDayOfMonth());
        assertEquals(8, date.getHourOfDay());
        assertEquals(13, date.getMinuteOfHour());
        assertEquals(14, date.getSecondOfMinute());
        assertEquals(0, date.getMillisOfSecond());

        date = DateTimeUtil.parseUTCToDateTime("2004-02-04T12:13:14.005Z");
        assertEquals(2004, date.getYear());
        assertEquals(2, date.getMonthOfYear());
        assertEquals(4, date.getDayOfMonth());
        assertEquals(12, date.getHourOfDay());
        assertEquals(13, date.getMinuteOfHour());
        assertEquals(14, date.getSecondOfMinute());
        assertEquals(5, date.getMillisOfSecond());

        date = DateTimeUtil.parseUTCToDateTime("2004-02-04T12:13Z");
        assertEquals(2004, date.getYear());
        assertEquals(2, date.getMonthOfYear());
        assertEquals(4, date.getDayOfMonth());
        assertEquals(12, date.getHourOfDay());
        assertEquals(13, date.getMinuteOfHour());
        assertEquals(0, date.getSecondOfMinute());
        assertEquals(0, date.getMillisOfSecond());

        try {
            date = DateTimeUtil.parseUTCToDateTime("not even close");
            fail();
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void formatDateToUTCTest() throws Exception {
        Date date = new Date(1407470400000L);
        assertEquals("2014-08-08T04:00:00.000Z", DateTimeUtil.formatDateToUTC(date));
    }

}
