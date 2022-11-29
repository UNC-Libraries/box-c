package edu.unc.lib.boxc.search.solr.models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import edu.unc.lib.boxc.search.api.models.Datastream;
import edu.unc.lib.boxc.search.solr.models.DatastreamImpl;

/**
 *
 * @author bbpennel
 *
 */
public class DatastreamTest {
    private static final String CHECKSUM = "urn:sha1:2c9e902133d6809b445942dca5a93a9b3e766c15";

    @Test
    public void testToString() {
        Datastream ds = new DatastreamImpl(null, "original", 555l, "text/plain", "file.txt", "txt", CHECKSUM, null);

        assertEquals("original", ds.getName());
        assertEquals("text/plain", ds.getMimetype());
        assertEquals("file.txt", ds.getFilename());
        assertEquals("txt", ds.getExtension());
        assertEquals(555, ds.getFilesize().longValue());
        assertEquals(CHECKSUM, ds.getChecksum());
        assertEmpty(ds.getOwner());

        assertEquals("original|text/plain|file.txt|txt|555|" + CHECKSUM + "||", ds.toString());
    }

    @Test
    public void testDatastreamParsing() {
        Datastream ds = new DatastreamImpl("original_file|image/jpeg|file.jpg|jpg"
                + "|30459|" + CHECKSUM + "||");

        assertEquals("original_file", ds.getName());
        assertEquals("image/jpeg", ds.getMimetype());
        assertEquals("file.jpg", ds.getFilename());
        assertEquals("jpg", ds.getExtension());
        assertEquals(30459, ds.getFilesize().longValue());
        assertEquals(CHECKSUM, ds.getChecksum());
        assertEmpty(ds.getOwner());
    }

    @Test
    public void testDatastreamSurrogateParsing() {
        Datastream ds = new DatastreamImpl("DATA_FILE|image/jpeg|file.jpg|jpg|30459|" + CHECKSUM + "|uuid:73247248-e351-49dc-9b27-fe44df3884e7|");

        assertEquals("DATA_FILE", ds.getName());
        assertEquals("image/jpeg", ds.getMimetype());
        assertEquals("file.jpg", ds.getFilename());
        assertEquals("jpg", ds.getExtension());
        assertEquals(30459, ds.getFilesize().longValue());
        assertEquals(CHECKSUM, ds.getChecksum());
        assertEquals("uuid:73247248-e351-49dc-9b27-fe44df3884e7", ds.getOwner());
    }

    @Test
    public void testDatastreamNoChecksum() {
        Datastream ds = new DatastreamImpl("AUDIT|text/xml|audit|xml|30459|||");

        assertEquals("AUDIT", ds.getName());
        assertEquals("text/xml", ds.getMimetype());
        assertEquals("audit", ds.getFilename());
        assertEquals("xml", ds.getExtension());
        assertEquals(30459, ds.getFilesize().longValue());
        assertEmpty(ds.getChecksum());
        assertEmpty(ds.getOwner());
    }

    @Test
    public void testDatastreamNoChecksumFromSurrogate() {
        Datastream ds = new DatastreamImpl("AUDIT|text/xml|audit|xml|30459||uuid:73247248-e351-49dc-9b27-fe44df3884e7|");

        assertEquals("AUDIT", ds.getName());
        assertEquals("text/xml", ds.getMimetype());
        assertEquals("audit", ds.getFilename());
        assertEquals("xml", ds.getExtension());
        assertEquals(30459, ds.getFilesize().longValue());
        assertEmpty(ds.getChecksum());
        assertEquals("uuid:73247248-e351-49dc-9b27-fe44df3884e7", ds.getOwner());
    }

    @Test
    public void testDatastreamEqualsString() {
        String dsName = "data_stream";
        Datastream ds = new DatastreamImpl("data_stream|image/jpeg|ds.jpg|jpg|0|||240x750");

        assertTrue(ds.equals(dsName));
    }

    @Test
    public void testDatastreamNotEqualsString() {
        String dsName = "other_ds";
        Datastream ds = new DatastreamImpl("data_stream|image/jpeg|ds.jpg|jpg|0|||240x750");

        assertFalse(ds.equals(dsName));
    }

    @Test
    public void testDatastreamEqualsFullDSString() {
        String dsName = "data_stream|image/jpeg|ds.jpg|jpg|0|||240x750";
        Datastream ds = new DatastreamImpl("data_stream|image/jpeg|ds.jpg|jpg|0|||240x750");

        assertTrue(ds.equals(dsName));
    }

    @Test
    public void testDatastreamEqualsDatastream() {
        Datastream ds1 = new DatastreamImpl("data_stream|image/jpeg|ds.jpg|jpg|0|||240x750");
        Datastream ds2 = new DatastreamImpl("data_stream|image/jpeg|ds.jpg|jpg|0|||240x750");

        assertTrue(ds1.equals(ds2));
    }

    @Test
    public void testDatastreamEqualsDatastreamByNameAndOwner() {
        Datastream ds1 = new DatastreamImpl("data_stream|image/jpeg|ds.jpg|jpg|0||owner_id|");
        Datastream ds2 = new DatastreamImpl("data_stream||||||owner_id|");

        assertTrue(ds1.equals(ds2));
    }

    @Test
    public void testDatastreamNotEqualsDatastreamByOwner() {
        Datastream ds1 = new DatastreamImpl("data_stream|image/jpeg|ds.jpg|jpg|0||owner_2|");
        Datastream ds2 = new DatastreamImpl("data_stream|image/jpeg|ds.jpg|jpg|0||owner_1|");

        assertFalse(ds1.equals(ds2));
    }

    private static void assertEmpty(String string) {
        assertTrue(string == null || string.length() == 0);
    }
}
