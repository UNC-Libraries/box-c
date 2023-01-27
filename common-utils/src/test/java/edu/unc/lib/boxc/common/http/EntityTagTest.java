package edu.unc.lib.boxc.common.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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

    @Test
    public void invalidEtagTest() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            String value = "123456789";

            new EntityTag(value);
        });
    }
}
