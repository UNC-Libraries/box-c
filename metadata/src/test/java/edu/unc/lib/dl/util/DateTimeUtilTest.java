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

import java.util.Date;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Gregory Jansen
 *
 */
public class DateTimeUtilTest extends Assert {

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
    public void utcToDateTest() throws Exception {
        Date plainDate = DateTimeUtil.parseUTCDateToDate("2001");
        DateTime date = new DateTime(plainDate.getTime());
        assertEquals(2001, date.getYear());

        plainDate = DateTimeUtil.parseUTCDateToDate("2001-05-08");
        date = new DateTime(plainDate.getTime());
        assertEquals(2001, date.getYear());
        assertEquals(5, date.getMonthOfYear());
        assertEquals(8, date.getDayOfMonth());

        try {
            plainDate = DateTimeUtil.parseUTCDateToDate("2002-02-01T12:13:14");
            fail();
        } catch (IllegalArgumentException e) {
        }
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
