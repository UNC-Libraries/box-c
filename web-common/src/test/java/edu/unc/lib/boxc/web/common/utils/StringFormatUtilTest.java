package edu.unc.lib.boxc.web.common.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import edu.unc.lib.boxc.web.common.utils.StringFormatUtil;

/**
 *
 * @author bbpennel
 *
 */
public class StringFormatUtilTest {
    private final static String BASE_URL = "http://example.com/path";

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
}
