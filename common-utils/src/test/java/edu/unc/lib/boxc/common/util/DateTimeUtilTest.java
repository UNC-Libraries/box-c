/**
 *
 */
package edu.unc.lib.boxc.common.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Date;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Gregory Jansen
 *
 */
public class DateTimeUtilTest {
    private final DateTimeZone defaultTimeZone = DateTimeZone.getDefault();

    @Before
    public void init() {
        DateTimeZone.setDefault(DateTimeZone.forID("EST"));
    }

    @After
    public void after() {
        DateTimeZone.setDefault(defaultTimeZone);
    }

    @Test
    public void parseUTCToDateYearTest() throws Exception {
        var date = DateTimeUtil.parseUTCToDate("2001");
        assertEquals("2001-01-01T00:00:00.000Z", DateTimeUtil.formatDateToUTC(date));
    }

    @Test
    public void parseUTCToDateYyyymmddTest() throws Exception {
        var date = DateTimeUtil.parseUTCToDate("2001-05-08");
        assertEquals("2001-05-08T00:00:00.000Z", DateTimeUtil.formatDateToUTC(date));
    }

    @Test
    public void parseUTCToDateTimestampUnspecifiedZoneTest() throws Exception {
        var date = DateTimeUtil.parseUTCToDate("2002-02-01T12:13:14");
        assertEquals("2002-02-01T12:13:14.000Z", DateTimeUtil.formatDateToUTC(date));
    }

    @Test
    public void parseUTCToDateTimestampWithZoneTest() throws Exception {
        var date = DateTimeUtil.parseUTCToDate("2002-02-01T12:13:14-05:00");
        assertEquals("2002-02-01T17:13:14.000Z", DateTimeUtil.formatDateToUTC(date));
    }

    @Test
    public void parseUTCToDateTimestampUtcMilliTest() throws Exception {
        var date = DateTimeUtil.parseUTCToDate("2004-02-04T12:13:14.005Z");
        assertEquals("2004-02-04T12:13:14.005Z", DateTimeUtil.formatDateToUTC(date));
    }

    @Test
    public void parseUTCToDateTimestampUtcMinuteTest() throws Exception {
        var date = DateTimeUtil.parseUTCToDate("2004-02-04T12:13Z");
        assertEquals("2004-02-04T12:13:00.000Z", DateTimeUtil.formatDateToUTC(date));
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseUTCToDateNotADateTest() throws Exception {
        DateTimeUtil.parseUTCToDate("not a date");
    }

    @Test
    public void formatDateToUTCTest() throws Exception {
        Date date = new Date(1407470400000L);
        assertEquals("2014-08-08T04:00:00.000Z", DateTimeUtil.formatDateToUTC(date));
    }

}
