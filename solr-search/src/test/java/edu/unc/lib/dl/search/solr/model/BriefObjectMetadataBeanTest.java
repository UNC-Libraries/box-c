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

import java.util.Arrays;

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
	public void datastreamNoChecksum() {
		Datastream ds = new Datastream("AUDIT|text/xml|xml|30459||");
		
		assertEquals("AUDIT", ds.getName());
		assertEquals("text/xml", ds.getMimetype());
		assertEquals("xml", ds.getExtension());
		assertEquals(30459, ds.getFilesize().longValue());
		assertNull(ds.getChecksum());
		assertNull(ds.getOwner());
	}
	
	@Test
	public void datastreamNoChecksumFromSurrogate() {
		Datastream ds = new Datastream("AUDIT|text/xml|xml|30459||uuid:73247248-e351-49dc-9b27-fe44df3884e7");
		
		assertEquals("AUDIT", ds.getName());
		assertEquals("text/xml", ds.getMimetype());
		assertEquals("xml", ds.getExtension());
		assertEquals(30459, ds.getFilesize().longValue());
		assertNull(ds.getChecksum());
		assertEquals("uuid:73247248-e351-49dc-9b27-fe44df3884e7", ds.getOwner().getPid());
	}
	
	@Test
	public void datastreamEquality(){
		Datastream ds = new Datastream("DATA_FILE");
		Datastream ds2 = new Datastream("DATA_FILE|image/jpeg|jpg|0||");
		assertTrue(ds.equals(ds));
		assertTrue(ds.equals(ds2));
		assertTrue(ds2.equals(ds));
		
		assertTrue(ds.equals("DATA_FILE"));
		
		assertFalse(ds.equals(null));
		
		ds = new Datastream("DATA_FILE|image/jpeg|jpg|0||uuid:1234");
		assertTrue(ds.equals(ds));
		assertTrue(ds.equals(ds2));
		assertTrue(ds2.equals(ds));
		
		ds2 = new Datastream("DATA_FILE|image/jpeg|jpg|0||uuid:1234");
		assertTrue(ds.equals(ds2));
		
		ds2 = new Datastream("DATA_FILE|image/jpeg|jpg|0||uuid:2345");
		assertFalse(ds.equals(ds2));
		
		ds2 = new Datastream("RELS-EXT|text/xml|xml|23||uuid:1234");
		assertFalse(ds.equals(ds2));
	}
	
	@Test
	public void setRoleGroupsEmpty() {
		BriefObjectMetadataBean mdb = new BriefObjectMetadataBean();
		mdb.setRoleGroup(Arrays.asList(""));
		assertEquals(0, mdb.getGroupRoleMap().size());
		assertEquals(1, mdb.getRoleGroup().size());
	}
	
	@Test
	public void setRoleGroups() {
		BriefObjectMetadataBean mdb = new BriefObjectMetadataBean();
		mdb.setRoleGroup(Arrays.asList("curator|admin", "patron|public"));
		assertEquals(2, mdb.getGroupRoleMap().size());
		assertEquals(2, mdb.getRoleGroup().size());
	}
	
	@Test
	public void labelFromRelation() {
		BriefObjectMetadataBean mdb = new BriefObjectMetadataBean();

		mdb.setRelations(Arrays.asList("label|hello.jpg"));
		assertEquals("hello.jpg", mdb.getLabel());

		mdb.setRelations(Arrays.asList("blah|xyz"));
		assertEquals(null, mdb.getLabel());
	}
}
