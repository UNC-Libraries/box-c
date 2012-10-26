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

import org.junit.Assert;
import org.junit.Test;

public class BriefObjectMetadataBeanTest extends Assert {

	@Test
	public void datastreamParsing() {
		Datastream ds = new Datastream("DATA_FILE|image/jpeg|jpg|30459|dc93eff50ca7dbd688971716e55e0084|");
		
		assertEquals("DATA_FILE", ds.getName());
		assertEquals("image/jpeg", ds.getMimetype());
		assertEquals("jpg", ds.getExtension());
		assertEquals(30459, ds.getFilesize().longValue());
		assertEquals("dc93eff50ca7dbd688971716e55e0084", ds.getChecksum());
		assertNull(ds.getOwner());
	}
	
	@Test
	public void datastreamSurrogateParsing() {
		Datastream ds = new Datastream("DATA_FILE|image/jpeg|jpg|30459|dc93eff50ca7dbd688971716e55e0084|uuid:73247248-e351-49dc-9b27-fe44df3884e7");
		
		assertEquals("DATA_FILE", ds.getName());
		assertEquals("image/jpeg", ds.getMimetype());
		assertEquals("jpg", ds.getExtension());
		assertEquals(30459, ds.getFilesize().longValue());
		assertEquals("dc93eff50ca7dbd688971716e55e0084", ds.getChecksum());
		assertEquals("uuid:73247248-e351-49dc-9b27-fe44df3884e7", ds.getOwner().getPid());
	}
	
	@Test
	public void datastreamEquality(){
		Datastream ds = new Datastream("DATA_FILE");
		Datastream ds2 = new Datastream("DATA_FILE");
		assertTrue(ds.equals(ds));
		assertTrue(ds.equals(ds2));
		assertTrue(ds2.equals(ds));
		
		assertTrue(ds.equals("DATA_FILE"));
		
		assertFalse(ds.equals(null));
		
		ds = new Datastream("uuid:1234/DATA_FILE");
		assertTrue(ds.equals(ds));
		assertTrue(ds.equals(ds2));
		assertTrue(ds2.equals(ds));
		
		ds2 = new Datastream("uuid:1234/DATA_FILE");
		assertTrue(ds.equals(ds2));
		
		ds2 = new Datastream("uuid:2345/DATA_FILE");
		assertFalse(ds.equals(ds2));
		
		ds2 = new Datastream("uuid:1234/RELS-EXT");
		assertFalse(ds.equals(ds2));
		
		ds = new Datastream("/DATA_FILE");
		ds2 = new Datastream("DATA_FILE");
		assertTrue(ds.equals(ds2));
		
		ds2 = new Datastream("DATA_FILE/");
		assertTrue(ds.equals(ds2));
		
		ds2 = new Datastream("/DATA_FILE");
		assertTrue(ds.equals(ds2));
	}
}
