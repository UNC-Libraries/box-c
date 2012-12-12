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
