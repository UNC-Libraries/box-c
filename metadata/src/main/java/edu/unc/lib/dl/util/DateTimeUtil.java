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
import java.text.SimpleDateFormat;
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
public class DateTimeUtil {

	/**
	 * Parse a date in any ISO 8601 format. Default TZ is based on Locale.
	 *
	 * @param isoDate
	 *           ISO8601 date/time string with or without TZ offset
	 * @return a Joda DateTime object in UTC (call toString() to print)
	 */
	public static DateTime parseISO8601toUTC(String isoDate) {
		DateTime result = null;
		DateTimeFormatter fmt = ISODateTimeFormat.dateTimeParser().withOffsetParsed();
		// TODO what about preserving the precision of the original?
		DateTime isoDT = fmt.parseDateTime(isoDate);
		if (isoDT.year().get() > 9999) {
			// you parsed my month as part of the year!
			try {
				fmt = DateTimeFormat.forPattern("yyyyMMdd");
				fmt = fmt.withZone(DateTimeZone.forID("America/New_York"));
				isoDT = fmt.parseDateTime(isoDate);
			} catch (IllegalArgumentException e) {
				try {
					fmt = DateTimeFormat.forPattern("yyyyMM");
					fmt = fmt.withZone(DateTimeZone.forID("America/New_York"));
					isoDT = fmt.parseDateTime(isoDate);
				} catch (IllegalArgumentException e1) {
					try {
						fmt = DateTimeFormat.forPattern("yyyy");
						fmt = fmt.withZone(DateTimeZone.forID("America/New_York"));
						isoDT = fmt.parseDateTime(isoDate);
					} catch (IllegalArgumentException ignored) {
						// I guess we go with first parse?
					}
				}
			}
		}
		result = isoDT.withZoneRetainFields(DateTimeZone.forID("Etc/UTC"));
		return result;
	}
	
	private final static DateTimeFormatter utcFormatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(DateTimeZone.UTC);
	private final static DateTimeFormatter utcYMDFormatter = DateTimeFormat.forPattern("yyyy-MM-dd").withZone(DateTimeZone.UTC);
	private final static DateTimeFormatter utcYMDHMSFormatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(DateTimeZone.UTC);
	
	public static Date parseUTCToDate(String utcDate) throws ParseException {
		DateTime dateTime = utcFormatter.parseDateTime(utcDate);
		return dateTime.toDate();
	}
	
	public static Date parsePartialUTCToDate(String utcDate) throws ParseException {
		if (utcDate.length() == 10) {
			DateTime dateTime = utcYMDFormatter.parseDateTime(utcDate);
			return dateTime.toDate();
		}
		if (utcDate.length() == 19) {
			DateTime dateTime = utcYMDHMSFormatter.parseDateTime(utcDate);
			return dateTime.toDate();
		}
		DateTime dateTime = utcFormatter.parseDateTime(utcDate);
		return dateTime.toDate();
	}
	
	public static String formatDateToUTC(Date date) throws ParseException {
		DateTime dateTime = new DateTime(date);
		return utcFormatter.print(dateTime);
	}
}
