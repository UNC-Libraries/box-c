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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.unc.lib.dl.acl.fcrepo3.ObjectAccessControlsBeanImpl;
import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.ObjectAccessControlsBean;
import edu.unc.lib.dl.fedora.AccessClient;
import edu.unc.lib.dl.fedora.ManagementClient;
import edu.unc.lib.dl.fedora.NotFoundException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.fedora.types.Datastream;
import edu.unc.lib.dl.fedora.types.MIMETypedStream;
import edu.unc.lib.dl.util.ContentModelHelper.CDRProperty;
import edu.unc.lib.dl.util.ContentModelHelper.FedoraProperty;
import edu.unc.lib.dl.util.ContentModelHelper.Relationship;
import edu.unc.lib.dl.util.TripleStoreQueryService;

public class DocumentIndexingPackageDataLoaderTest extends Assert {

	@Mock
	private ManagementClient managementClient;
	@Mock
	private AccessClient accessClient;
	@Mock
	private AccessControlService aclLookup;
	@Mock
	private ObjectAccessControlsBean aclBean;
	@Mock
	private TripleStoreQueryService tsqs;
	@Mock
	private Datastream datastream;
	@Mock
	private MIMETypedStream datastreamData;
	@Mock
	private Document mockFoxml;
	
	private DocumentIndexingPackageFactory factory;
	private DocumentIndexingPackageDataLoader loader;
	
	@Mock
	private DocumentIndexingPackage parentDip;
	
	private DocumentIndexingPackage dip;

	@Before
	public void setup() {
		initMocks(this);
		
		factory = new DocumentIndexingPackageFactory();
		loader = new DocumentIndexingPackageDataLoader();
		loader.setAccessClient(accessClient);
		loader.setManagementClient(managementClient);
		loader.setAccessControlService(aclLookup);
		loader.setTsqs(tsqs);
		loader.setFactory(factory);
		factory.setDataLoader(loader);
		
		dip = factory.createDip("uuid:test");
	}
	
	@Test
	public void getFoxmlTest() throws Exception {
		when(managementClient.getObjectXML(any(PID.class))).thenReturn(mockFoxml);
		
		Document foxml = dip.getFoxml();
		dip.getFoxml();
		
		assertEquals(foxml, mockFoxml);
		verify(managementClient).getObjectXML(any(PID.class));
	}

	@Test
	public void getFoxmlRetryTest() throws Exception {
		when(managementClient.getObjectXML(any(PID.class))).thenReturn(null, mockFoxml);
		
		Document foxml = dip.getFoxml();
		
		assertEquals(foxml, mockFoxml);
		verify(managementClient, times(2)).getObjectXML(any(PID.class));
	}

	@Test
	public void getAclBeanTest() throws Exception {
		
		when(aclLookup.getObjectAccessControls(any(PID.class))).thenReturn(aclBean);
		
		ObjectAccessControlsBean resultAcl = dip.getAclBean();
		
		assertEquals(resultAcl, aclBean);
		verify(aclLookup).getObjectAccessControls(any(PID.class));
	}

	@Test
	public void getAclBeanFromParentTest() throws Exception {
		aclBean = new ObjectAccessControlsBeanImpl(new PID("uuid:parent"), Collections.<String>emptyList());
		
		when(parentDip.hasAclBean()).thenReturn(true);
		when(parentDip.getAclBean()).thenReturn(aclBean);
		dip.setParentDocument(parentDip);
		
		ObjectAccessControlsBean resultAcl = dip.getAclBean();
		
		assertNotNull("Resulting bean should be present", resultAcl);
		assertNotEquals("But should also not be the parent's bean", resultAcl, aclBean);
		verify(aclLookup, never()).getObjectAccessControls(any(PID.class));
	}

