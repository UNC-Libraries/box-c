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

import org.apache.solr.common.util.DateUtil;

public class DateFormatUtil {
	public static String getFormattedDate(String dateString, boolean inclusive, boolean endRange){
		return getFormattedDate(dateString, "yyyy-MM-dd'T'hh:mm:ss'Z'", inclusive, endRange);
	}
	
	/**
	 * Returns a date string formatted in ISO-8601, using UTC time.  The date is adjusted for use
	 * in ranges, either being the start or end of the range, and inclusive or exclusive.  The incoming date must
	 * be formatted in ISO-8601 format, and can have degrees of precision including from yyyy, yyyy-mm, yyyy-mm-dd
	 * or yyyy-mm-ddThh:mm:ss.SSS.  The modification to the date various based on inclusivity, side of the range
	 * it represents, and the level of precision in the date. 
	 * @param dateString
	 * @param format
	 * @param inclusive
	 * @param endRange
	 * @return
	 * @throws NumberFormatException
	 */
	public static String getFormattedDate(String dateString, String format, boolean inclusive, boolean endRange) throws NumberFormatException {
		if (dateString == null || dateString.length() == 0)
			return null;
		
		
		if (dateString.contains("T") && dateString.contains("Z")){
			//If the string is a full timestamp, is already in UTC and it is an inclusive request, simply return
			if (inclusive){
				return dateString;
			} else {
				//If it wasn't inclusive, then add or subtract 1 second
				SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
				Calendar calendar = Calendar.getInstance();
				try {
					calendar.setTime(formatter.parse(dateString));
					if (!inclusive){
						if (endRange){
							calendar.set(Calendar.SECOND, calendar.get(Calendar.SECOND) - 1);
						} else {
							calendar.set(Calendar.SECOND, calendar.get(Calendar.SECOND) + 1);
						}
					}
					return org.apache.solr.common.util.DateUtil.getThreadLocalDateFormat().format(calendar.getTime());
				} catch (ParseException e) {
					return null;
				}
			}
		}
		
		String[] dateArray = dateString.split("[-TZ]+");
		
		Calendar calendar = Calendar.getInstance();
		
		calendar.setTimeZone(TimeZone.getTimeZone("GMT"));
		
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		
		if (dateArray.length >= 1){
			calendar.set(Calendar.YEAR, Integer.parseInt(dateArray[0]));
		}
		if (dateArray.length >= 2){
			calendar.set(Calendar.MONTH, Integer.parseInt(dateArray[1])-1);
		} else {
			calendar.set(Calendar.MONTH, 0);
		}
		if (dateArray.length >= 3){
			calendar.set(Calendar.DATE, Integer.parseInt(dateArray[2]));
		} else {
			calendar.set(Calendar.DATE, 1);
		}
		
		if (dateArray.length == 1){
			if (inclusive){
				if (endRange){
					calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR)+1);
					calendar.set(Calendar.MILLISECOND, calendar.get(Calendar.MILLISECOND)-1);
				}
			} else {
				if (!endRange){
					calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR)+1);
				}
			}
		}
		
		if (dateArray.length == 2){
			if (inclusive){
				if (endRange){
					calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH)+1);
					calendar.set(Calendar.MILLISECOND, calendar.get(Calendar.MILLISECOND)-1);
				}
			} else {
				if (!endRange){
					calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH)+1);
				}
			}
		}
		
		if (dateArray.length == 3){
			if (inclusive){
				if (endRange){
					calendar.set(Calendar.DATE, calendar.get(Calendar.DATE)+1);
					calendar.set(Calendar.MILLISECOND, calendar.get(Calendar.MILLISECOND)-1);
				}
			} else {
				if (!endRange){
					calendar.set(Calendar.DATE, calendar.get(Calendar.DATE)+1);
				}
			}
		}
		
		return DateUtil.getThreadLocalDateFormat().format(calendar.getTime());
	}
}
