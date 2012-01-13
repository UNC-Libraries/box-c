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
package edu.unc.lib.dl.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides utility functions for creating path slugs from other strings.
 * 
 * @author count0
 * 
 */
public class PathUtil {
	private static Pattern incremented = Pattern.compile("[_][0-9]+$");

	/**
	 * Adds an underscore and a number to a slug string. If the input string already ends with an underscore and a string
	 * of digits, then it will increment the number. (leading zeros on the digits are removed)
	 * 
	 * @param input
	 *           a slug or slug input string to increment
	 * @return an incremented slug string
	 */
	public static String incrementSlug(String input) {
		String result = makeSlug(input);
		// if ends with _digit, then increment digit, other wise add _1
		Matcher m = incremented.matcher(result);
		if (m.find()) {
			String num = result.substring(m.start() + 1);
			int i = Integer.parseInt(num);
			i++;
			result = result.substring(0, m.start() + 1) + i;
		} else {
			result = result + "_1";
		}
		return result;
	}

	/**
	 * Tests to see if this string can be a valid repository path.
	 * 
	 * @param input
	 *           path test string
	 * @return result of test
	 */
	public static boolean isValidPathString(String input) {
		boolean result = false;
		result = input.matches("[a-zA-Z0-9_/\\-\\.]+");
		result = result && (input.startsWith("REPOSITORY/") || input.startsWith("/"));
		result = result && !input.contains("//");
		result = result && !input.endsWith("/");
		result = result && (!input.contains("/_") && !input.contains("_/"));
		return result;
	}

	/**
	 * Replaces non-alphanumeric character groups with an underscore and removes any leading or trailing underscores.
	 * 
	 * @param input
	 *           a string that provide input for the slug
	 * @return a slug string
	 */
	public static String makeSlug(String input) {
		String result = null;
		result = input.replaceAll("[^a-zA-Z0-9_\\-\\.]+", "_");
		result = result.replaceAll("_+$", "");
		result = result.replaceAll("^_+", "");
		return result;
	}

}
