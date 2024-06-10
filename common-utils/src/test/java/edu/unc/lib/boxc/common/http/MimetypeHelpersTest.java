package edu.unc.lib.boxc.common.http;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author bbpennel
 */
public class MimetypeHelpersTest {
    @Test
    public void isValidMimetypeNullTest() {
        String mimetype = null;
        assertFalse(MimetypeHelpers.isValidMimetype(mimetype));
    }

    @Test
    public void isValidMimetypeBlankTest() {
        String mimetype = "";
        assertFalse(MimetypeHelpers.isValidMimetype(mimetype));
    }

    @Test
    public void isValidMimetypeSymlinkTest() {
        String mimetype = "inode/symlink";
        assertFalse(MimetypeHelpers.isValidMimetype(mimetype));
    }

    @Test
    public void isValidMimetypeInvalidFormatTest() {
        String mimetype = "invalid format";
        assertFalse(MimetypeHelpers.isValidMimetype(mimetype));
    }

    @Test
    public void isValidMimetypeNefTest() {
        String mimetype = "image/x-nikon-nef";
        assertTrue(MimetypeHelpers.isValidMimetype(mimetype));
    }

    @Test
    public void formatMimetypeNullTest() {
        String mimetype = null;
        assertNull(MimetypeHelpers.formatMimetype(mimetype));
    }

    @Test
    public void formatMimetypeWithParamsTest() {
        String mimetype = "text/plain; charset=UTF-8";
        assertEquals("text/plain", MimetypeHelpers.formatMimetype(mimetype));
    }

    @Test
    public void formatMimetypeRegularTest() {
        String mimetype = "text/plain";
        assertEquals("text/plain", MimetypeHelpers.formatMimetype(mimetype));
    }
}
