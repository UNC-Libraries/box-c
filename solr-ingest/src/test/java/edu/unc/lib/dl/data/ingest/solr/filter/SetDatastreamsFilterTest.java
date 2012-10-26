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
		assertTrue(idb.getDatastream().contains("DATA_FILE|image/jpeg|jpg|3645539|1adeece64580be3b9a185e8f12e8653b|"));
		assertTrue(idb.getDatastream().contains("AUDIT|text/xml|xml|0||"));
		assertTrue(idb.getDatastream().contains("DC|text/xml|xml|417|c177272d23b874fdcfd4fe8d6626c853|"));
		assertTrue(idb.getDatastream().contains("RELS-EXT|text/xml|xml|2128|4ac9a05bbcc2828354d6811723e641ab|"));
		assertTrue(idb.getDatastream().contains("IMAGE_JP2000|image/jp2|jp2|4893818|2eef5ccd78d8e1f2854d8c5cb533f427|"));
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

		assertTrue(idb.getDatastream().contains("MD_DESCRIPTIVE|text/xml|xml|6533|84e583a9280840f9c18502bc115481cb|"));
		assertTrue(idb.getDatastream().contains("RELS-EXT|text/xml|xml|1697|da8227f1a4d74d5a1b758bb47bf929aa|"));
		
		assertTrue(idb.getDatastream().contains(
				"DATA_FILE|image/jpeg|jpg|3645539|1adeece64580be3b9a185e8f12e8653b|uuid:a4fa0296-1ce7-42a1-b74d-0222afd98194"));
		assertTrue(idb.getDatastream().contains("DC|text/xml|xml|417|c177272d23b874fdcfd4fe8d6626c853|uuid:a4fa0296-1ce7-42a1-b74d-0222afd98194"));
		assertTrue(idb.getDatastream().contains(
				"RELS-EXT|text/xml|xml|2128|4ac9a05bbcc2828354d6811723e641ab|uuid:a4fa0296-1ce7-42a1-b74d-0222afd98194"));
		assertTrue(idb.getDatastream().contains(
				"IMAGE_JP2000|image/jp2|jp2|4893818|2eef5ccd78d8e1f2854d8c5cb533f427|uuid:a4fa0296-1ce7-42a1-b74d-0222afd98194"));
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

		assertTrue(idb.getDatastream().contains("DC|text/xml|xml|405|e9f9b5ac744374e659fce6c65e2dd4c0|"));
		assertTrue(idb.getDatastream().contains("RELS-EXT|text/xml|xml|1770|9c975fd3e2bbc34a4912adb5aea99e02|"));

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
