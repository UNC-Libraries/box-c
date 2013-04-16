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
package edu.unc.lib.dl.data.ingest.solr.filter;

import java.io.File;
import java.io.FileInputStream;

import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.junit.Assert;
import org.junit.Test;

import edu.unc.lib.dl.data.ingest.solr.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;

public class SetRelationsFilterTest extends Assert {

	@Test(expected=IndexingException.class)
	public void noFOXML() throws Exception {
		DocumentIndexingPackage dip = new DocumentIndexingPackage("info:fedora/uuid:aggregate");
		SetRelationsFilter filter = new SetRelationsFilter();
		filter.filter(dip);
	}
	
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
		
		assertTrue(idb.getRelations().contains("defaultWebData|uuid:37c23b03-0ca4-4487-a1c5-92c28cadc71b/DATA_FILE"));
		assertTrue(idb.getRelations().contains("slug|A1100-A800_NS_final.jpg"));
		assertTrue(idb.getRelations().contains("sourceData|uuid:37c23b03-0ca4-4487-a1c5-92c28cadc71b/DATA_FILE"));
	}
	
	@Test
	public void embargoedRelation() throws Exception {
		DocumentIndexingPackage dip = new DocumentIndexingPackage("info:fedora/uuid:item");
		SAXBuilder builder = new SAXBuilder();
		Document foxml = builder.build(new FileInputStream(new File(
				"src/test/resources/foxml/embargoed.xml")));
		dip.setFoxml(foxml);
		
		IndexDocumentBean idb = dip.getDocument();
		SetRelationsFilter filter = new SetRelationsFilter();
		filter.filter(dip);
		
		assertTrue(idb.getRelations().contains("embargo-until|2074-02-03T00:00:00"));
		assertTrue(idb.getRelations().size() > 1);
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
