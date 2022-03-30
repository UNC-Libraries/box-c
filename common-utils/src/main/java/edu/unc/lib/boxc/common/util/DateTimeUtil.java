/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
     * Parse a UTC formatted timestamp as a date
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
        Chronology chrono = GJChronology.getInstance();
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
