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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

/**
 *
 * @author bbpennel
 *
 */
public class StringFormatUtilTest {
    private final static String BASE_URL = "http://example.com/path";

    @Test
    public void testTruncateText() {
        String text = StringUtils.repeat('1', 100);
        assertEquals(50, StringFormatUtil.truncateText(text, 50).length());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testTruncateTextNegativeIndex() {
        String text = StringUtils.repeat('1', 5);
        StringFormatUtil.truncateText(text, -1);
    }

    @Test
    public void testTruncateTextGreaterThanLength() {
        String text = StringUtils.repeat('1', 5);
        assertEquals(5, StringFormatUtil.truncateText(text, 10).length());
    }

    @Test
    public void testTruncateTextNull() {
        assertNull(StringFormatUtil.truncateText(null, 10));
    }

    @Test
    public void testTruncateTextWordBoundry() {
        String text = StringUtils.repeat("word ", 10);
        assertEquals(39, StringFormatUtil.truncateText(text, 42).length());
    }

    @Test
    public void testTruncateTextWordBoundryOutOfRange() {
        String text = StringUtils.repeat("reallyreallylongword ", 5);
        assertEquals(80, StringFormatUtil.truncateText(text, 80).length());
    }

    @Test
    public void testTruncateTextCutOffBeforeWordLongerThanAllowance() {
        String text = StringUtils.repeat("reallyreallylongword ", 5);
        assertEquals(62, StringFormatUtil.truncateText(text, 68).length());
    }

    @Test
    public void testRemoveOnlyQueryParameter() {
        String query = BASE_URL + "?param=val";
        assertEquals(BASE_URL, StringFormatUtil.removeQueryParameter(query, "param"));
    }

    @Test
    public void testRemoveSecondQueryParameter() {
        String query = BASE_URL + "?first=val&second=2";
        assertEquals(BASE_URL + "?first=val", StringFormatUtil.removeQueryParameter(query, "second"));
    }

    @Test
    public void testRemoveFirstQueryParameter() {
        String query = BASE_URL + "?first=val&second=2";
        assertEquals(BASE_URL + "?second=2", StringFormatUtil.removeQueryParameter(query, "first"));
    }

    @Test
    public void testRemoveQueryParameterNotPresent() {
        String query = BASE_URL + "?first=val&second=2";
        assertEquals(query, StringFormatUtil.removeQueryParameter(query, "other"));
    }

    @Test
    public void makeTokenTest() {
        assertEquals("blah", StringFormatUtil.makeToken("blah", "_"));
        assertEquals("caf_", StringFormatUtil.makeToken("café", "_"));
        assertEquals("lorem_ipsum_", StringFormatUtil.makeToken("lorem ipsum?", "_"));
        assertEquals("lorem__ipsum_", StringFormatUtil.makeToken("lorem? ipsum?", "_"));
        assertEquals("lorem___ipsum", StringFormatUtil.makeToken("lorem;  ipsum", "_"));
    }

    @Test
    public void testFormatFilesizeBytes() {
        assertEquals("6 B", StringFormatUtil.formatFilesize("6", 0));
    }

    @Test
    public void testFormatFilesizeGB() {
        long size = 562l * 1024l * 1024l * 1024l;
        assertEquals("562 GB", StringFormatUtil.formatFilesize(size, 1));
    }

    @Test
    public void testFormatFilesizeGBWithDecimal() {
        long size = (562l * 1024l * 1024l * 1024l) / 100;
        assertEquals("5.6 GB", StringFormatUtil.formatFilesize(size, 1));
    }
}
