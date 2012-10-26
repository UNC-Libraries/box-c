package edu.unc.lib.dl.data.ingest.solr.indexing;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.junit.Assert;
import org.junit.Test;

import edu.unc.lib.dl.util.ContentModelHelper;

public class DocumentIndexingPackageTest extends Assert {

	@Test
	public void getDatastreamMap() throws Exception { 
		DocumentIndexingPackage dip = new DocumentIndexingPackage("info:fedora/uuid:a4fa0296-1ce7-42a1-b74d-0222afd98194");
		
		SAXBuilder builder = new SAXBuilder();
		Document foxml = builder.build(new FileInputStream(new File(
				"src/test/resources/foxml/aggregateSplitDepartments.xml")));
		dip.setFoxml(foxml);
		
		Map<String,Element> datastreams = dip.getMostRecentDatastreamMap();
		assertEquals(6, datastreams.size());
		assertTrue(datastreams.containsKey(ContentModelHelper.Datastream.DC.name()));
		assertTrue(datastreams.containsKey(ContentModelHelper.Datastream.MD_DESCRIPTIVE.name()));
		assertTrue(datastreams.containsKey(ContentModelHelper.Datastream.MD_CONTENTS.name()));
		assertTrue(datastreams.containsKey(ContentModelHelper.Datastream.RELS_EXT.getName()));
		assertTrue(datastreams.containsKey(ContentModelHelper.Datastream.AUDIT.name()));
		assertTrue(datastreams.containsKey(ContentModelHelper.Datastream.MD_EVENTS.name()));
	}
}
