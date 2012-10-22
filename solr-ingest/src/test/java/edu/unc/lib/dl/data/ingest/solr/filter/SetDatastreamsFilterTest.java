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
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackageFactory;
import edu.unc.lib.dl.fedora.ManagementClient;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;

import static org.mockito.Mockito.*;

public class SetDatastreamsFilterTest extends Assert {
	@Test
	public void extractDatastreamsImage() throws Exception {
		DocumentIndexingPackage dip = new DocumentIndexingPackage("info:fedora/uuid:item");
		SAXBuilder builder = new SAXBuilder();
		Document foxml = builder.build(new FileInputStream(new File("src/test/resources/foxml/imageNoMODS.xml")));
		dip.setFoxml(foxml);

		SetDatastreamContentFilter filter = new SetDatastreamContentFilter();
		filter.filter(dip);

		IndexDocumentBean idb = dip.getDocument();

		assertEquals(9, idb.getDatastream().size());
		assertTrue(idb.getDatastream().contains("DATA_FILE|3645539|image/jpeg|jpg|Original|"));
		assertTrue(idb.getDatastream().contains("AUDIT|0|text/xml|xml|Administrative|"));
		assertTrue(idb.getDatastream().contains("DC|417|text/xml|xml|Metadata|"));
		assertTrue(idb.getDatastream().contains("RELS-EXT|2128|text/xml|xml|Administrative|"));
		assertTrue(idb.getDatastream().contains("IMAGE_JP2000|4893818|image/jp2|jp2|Derivative|"));
		assertEquals(8575798, idb.getFilesizeTotal().longValue());

		assertEquals(2, idb.getContentType().size());
		assertTrue(idb.getContentType().contains("|image,Image"));
		assertTrue(idb.getContentType().contains("/image|jpg,jpg"));

		assertEquals("DATA_FILE", dip.getDefaultWebData());
	}

	@Test
	public void extractDefaultWebDataNoMimetype() throws Exception {
		DocumentIndexingPackage dip = new DocumentIndexingPackage("info:fedora/uuid:item");
		SAXBuilder builder = new SAXBuilder();
		Document foxml = builder.build(new FileInputStream(new File("src/test/resources/foxml/fileOctetStream.xml")));
		dip.setFoxml(foxml);

		SetDatastreamContentFilter filter = new SetDatastreamContentFilter();
		filter.filter(dip);

		IndexDocumentBean idb = dip.getDocument();

		assertEquals(2, idb.getContentType().size());
		assertTrue(idb.getContentType().contains("|image,Image"));
		assertTrue(idb.getContentType().contains("/image|png,png"));

		assertEquals("DATA_FILE", dip.getDefaultWebData());
	}

	@Test
	public void extractDatastreamsDefaultWebObject() throws Exception {
		DocumentIndexingPackage dip = new DocumentIndexingPackage("info:fedora/uuid:aggregate");
		SAXBuilder builder = new SAXBuilder();
		Document foxml = builder.build(new FileInputStream(new File(
				"src/test/resources/foxml/aggregateSplitDepartments.xml")));
		dip.setFoxml(foxml);

		Document dwoFOXML = builder.build(new FileInputStream(new File("src/test/resources/foxml/imageNoMODS.xml")));
		DocumentIndexingPackageFactory dipFactory = new DocumentIndexingPackageFactory();
		ManagementClient managementClient = mock(ManagementClient.class);
		when(managementClient.getObjectXML(any(PID.class))).thenReturn(dwoFOXML);
		dipFactory.setManagementClient(managementClient);

		SetDatastreamContentFilter filter = new SetDatastreamContentFilter();
		filter.setDocumentIndexingPackageFactory(dipFactory);
		filter.filter(dip);

		IndexDocumentBean idb = dip.getDocument();

		assertEquals(15, idb.getDatastream().size());

		assertTrue(idb.getDatastream().contains("MD_DESCRIPTIVE|6533|text/xml|xml|Metadata|"));
		assertTrue(idb.getDatastream().contains("RELS-EXT|1697|text/xml|xml|Administrative|"));

		assertTrue(idb.getDatastream().contains(
				"DATA_FILE|3645539|image/jpeg|jpg|Original|uuid:a4fa0296-1ce7-42a1-b74d-0222afd98194"));
		assertTrue(idb.getDatastream().contains("DC|417|text/xml|xml|Metadata|uuid:a4fa0296-1ce7-42a1-b74d-0222afd98194"));
		assertTrue(idb.getDatastream().contains(
				"RELS-EXT|2128|text/xml|xml|Administrative|uuid:a4fa0296-1ce7-42a1-b74d-0222afd98194"));
		assertTrue(idb.getDatastream().contains(
				"IMAGE_JP2000|4893818|image/jp2|jp2|Derivative|uuid:a4fa0296-1ce7-42a1-b74d-0222afd98194"));
		// Does not include the filesize of the second copy of the MD_DESCRIPTIVE DS
		assertEquals(17896, idb.getFilesizeTotal().longValue());

		assertEquals(2, idb.getContentType().size());
		assertTrue(idb.getContentType().contains("|image,Image"));
		assertTrue(idb.getContentType().contains("/image|jpg,jpg"));
		assertEquals("uuid:a4fa0296-1ce7-42a1-b74d-0222afd98194/DATA_FILE", dip.getDefaultWebData());
	}

	@Test
	public void extractDatastreamsNoDefaultWebData() throws Exception {
		DocumentIndexingPackage dip = new DocumentIndexingPackage("info:fedora/uuid:folder");
		SAXBuilder builder = new SAXBuilder();
		Document foxml = builder.build(new FileInputStream(new File("src/test/resources/foxml/folderSmall.xml")));
		dip.setFoxml(foxml);

		SetDatastreamContentFilter filter = new SetDatastreamContentFilter();
		filter.filter(dip);

		IndexDocumentBean idb = dip.getDocument();

		assertEquals(5, idb.getDatastream().size());

		assertTrue(idb.getDatastream().contains("DC|405|text/xml|xml|Metadata|"));
		assertTrue(idb.getDatastream().contains("RELS-EXT|1770|text/xml|xml|Administrative|"));

		assertNull(idb.getContentType());
		assertNull(dip.getDefaultWebData());
	}

	@Test(expected = IndexingException.class)
	public void extractDatastreamsNoFOXML() throws Exception {
		DocumentIndexingPackage dip = new DocumentIndexingPackage("info:fedora/uuid:aggregate");

		SetDatastreamContentFilter filter = new SetDatastreamContentFilter();
		filter.filter(dip);
	}

	@Test(expected = IndexingException.class)
	public void extractDatastreamsDefaultWebObjectNotFound() throws Exception {
		DocumentIndexingPackage dip = new DocumentIndexingPackage("info:fedora/uuid:aggregate");
		SAXBuilder builder = new SAXBuilder();
		Document foxml = builder.build(new FileInputStream(new File(
				"src/test/resources/foxml/aggregateSplitDepartments.xml")));
		dip.setFoxml(foxml);

		DocumentIndexingPackageFactory dipFactory = new DocumentIndexingPackageFactory();
		ManagementClient managementClient = mock(ManagementClient.class);
		dipFactory.setManagementClient(managementClient);

		SetDatastreamContentFilter filter = new SetDatastreamContentFilter();
		filter.setDocumentIndexingPackageFactory(dipFactory);
		filter.filter(dip);
	}
}
