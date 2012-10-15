package edu.unc.lib.dl.data.ingest.solr.filter;

import java.io.File;
import java.io.FileInputStream;

import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.junit.Assert;
import org.junit.Test;

import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;

public class SetRelationsFilterTest extends Assert {

	@Test
	public void aggregateRelations() throws Exception {
		DocumentIndexingPackage dip = new DocumentIndexingPackage("info:fedora/uuid:aggregate");
		SAXBuilder builder = new SAXBuilder();
		Document foxml = builder.build(new FileInputStream(new File(
				"src/test/resources/foxml/aggregateSplitDepartments.xml")));
		dip.setFoxml(foxml);
		
		IndexDocumentBean idb = dip.getDocument();
		SetRelationsFilter filter = new SetRelationsFilter();
		filter.filter(dip);
		
		assertTrue(idb.getRelations().contains("defaultWebObject|uuid:a4fa0296-1ce7-42a1-b74d-0222afd98194"));
		assertTrue(idb.getRelations().contains("slug|A_Comparison_of_Machine_Learning_Algorithms_for_C"));
	}
	
	@Test
	public void itemWithOriginalRelations() throws Exception {
		DocumentIndexingPackage dip = new DocumentIndexingPackage("info:fedora/uuid:item");
		SAXBuilder builder = new SAXBuilder();
		Document foxml = builder.build(new FileInputStream(new File(
				"src/test/resources/foxml/imageNoMODS.xml")));
		dip.setFoxml(foxml);
		
		IndexDocumentBean idb = dip.getDocument();
		SetRelationsFilter filter = new SetRelationsFilter();
		filter.filter(dip);
		
		assertTrue(idb.getRelations().contains("defaultWebObject|uuid:a4fa0296-1ce7-42a1-b74d-0222afd98194"));
		assertTrue(idb.getRelations().contains("slug|A1100-A800_NS_final.jpg"));
		assertTrue(idb.getRelations().contains("sourceData|uuid:37c23b03-0ca4-4487-a1c5-92c28cadc71b/DATA_FILE"));
	}
	
	@Test
	public void orderedContainerRelations() throws Exception {
		DocumentIndexingPackage dip = new DocumentIndexingPackage("info:fedora/uuid:aggregate");
		SAXBuilder builder = new SAXBuilder();
		Document foxml = builder.build(new FileInputStream(new File(
				"src/test/resources/foxml/folderSmall.xml")));
		dip.setFoxml(foxml);
		
		IndexDocumentBean idb = dip.getDocument();
		SetRelationsFilter filter = new SetRelationsFilter();
		filter.filter(dip);
		
		assertTrue(idb.getRelations().contains("sortOrder|ordered"));
		assertTrue(idb.getRelations().contains("slug|Field_notes"));
	}
}