	@Test
	public void getTriplesFromFoxmlTest() throws Exception {
		dip.setFoxml(getDocument("src/test/resources/foxml/folderSmall.xml"));
		
		Map<String, List<String>> triples = dip.getTriples();
		
		assertTrue(triples.size() > 0);
		assertTrue("Check that fedora properties are extracted",
				triples.containsKey(FedoraProperty.state.toString()));
		assertEquals("Check that datastreams appear",
				5, triples.get(FedoraProperty.disseminates.toString()).size());
		
		List<String> containsRels = triples.get(Relationship.contains.toString());
		assertTrue(containsRels.size() > 5);
		assertTrue("Check that relations get populated",
				containsRels.contains("info:fedora/uuid:0f7343da-2c6f-48e2-9a2c-225e37cff2f6"));
		assertEquals("yes", dip.getFirstTriple(CDRProperty.allowIndexing.toString()));
		
		verify(tsqs, never()).fetchAllTriples(any(PID.class));
	}
	
	@Test
	public void getTriplesTest() throws Exception {
		Map<String, List<String>> tripleResponse = new HashMap<>();
		when(tsqs.fetchAllTriples(any(PID.class))).thenReturn(tripleResponse);
		
		Map<String, List<String>> triples = dip.getTriples();
		// Invoke multiple times
		dip.getTriples();
		
		assertNotNull(triples);
		// Check that only called once
		verify(tsqs).fetchAllTriples(any(PID.class));
	}
	
	@Test
	public void getMODSFromFoxmlTest() throws Exception {
		dip.setFoxml(getDocument("src/test/resources/foxml/aggregateSplitDepartments.xml"));
		
		Element mods = dip.getMods();
		
		assertNotNull(mods);
		assertEquals("mods", mods.getName());
		verify(managementClient, never()).getDatastream(any(PID.class), anyString());
	}
	
	@Test
	public void getMDContentsTest() throws Exception {
		when(managementClient.getDatastream(any(PID.class), anyString())).thenReturn(datastream);
		
		byte[] mdContentsBytes = IOUtils.toByteArray(
				new FileInputStream(new File("src/test/resources/datastream/mdContents.xml")));
		when(datastreamData.getStream()).thenReturn(mdContentsBytes);
		when(accessClient.getDatastreamDissemination(any(PID.class), anyString(), anyString())).thenReturn(datastreamData);
		
		Element mdContents = dip.getMdContents();
		
		verify(accessClient).getDatastreamDissemination(any(PID.class), anyString(), anyString());
		assertNotNull(mdContents);
		assertEquals("structMap", mdContents.getName());
	}
	
	@Test
	public void getMDContentsNotFoundTest() throws Exception {
		when(managementClient.getDatastream(any(PID.class), anyString())).thenReturn(null);
		when(accessClient.getDatastreamDissemination(any(PID.class), anyString(), anyString())).thenReturn(datastreamData);
		
		Element mdContents = dip.getMdContents();
		
		verify(accessClient, never()).getDatastreamDissemination(any(PID.class), anyString(), anyString());
		assertNull(mdContents);
	}
	
	@Test
	public void getMDContentsRetryTest() throws Exception {
		when(managementClient.getDatastream(any(PID.class), anyString())).thenReturn(datastream);
		
		byte[] mdContentsBytes = IOUtils.toByteArray(
				new FileInputStream(new File("src/test/resources/datastream/mdContents.xml")));
		when(datastreamData.getStream()).thenReturn(mdContentsBytes);
		when(accessClient.getDatastreamDissemination(any(PID.class), anyString(), anyString()))
				.thenThrow(new NotFoundException("")).thenReturn(datastreamData);
		
		Element mdContents = dip.getMdContents();
		
		verify(accessClient, times(2)).getDatastreamDissemination(any(PID.class), anyString(), anyString());
		assertNotNull(mdContents);
		assertEquals("structMap", mdContents.getName());
	}
	
	private Document getDocument(String filePath) throws Exception {
		SAXBuilder builder = new SAXBuilder();
		Document foxml = builder.build(new FileInputStream(new File(filePath)));
		return foxml;
	}
}
