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
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * String manipulation utility methods for formatting and rendering string data
 * to web clients.
 *
 * @author bbpennel
 *
 */
public class StringFormatUtil {
    private static final Logger LOG = LoggerFactory.getLogger(StringFormatUtil.class);

    private static final long UNIT_FACTOR = 1024L;
    private static final String[] filesizeSuffixes = {"B", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"};

    private static final int DEFAULT_LAST_WORD_ALLOWANCE = 10;
    private static final double LAST_WORD_ALLOWANCE_MIN_LENGTH_FACTOR = 1.5;

    private StringFormatUtil() {
    }

    /**
     * Formats the filesize into a human readable format using the largest
     * applicable unit, with decimalPlaces number of decimals returned.
     *
     * @param filesize string containing the filesize in bytes
     * @param decimalPlaces number of decimal places to return. If 0, then no
     *            decimal places are returned.
     * @return formatted filesize, or the provided filesize string if it was not
     *         a number
     */
    public static String formatFilesize(String filesize, int decimalPlaces) {
        try {
            return formatFilesize(Long.parseLong(filesize), decimalPlaces);
        } catch (NumberFormatException e) {
            LOG.error("Failed to format filesize {}, not a valid number.", filesize);
        }
        return filesize;
    }

    /**
     * Formats the filesize into a human readable format using the largest
     * applicable unit, with decimalPlaces number of decimals returned.
     *
     * @param filesize filesize in bytes to format.
     * @param decimalPlaces number of decimal places to return. If 0, then no
     *            decimal places are returned.
     * @return Filesize formatted to largest unit that returns a whole number,
     *         plus the requested number of decimal places.
     */
    public static String formatFilesize(long filesize, int decimalPlaces) {
        int multipleCount = 0;
        long filesizeRemainder = 0;
        long filesizeValue = filesize;
        while (filesizeValue > UNIT_FACTOR && multipleCount < filesizeSuffixes.length) {
            filesizeRemainder = filesizeValue % UNIT_FACTOR;
            filesizeValue /= UNIT_FACTOR;
            multipleCount++;
        }
        StringBuilder filesizeBuilder = new StringBuilder();
        filesizeBuilder.append(filesizeValue);
        if (filesizeRemainder > 0 && decimalPlaces > 0) {
            int decimalMultiplier = 1;
            for (int i = 0; i < decimalPlaces; i++) {
                decimalMultiplier *= 10;
            }
            filesizeRemainder = (filesizeRemainder * decimalMultiplier) / UNIT_FACTOR;
            if (filesizeRemainder > 0) {
                filesizeBuilder.append(".").append(filesizeRemainder);
            }
        }

        filesizeBuilder.append(" ");
        if (multipleCount < filesizeSuffixes.length) {
            filesizeBuilder.append(filesizeSuffixes[multipleCount]);
        } else {
            filesizeBuilder.append(filesizeSuffixes[filesizeSuffixes.length - 1]);
        }
        return filesizeBuilder.toString();
    }

    /**
     * Truncates the given text to no more than the requested length, in a human
     * readable manner if possible.
     *
     * If this would cut off in the middle of a word, will attempt to truncate
     * to before this word within reason.
     *
     * @param text string to truncate
     * @param length maximum length of the truncated text
     * @return text truncated to be no more than the requested length.
     */
    public static String truncateText(String text, int length) {
        return truncateText(text, length, DEFAULT_LAST_WORD_ALLOWANCE);
    }

    private static String truncateText(String text, int length, int lastWordAllowance) {
        if (length < 0) {
            throw new IndexOutOfBoundsException();
        }

        if (text == null || text.length() <= length) {
            return text;
        }

        String truncated;
        if (length < text.length()) {
            truncated = text.substring(0, length);
        } else {
            truncated = text;
        }

        // Truncate text down to last word boundary if it occurs within
        // lastWordAllowance number of characters from the end of the truncated
        // text. Effectively this will try to cut off to the last whole word in
        // the text within the character limit
        if (truncated.length() > lastWordAllowance * LAST_WORD_ALLOWANCE_MIN_LENGTH_FACTOR) {
            String[] parts = truncated.split("\\b(?=\\w*$)");
            if (parts.length > 1) {
                int lastBoundaryIndex = parts[0].length();
                if (truncated.length() - lastBoundaryIndex < lastWordAllowance) {
                    truncated = parts[0].trim();
                }
            }
        }

        return truncated;
    }

    /**
     * Encodes the provided value into URL UTF-8 form.
     *
     * @param value
     * @return
     * @throws UnsupportedEncodingException
     */
    public static String urlEncode(String value) throws UnsupportedEncodingException {
        return URLEncoder.encode(value, "UTF-8");
    }

    /**
     * Removes a query parameter from a URL
     *
     * @param query
     * @param key
     * @return
     */
    public static String removeQueryParameter(String query, String key) {
        int index = query.indexOf("&" + key + "=");
        boolean isFirstParameter = false;
        if (index == -1) {
            index = query.indexOf(key + "=");
            if (index == -1) {
                return query;
            }
            isFirstParameter = index > 0 && query.charAt(index - 1) == '?';
            if (!isFirstParameter) {
                return query;
            }
        }

        int end = query.indexOf('&', index + 1);
        if (end == -1) {
            return query.substring(0, index - (isFirstParameter ? 1 : 0));
        }
        return query.substring(0, index) + query.substring(end + (isFirstParameter ? 1 : 0));
    }

    /**
     * Given a string as input, convert it to US-ASCII and replace any characters which would
     * prevent it from being parsed as a "token" as defined by RFC 1521 and RFC 2045.
     * @param input
     * @param replacement
     * @return
     */
    public static String makeToken(String input, String replacement) {
        String result = input;

        // Encode as US-ASCII, replacing malformed or unmappable input
        result = new String(StandardCharsets.US_ASCII.encode(result).array(), StandardCharsets.US_ASCII);

        // No space characters (includes newlines, etc. as well as just space and tab)
        result = result.replaceAll("\\s", replacement);

        // No control characters
        result = result.replaceAll("[^\\x20-\\x7f]", replacement);

        // No "tspecial" characters
        result = result.replaceAll("[()<>@,;:\\\\\\\"/\\[\\]?=]", replacement);

        return result;
    }

}
