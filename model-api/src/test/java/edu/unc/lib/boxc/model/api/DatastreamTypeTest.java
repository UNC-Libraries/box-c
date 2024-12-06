package edu.unc.lib.boxc.model.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author bbpennel
 */
public class DatastreamTypeTest {
    @Test
    void testGetId() {
        assertEquals("access_surrogate", DatastreamType.ACCESS_SURROGATE.getId());
        assertEquals("alt_text", DatastreamType.ALT_TEXT.getId());
    }

    @Test
    void testGetMimetype() {
        assertEquals("application/octet-stream", DatastreamType.ACCESS_SURROGATE.getMimetype());
        assertEquals("text/plain", DatastreamType.ALT_TEXT.getMimetype());
        assertNull(DatastreamType.ORIGINAL_FILE.getMimetype());
    }

    @Test
    void testGetExtension() {
        assertEquals("txt", DatastreamType.ALT_TEXT.getExtension());
        assertEquals("m4a", DatastreamType.AUDIO_ACCESS_COPY.getExtension());
        assertNull(DatastreamType.ORIGINAL_FILE.getExtension());
    }

    @Test
    void testGetContainer() {
        assertEquals("md", DatastreamType.ALT_TEXT.getContainer());
        assertEquals("datafs", DatastreamType.ORIGINAL_FILE.getContainer());
        assertNull(DatastreamType.ACCESS_SURROGATE.getContainer());
    }

    @Test
    void testGetStoragePolicy() {
        assertEquals(StoragePolicy.INTERNAL, DatastreamType.ALT_TEXT.getStoragePolicy());
        assertEquals(StoragePolicy.EXTERNAL, DatastreamType.ACCESS_SURROGATE.getStoragePolicy());
    }

    @Test
    void testGetDefaultFilename() {
        assertEquals("alt_text.txt", DatastreamType.ALT_TEXT.getDefaultFilename());
        assertEquals("audio.m4a", DatastreamType.AUDIO_ACCESS_COPY.getDefaultFilename());
        assertEquals("original_file.null", DatastreamType.ORIGINAL_FILE.getDefaultFilename());
    }

    @Test
    void testGetByIdentifier() {
        assertEquals(DatastreamType.ACCESS_SURROGATE, DatastreamType.getByIdentifier("access_surrogate"));
        assertEquals(DatastreamType.ALT_TEXT, DatastreamType.getByIdentifier("alt_text"));
        assertNull(DatastreamType.getByIdentifier("non_existent_id"));
    }
}
