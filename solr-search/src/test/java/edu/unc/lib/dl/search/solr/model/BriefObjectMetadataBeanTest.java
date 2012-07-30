package edu.unc.lib.dl.search.solr.model;

import org.junit.Assert;
import org.junit.Test;

import edu.unc.lib.dl.search.solr.model.BriefObjectMetadataBean.Datastream;

public class BriefObjectMetadataBeanTest extends Assert {

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
