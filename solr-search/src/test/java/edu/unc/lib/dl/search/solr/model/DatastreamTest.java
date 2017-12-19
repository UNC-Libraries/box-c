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
package edu.unc.lib.dl.search.solr.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 *
 * @author bbpennel
 *
 */
public class DatastreamTest {

    @Test
    public void datastreamParsing() {
        Datastream ds = new Datastream("DATA_FILE|image/jpeg|jpg|30459|dc93eff50ca7dbd688971716e55e0084|");

        assertEquals("DATA_FILE", ds.getName());
        assertEquals("image/jpeg", ds.getMimetype());
        assertEquals("jpg", ds.getExtension());
        assertEquals(30459, ds.getFilesize().longValue());
        assertEquals("dc93eff50ca7dbd688971716e55e0084", ds.getChecksum());
        assertEmpty(ds.getOwner());
    }

    @Test
    public void datastreamSurrogateParsing() {
        Datastream ds = new Datastream("DATA_FILE|image/jpeg|jpg|30459|dc93eff50ca7dbd688971716e55e0084|uuid:73247248-e351-49dc-9b27-fe44df3884e7");

        assertEquals("DATA_FILE", ds.getName());
        assertEquals("image/jpeg", ds.getMimetype());
        assertEquals("jpg", ds.getExtension());
        assertEquals(30459, ds.getFilesize().longValue());
        assertEquals("dc93eff50ca7dbd688971716e55e0084", ds.getChecksum());
        assertEquals("uuid:73247248-e351-49dc-9b27-fe44df3884e7", ds.getOwner());
    }

    @Test
    public void datastreamNoChecksum() {
        Datastream ds = new Datastream("AUDIT|text/xml|xml|30459||");

        assertEquals("AUDIT", ds.getName());
        assertEquals("text/xml", ds.getMimetype());
        assertEquals("xml", ds.getExtension());
        assertEquals(30459, ds.getFilesize().longValue());
        assertEmpty(ds.getChecksum());
        assertEmpty(ds.getOwner());
    }

    @Test
    public void datastreamNoChecksumFromSurrogate() {
        Datastream ds = new Datastream("AUDIT|text/xml|xml|30459||uuid:73247248-e351-49dc-9b27-fe44df3884e7");

        assertEquals("AUDIT", ds.getName());
        assertEquals("text/xml", ds.getMimetype());
        assertEquals("xml", ds.getExtension());
        assertEquals(30459, ds.getFilesize().longValue());
        assertEmpty(ds.getChecksum());
        assertEquals("uuid:73247248-e351-49dc-9b27-fe44df3884e7", ds.getOwner());
    }

    @Test
    public void testDatastreamEqualsString() {
        String dsName = "data_stream";
        Datastream ds = new Datastream("data_stream|image/jpeg|jpg|0||");

        assertTrue(ds.equals(dsName));
    }

    @Test
    public void testDatastreamNotEqualsString() {
        String dsName = "other_ds";
        Datastream ds = new Datastream("data_stream|image/jpeg|jpg|0||");

        assertFalse(ds.equals(dsName));
    }

    @Test
    public void testDatastreamEqualsFullDSString() {
        String dsName = "data_stream|image/jpeg|jpg|0||";
        Datastream ds = new Datastream("data_stream|image/jpeg|jpg|0||");

        assertTrue(ds.equals(dsName));
    }

    @Test
    public void testDatastreamEqualsDatastream() {
        Datastream ds1 = new Datastream("data_stream|image/jpeg|jpg|0||");
        Datastream ds2 = new Datastream("data_stream|image/jpeg|jpg|0||");

        assertTrue(ds1.equals(ds2));
    }

    @Test
    public void testDatastreamEqualsDatastreamByNameAndOwner() {
        Datastream ds1 = new Datastream("data_stream|image/jpeg|jpg|0||owner_id");
        Datastream ds2 = new Datastream("data_stream|||||owner_id");

        assertTrue(ds1.equals(ds2));
    }

    @Test
    public void testDatastreamNotEqualsDatastreamByOwner() {
        Datastream ds1 = new Datastream("data_stream|image/jpeg|jpg|0||owner_2");
        Datastream ds2 = new Datastream("data_stream|image/jpeg|jpg|0||owner_1");

        assertFalse(ds1.equals(ds2));
    }

    private static void assertEmpty(String string) {
        assertTrue(string == null || string.length() == 0);
    }
}
