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
package edu.unc.lib.dl.util;

import java.text.ParseException;
import java.util.Date;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

/**
 * @author Gregory Jansen
 *
 */
public abstract class DateTimeUtil {

    public final static DateTimeFormatter utcFormatter = DateTimeFormat
            .forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(
                    DateTimeZone.UTC);
    public final static DateTimeFormatter utcYMDFormatter = DateTimeFormat
            .forPattern("yyyy-MM-dd").withZone(DateTimeZone.UTC);

    public static Date parseUTCDateToDate(String utcDate) throws ParseException {
        return ISODateTimeFormat.dateParser().parseDateTime(utcDate).toDate();
    }

    public static Date parseUTCToDate(String utcDate) throws ParseException {
        try {
            return parseUTCToDateTime(utcDate).toDate();
        } catch (IllegalArgumentException e) {
            throw new ParseException("Unparseable date: " + utcDate, 0);
        }
    }

    public static DateTime parseUTCToDateTime(String utcDate)
            throws IllegalArgumentException {
        DateTime isoDT = ISODateTimeFormat.dateTimeParser().withOffsetParsed()
                .parseDateTime(utcDate);
        return isoDT.withZone(DateTimeZone.forID("UTC"));
    }

    public static String formatDateToUTC(Date date) throws ParseException {
        DateTime dateTime = new DateTime(date);
        return utcFormatter.print(dateTime);
    }
}
