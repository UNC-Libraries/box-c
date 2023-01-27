/**
 *
 */
package edu.unc.lib.boxc.common.util;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Date;

import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.chrono.GJChronology;
import org.joda.time.format.ISODateTimeFormat;

/**
 * @author Gregory Jansen
 *
 */
public class DateTimeUtil {
    private DateTimeUtil() {
    }

    public final static DateTimeFormatter ISO_UTC_MILLISECONDS =
            new DateTimeFormatterBuilder().appendInstant(3).toFormatter();

    /**
     * Parse a UTC formatted timestamp as a date. Assumes UTC timezone if none specified in provided value
     * @param utcDate
     * @return
     */
    public static Date parseUTCToDate(String utcDate) {
        return parseUTCToDateTime(utcDate).toDate();
    }

    /**
     * This is only visible for testing until we switch to java 8 time implementation
     * Parses Julian or Gregorian dates, cutoff year is 1582.
     * @param utcDate
     * @return
     */
    protected static DateTime parseUTCToDateTime(String utcDate) {
        // TODO remove jodatime dependency. At the moment it is required for extensive ISO8601 parsing
        Chronology chrono = GJChronology.getInstanceUTC();
        DateTime isoDT = ISODateTimeFormat.dateTimeParser().withChronology(chrono).withOffsetParsed()
                .parseDateTime(utcDate);
        return isoDT.withZone(DateTimeZone.forID("UTC"));
    }

    /**
     * @param date
     * @return date formatted in UTC YYYY-MM-DDTHH:MM:SS.SSSZ format
     */
    public static String formatDateToUTC(Date date) {
        return ISO_UTC_MILLISECONDS.format(date.toInstant().atOffset(ZoneOffset.UTC));
    }
}
