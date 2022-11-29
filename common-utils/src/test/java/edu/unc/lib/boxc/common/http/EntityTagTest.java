package edu.unc.lib.boxc.common.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import edu.unc.lib.boxc.common.http.EntityTag;

public class EntityTagTest {
    @Test
    public void strongEtagTest() {
        String value = "123456789";
        String header = '"' + value + '"';

        EntityTag etag = new EntityTag(header);
        assertEquals(value, etag.getValue());
        assertFalse(etag.isWeak());
    }

    @Test
    public void weakEtagTest() {
        String value = "123456789";
        String header = "W/\"" + value + '"';

        EntityTag etag = new EntityTag(header);
        assertEquals(value, etag.getValue());
        assertTrue(etag.isWeak());
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidEtagTest() {
        String value = "123456789";

        new EntityTag(value);
    }
}
