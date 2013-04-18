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
import java.util.Arrays;

import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import static org.mockito.Mockito.*;

import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackageFactory;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.TripleStoreQueryService;

public class SetDisplayOrderTest extends Assert {

	@Test
	public void fromParents() throws Exception {
		DocumentIndexingPackage dip = new DocumentIndexingPackage("info:fedora/uuid:a4fa0296-1ce7-42a1-b74d-0222afd98194");

		DocumentIndexingPackage parentDIP = new DocumentIndexingPackage("info:fedora/uuid:parent");
		SAXBuilder builder = new SAXBuilder();
		Document foxml = builder.build(new FileInputStream(new File(
				"src/test/resources/foxml/aggregateSplitDepartments.xml")));
		parentDIP.setFoxml(foxml);
		dip.setParentDocument(parentDIP);

		SetDisplayOrderFilter filter = new SetDisplayOrderFilter();
		filter.filter(dip);

		assertEquals(2, dip.getDocument().getDisplayOrder().longValue());
		dip.setPid(new PID("info:fedora/uuid:9a7f19d7-5f1d-44f9-9c3d-3ff4f7dac42d"));
		filter.filter(dip);
		assertEquals(1, dip.getDocument().getDisplayOrder().longValue());
	}

	@Test
	public void fromRetrievedParent() throws Exception {
		DocumentIndexingPackage dip = new DocumentIndexingPackage("info:fedora/uuid:a4fa0296-1ce7-42a1-b74d-0222afd98194");

		DocumentIndexingPackage parentDIP = new DocumentIndexingPackage("info:fedora/uuid:parent");
		SAXBuilder builder = new SAXBuilder();
		Document foxml = builder.build(new FileInputStream(new File(
				"src/test/resources/foxml/aggregateSplitDepartments.xml")));
		parentDIP.setFoxml(foxml);

		TripleStoreQueryService tsqs = mock(TripleStoreQueryService.class);
		when(tsqs.fetchByPredicateAndLiteral(anyString(), any(PID.class))).thenReturn(Arrays.asList(new PID("info:fedora/uuid:parent")));

		DocumentIndexingPackageFactory dipFactory = mock(DocumentIndexingPackageFactory.class);
		when(dipFactory.createDocumentIndexingPackage(any(PID.class))).thenReturn(parentDIP);

		SetDisplayOrderFilter filter = new SetDisplayOrderFilter();
		filter.setDocumentIndexingPackageFactory(dipFactory);
		filter.setTripleStoreQueryService(tsqs);
		filter.filter(dip);

		assertEquals(2, dip.getDocument().getDisplayOrder().longValue());
		dip.setPid(new PID("info:fedora/uuid:9a7f19d7-5f1d-44f9-9c3d-3ff4f7dac42d"));
	}

	private static class PIDMatcher extends ArgumentMatcher<PID> {
		private PID pid;

		public PIDMatcher(PID pid) {
			this.pid = pid;
		}

		public boolean matches(Object pidObj) {
			PID rightPID = (PID) pidObj;
			return rightPID.getPid().equals(pid.getPid());
		}
	}

	@Test
	public void fromAncestorParent() throws Exception {
		DocumentIndexingPackage dip = new DocumentIndexingPackage("info:fedora/uuid:a4fa0296-1ce7-42a1-b74d-0222afd98194");

		DocumentIndexingPackage parentDIP = new DocumentIndexingPackage("info:fedora/uuid:parent");
		SAXBuilder builder = new SAXBuilder();
		Document foxml = builder.build(new FileInputStream(new File(
				"src/test/resources/foxml/aggregateSplitDepartments.xml")));
		parentDIP.setFoxml(foxml);

		dip.getDocument().setAncestorPath(Arrays.asList("1,uuid:Collections,Collections", "2,uuid:parent,Parent"));

		PIDMatcher matcher = new PIDMatcher(new PID("uuid:parent"));
		DocumentIndexingPackageFactory dipFactory = mock(DocumentIndexingPackageFactory.class);
		when(dipFactory.createDocumentIndexingPackage((PID) argThat(matcher))).thenReturn(parentDIP);

		SetDisplayOrderFilter filter = new SetDisplayOrderFilter();
		filter.setDocumentIndexingPackageFactory(dipFactory);
		filter.filter(dip);

		assertEquals(2, dip.getDocument().getDisplayOrder().longValue());
		dip.setPid(new PID("info:fedora/uuid:9a7f19d7-5f1d-44f9-9c3d-3ff4f7dac42d"));
	}

	@Test(expected = IndexingException.class)
	public void fromAncestorParentNoFound() throws Exception {
		DocumentIndexingPackage dip = new DocumentIndexingPackage("info:fedora/uuid:a4fa0296-1ce7-42a1-b74d-0222afd98194");

		DocumentIndexingPackage parentDIP = new DocumentIndexingPackage("info:fedora/uuid:parent");
		SAXBuilder builder = new SAXBuilder();
		Document foxml = builder.build(new FileInputStream(new File(
				"src/test/resources/foxml/aggregateSplitDepartments.xml")));
		parentDIP.setFoxml(foxml);

		dip.getDocument().setAncestorPath(Arrays.asList("1,uuid:Collections,Collections", "2,uuid:fail,Parent"));

		PIDMatcher matcher = new PIDMatcher(new PID("uuid:parent"));
		DocumentIndexingPackageFactory dipFactory = mock(DocumentIndexingPackageFactory.class);
		when(dipFactory.createDocumentIndexingPackage((PID) argThat(matcher))).thenReturn(parentDIP);

		SetDisplayOrderFilter filter = new SetDisplayOrderFilter();
		filter.setDocumentIndexingPackageFactory(dipFactory);
		filter.filter(dip);
	}

	@Test
	public void fromParentsNoMDContents() throws Exception {
		DocumentIndexingPackage dip = new DocumentIndexingPackage("info:fedora/uuid:a4fa0296-1ce7-42a1-b74d-0222afd98194");

		DocumentIndexingPackage parentDIP = new DocumentIndexingPackage("info:fedora/uuid:parent");
		SAXBuilder builder = new SAXBuilder();
		Document foxml = builder.build(new FileInputStream(new File("src/test/resources/foxml/folderNoMDContents.xml")));
		parentDIP.setFoxml(foxml);
		dip.setParentDocument(parentDIP);

		SetDisplayOrderFilter filter = new SetDisplayOrderFilter();
		filter.filter(dip);

		assertNull(dip.getDocument().getDisplayOrder());
	}
}
