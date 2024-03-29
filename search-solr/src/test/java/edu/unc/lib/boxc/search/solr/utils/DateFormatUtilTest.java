package edu.unc.lib.boxc.search.solr.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import edu.unc.lib.boxc.search.solr.utils.DateFormatUtil;

/**
 *
 * @author bbpennel
 *
 */
public class DateFormatUtilTest {

    private final static String TIMESTAMP = "2017-12-12T01:20:55.111Z";

    @Test
    public void testFormatNoDate() {
        assertNull(DateFormatUtil.getFormattedDate(null, true, true));
    }

    @Test
    public void testFormatInclusiveTimestamp() {
        String formatted = DateFormatUtil.getFormattedDate(TIMESTAMP, true, true);

        assertEquals(TIMESTAMP, formatted);
    }

    @Test
    public void testFormatExclusiveTimestamp() {
        String formatted = DateFormatUtil.getFormattedDate(TIMESTAMP, false, false);

        assertEquals("2017-12-12T01:20:55.112Z", formatted);
    }

    @Test
    public void testFormatExclusiveEndTimestamp() {
        String formatted = DateFormatUtil.getFormattedDate(TIMESTAMP, false, true);

        assertEquals("2017-12-12T01:20:55.110Z", formatted);
    }

    @Test
    public void testFormatYearExclEndRange() {
        String formatted = DateFormatUtil.getFormattedDate("2017", false, true);

        assertEquals("2016-12-31T23:59:59.999Z", formatted);
    }

    @Test
    public void testFormatYearInclEndRange() {
        String formatted = DateFormatUtil.getFormattedDate("2017", true, true);

        assertEquals("2017-12-31T23:59:59.999Z", formatted);
    }

    @Test
    public void testFormatYearInclStartRange() {
        String formatted = DateFormatUtil.getFormattedDate("2017", true, false);

        assertEquals("2016-12-31T23:59:59.999Z", formatted);
    }

    @Test
    public void testFormatYearExclStartRange() {
        String formatted = DateFormatUtil.getFormattedDate("2017", false, false);

        assertEquals("2017-12-31T23:59:59.999Z", formatted);
    }

    @Test
    public void testFormatMonthExclEndRange() {
        String formatted = DateFormatUtil.getFormattedDate("2017-06", false, true);

        assertEquals("2017-05-31T23:59:59.999Z", formatted);
    }

    @Test
    public void testFormatMonthInclEndRange() {
        String formatted = DateFormatUtil.getFormattedDate("2017-06", true, true);

        assertEquals("2017-06-30T23:59:59.999Z", formatted);
    }

    @Test
    public void testFormatMonthInclStartRange() {
        String formatted = DateFormatUtil.getFormattedDate("2017-06", true, false);

        assertEquals("2017-05-31T23:59:59.999Z", formatted);
    }

    @Test
    public void testFormatMonthExclStartRange() {
        String formatted = DateFormatUtil.getFormattedDate("2017-06", false, false);

        assertEquals("2017-06-30T23:59:59.999Z", formatted);
    }

    @Test
    public void testFormatDayExclEndRange() {
        String formatted = DateFormatUtil.getFormattedDate("2017-06-20", false, true);

        assertEquals("2017-06-19T23:59:59.999Z", formatted);
    }

    @Test
    public void testFormatDayInclEndRange() {
        String formatted = DateFormatUtil.getFormattedDate("2017-06-20", true, true);

        assertEquals("2017-06-20T23:59:59.999Z", formatted);
    }

    @Test
    public void testFormatDayInclStartRange() {
        String formatted = DateFormatUtil.getFormattedDate("2017-06-20", true, false);

        assertEquals("2017-06-19T23:59:59.999Z", formatted);
    }

    @Test
    public void testFormatDayExclStartRange() {
        String formatted = DateFormatUtil.getFormattedDate("2017-06-20", false, false);

        assertEquals("2017-06-20T23:59:59.999Z", formatted);
    }
}
