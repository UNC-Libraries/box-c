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
package edu.unc.lib.dl.search.solr.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Utility method for formatting dates to use in solr queries
 *
 * @author bbpennel
 *
 */
public abstract class DateFormatUtil {
    public static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    static {
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    /**
     * Processes an ISO-8601 timestamp and returns it adjusted for use in solr date ranges.
     *
     * @param dateString date string in iso-8601 format for adjustment.  Supports yyyy, yyyy-mm, yyyy-mm-dd
     * or yyyy-mm-ddThh:mm:ss.SSS formats.
     * @param inclusive whether to include the exact provided timestamp or not
     * @param endRange indicates whether the date being formatted is at the beginning or end of the date range
     * @return adjusted date in ISO-8601 format
     */
    public static String getFormattedDate(String dateString, boolean inclusive, boolean endRange) {
        if (dateString == null || dateString.length() == 0) {
            return null;
        }

        if (dateString.contains("T") && dateString.contains("Z")) {
            //If the string is a full timestamp, is already in UTC and it is an inclusive request, simply return
            if (inclusive) {
                return dateString;
            } else {
                //If it wasn't inclusive, then add or subtract 1 second
                Calendar calendar = Calendar.getInstance();
                try {
                    calendar.setTime(formatter.parse(dateString));
                    if (!inclusive) {
                        if (endRange) {
                            calendar.add(Calendar.MILLISECOND, -1);
                        } else {
                            calendar.add(Calendar.MILLISECOND, 1);
                        }
                    }

                    return formatter.format(calendar.getTime());
                } catch (ParseException e) {
                    return null;
                }
            }
        }

        String[] dateArray = dateString.split("[-TZ]+");

        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // Extract the units of the date to the highest level of precision provided
        if (dateArray.length >= 1) {
            calendar.set(Calendar.YEAR, Integer.parseInt(dateArray[0]));
        }
        if (dateArray.length >= 2) {
            calendar.set(Calendar.MONTH, Integer.parseInt(dateArray[1]) - 1);
        } else {
            // Month is 0 based
            calendar.set(Calendar.MONTH, 0);
        }
        if (dateArray.length >= 3) {
            calendar.set(Calendar.DATE, Integer.parseInt(dateArray[2]));
        } else {
            calendar.set(Calendar.DATE, 1);
        }

        // Adjust the date by one increment of the significant unit if it
        // is either an inclusive end of range, or an exclusive start of a range
        if (dateArray.length == 1) {
            if (inclusive == endRange) {
                calendar.add(Calendar.YEAR, 1);
            }
        }

        if (dateArray.length == 2) {
            if (inclusive == endRange) {
                calendar.add(Calendar.MONTH, 1);
            }
        }

        if (dateArray.length == 3) {
            if (inclusive == endRange) {
                calendar.add(Calendar.DATE, 1);
            }
        }

        // Subtract one millisecond from the timestamp to go from
        // 2000 to 1999-12-31T23:59:59:999Z
        calendar.add(Calendar.MILLISECOND, -1);

        return formatter.format(calendar.getTime());
    }
}
