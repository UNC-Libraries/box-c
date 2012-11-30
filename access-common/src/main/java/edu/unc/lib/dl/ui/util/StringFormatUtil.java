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
package edu.unc.lib.dl.ui.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * String manipulation utility methods related to Solr data.
 * @author bbpennel
 *
 */
public class StringFormatUtil {

	private static String[] filesizeSuffixes = {"B", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"};

	/**
	 * Formats a file size string if it is a numeric representation stored in bytes.  If there 
	 * are non-numeric characters in the file size, then the filesize is returned.  Numeric file sizes
	 * are labeled with the highest applicable unit which returns a whole number, plus decimalPlaces 
	 * number of digits after the decimal.  If the value after the decimal would be 0, then no decimal
	 * point is returned. 
	 * @param filesize
	 * @param decimalPlaces
	 * @return
	 */
	public static String formatFilesize(String filesize, int decimalPlaces){
		try {
			int multipleCount = 0;
			long filesizeValue = Long.parseLong(filesize);
			long filesizeRemainder = 0;
			while (filesizeValue > 1024 && multipleCount < filesizeSuffixes.length){
				filesizeRemainder = filesizeValue % 1024;
				filesizeValue /= 1024;
				multipleCount++;
			}
			StringBuilder filesizeBuilder = new StringBuilder();
			filesizeBuilder.append(filesizeValue);
			if (filesizeRemainder > 0 && decimalPlaces > 0){
				int decimalMultiplier = 1;
				for (int i=0; i<decimalPlaces; i++){
					decimalMultiplier *= 10;
				}
				filesizeRemainder = (filesizeRemainder * decimalMultiplier)/1024;
				if (filesizeRemainder > 0)
					filesizeBuilder.append(".").append(filesizeRemainder);
			}
			
			filesizeBuilder.append(" ");
			if (multipleCount < filesizeSuffixes.length)
				filesizeBuilder.append(filesizeSuffixes[multipleCount]);
			else filesizeBuilder.append(filesizeSuffixes[filesizeSuffixes.length - 1]);
			return filesizeBuilder.toString();
		} catch (NumberFormatException e){
		}
		return filesize;
	}
	
	
	public static String truncateText(String text, int length){
		if (length < 0)
			throw new IndexOutOfBoundsException();
		
		if (text == null || text.length() <= length){
			return text;
		}
		Matcher m = Pattern.compile("(.|\n){0," + length + "}\\b").matcher(text);
		m.find();
		return m.group(0);
	}
	
	public static String urlEncode(String value) throws UnsupportedEncodingException {
	    return URLEncoder.encode(value, "UTF-8");
	}
}
