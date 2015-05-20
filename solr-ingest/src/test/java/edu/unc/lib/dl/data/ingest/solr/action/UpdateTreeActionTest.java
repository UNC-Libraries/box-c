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
package edu.unc.lib.dl.data.ingest.solr.action;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackageFactory;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPipeline;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.IndexingActionType;
import edu.unc.lib.dl.util.TripleStoreQueryService;

public class UpdateTreeActionTest extends BaseEmbeddedSolrTest {
	private static final Logger log = LoggerFactory.getLogger(UpdateTreeActionTest.class);

	@Mock
	protected DocumentIndexingPackageFactory dipFactory;
	@Mock
	protected TripleStoreQueryService tsqs;
	@Mock
	protected DocumentIndexingPipeline pipeline;

	protected Map<String, List<PID>> children;

	protected UpdateTreeAction action;

	@Before
	public void setup() throws Exception {
		initMocks(this);

		server.add(populate());
		server.commit();

		when(tsqs.queryResourceIndex(anyString())).thenReturn(Arrays.asList(Arrays.asList("3")));

		children = populateChildren();

		when(dipFactory.createDocumentIndexingPackage(any(PID.class))).thenAnswer(new Answer<DocumentIndexingPackage>() {
			@Override
			public DocumentIndexingPackage answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();
				PID pid = (PID) args[0];
				if (pid.getPid().equals("uuid:doesnotexist"))
					throw new IndexingException("");
				DocumentIndexingPackage dip = new DocumentIndexingPackage(pid);
				dip.setChildren(children.get(pid.getPid()));
				dip.getDocument().setTitle("Text");
				return dip;
			}
		});

		action = getAction();

		action.setTsqs(tsqs);
		action.setPipeline(pipeline);
		action.setSolrUpdateDriver(driver);
		action.setDipFactory(dipFactory);
		action.setAddDocumentMode(false);
		action.setCollectionsPid(new PID("uuid:1"));
		action.init();
	}

	protected UpdateTreeAction getAction() {
		return new UpdateTreeAction();
	}

	@Test
	public void testVerifyUpdated() throws Exception {

		SolrDocumentList docListBefore = getDocumentList();

		action.performAction(new SolrUpdateRequest(new PID("uuid:2"), IndexingActionType.RECURSIVE_ADD, "1", null));
		server.commit();

		SolrDocumentList docListAfter = getDocumentList();

		log.debug("Docs: " + docListBefore);
		log.debug("Docs: " + docListAfter);

		// Verify that only the object itself and its children, excluding orphans, were updated
		for (SolrDocument docAfter : docListAfter) {
			String id = (String) docAfter.getFieldValue("id");
			for (SolrDocument docBefore : docListBefore) {
				if (id.equals(docBefore.getFieldValue("id"))) {
					if ("uuid:1".equals(id) || "uuid:3".equals(id) || "uuid:5".equals(id))
						assertTrue(docAfter.getFieldValue("_version_").equals(docBefore.getFieldValue("_version_")));
					else assertFalse(docAfter.getFieldValue("_version_").equals(docBefore.getFieldValue("_version_")));
				}
			}
		}
	}

	@Test
	public void danglingContains() throws Exception {
		children.put("uuid:4", Arrays.asList(new PID("uuid:doesnotexist")));

		SolrDocumentList docListBefore = getDocumentList();

		action.performAction(new SolrUpdateRequest(new PID("uuid:2"), IndexingActionType.RECURSIVE_ADD, "1", null));
		server.commit();

		SolrDocumentList docListAfter = getDocumentList();

		// Verify that all appropriate objects were updated, and that the dangling contains didn't create a record
		for (SolrDocument docAfter : docListAfter) {
			String id = (String) docAfter.getFieldValue("id");
			if ("uuid:doesnotexist".equals(id))
				fail("Record for dangling exists");
			for (SolrDocument docBefore : docListBefore) {
				if (id.equals(docBefore.getFieldValue("id"))) {
					if ("uuid:1".equals(id) || "uuid:3".equals(id) || "uuid:5".equals(id))
						assertTrue(docAfter.getFieldValue("_version_").equals(docBefore.getFieldValue("_version_")));
					else
						assertFalse(docAfter.getFieldValue("_version_").equals(docBefore.getFieldValue("_version_")));
				}
			}
		}
	}

	@Test
	public void testNoDescendents() throws Exception {

		SolrDocumentList docListBefore = getDocumentList();

		when(tsqs.queryResourceIndex(anyString())).thenReturn(new ArrayList<List<String>>());

		action.performAction(new SolrUpdateRequest("uuid:6", IndexingActionType.RECURSIVE_ADD));
		server.commit();

		SolrDocumentList docListAfter = getDocumentList();

		// Verify that only the object itself and its children, excluding orphans, were updated
		for (SolrDocument docAfter : docListAfter) {
			String id = (String) docAfter.getFieldValue("id");
			for (SolrDocument docBefore : docListBefore) {
				if (id.equals(docBefore.getFieldValue("id"))) {
					if ("uuid:6".equals(id))
						assertFalse(docAfter.getFieldValue("_version_").equals(docBefore.getFieldValue("_version_")));
					else
						assertTrue(docAfter.getFieldValue("_version_").equals(docBefore.getFieldValue("_version_")));
				}
			}
		}
	}

	protected Map<String, List<PID>> populateChildren() {
		Map<String, List<PID>> children = new HashMap<String, List<PID>>();
		children.put("uuid:1", Arrays.asList(new PID("uuid:2"), new PID("uuid:3")));
		children.put("uuid:2", Arrays.asList(new PID("uuid:4"), new PID("uuid:6")));
		return children;
	}

	protected List<SolrInputDocument> populate() {
		List<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();

		SolrInputDocument newDoc = new SolrInputDocument();
		newDoc.addField("title", "Collections");
		newDoc.addField("id", "uuid:1");
		newDoc.addField("rollup", "uuid:1");
		newDoc.addField("roleGroup", "");
		newDoc.addField("readGroup", "");
		newDoc.addField("adminGroup", "");
		newDoc.addField("ancestorIds", "");
		newDoc.addField("resourceType", "Folder");
		docs.add(newDoc);

		newDoc = new SolrInputDocument();
		newDoc.addField("title", "A collection");
		newDoc.addField("id", "uuid:2");
		newDoc.addField("rollup", "uuid:2");
		newDoc.addField("roleGroup", "public admin");
		newDoc.addField("readGroup", "public");
		newDoc.addField("adminGroup", "admin");
		newDoc.addField("ancestorIds", "/uuid:1/uuid:2");
		newDoc.addField("ancestorPath", Arrays.asList("1,uuid:1"));
		newDoc.addField("resourceType", "Collection");
		docs.add(newDoc);

		newDoc = new SolrInputDocument();
		newDoc.addField("title", "Subfolder 1");
		newDoc.addField("id", "uuid:4");
		newDoc.addField("rollup", "uuid:4");
		newDoc.addField("roleGroup", "public admin");
		newDoc.addField("readGroup", "public");
		newDoc.addField("adminGroup", "admin");
		newDoc.addField("ancestorIds", "/uuid:1/uuid:2/uuid:4");
		newDoc.addField("ancestorPath", Arrays.asList("1,uuid:1", "2,uuid:2"));
		newDoc.addField("resourceType", "Folder");
		docs.add(newDoc);

		newDoc = new SolrInputDocument();
		newDoc.addField("title", "Orphaned");
		newDoc.addField("id", "uuid:5");
		newDoc.addField("rollup", "uuid:5");
		newDoc.addField("roleGroup", "public admin");
		newDoc.addField("readGroup", "public");
		newDoc.addField("adminGroup", "admin");
		newDoc.addField("ancestorIds", "/uuid:1/uuid:2");
		newDoc.addField("ancestorPath", Arrays.asList("1,uuid:1", "2,uuid:2"));
		newDoc.addField("resourceType", "File");
		docs.add(newDoc);

		newDoc = new SolrInputDocument();
		newDoc.addField("title", "File");
		newDoc.addField("id", "uuid:6");
		newDoc.addField("rollup", "uuid:6");
		newDoc.addField("roleGroup", "public admin");
		newDoc.addField("readGroup", "public");
		newDoc.addField("adminGroup", "admin");
		newDoc.addField("ancestorIds", "/uuid:1/uuid:2");
		newDoc.addField("ancestorPath", Arrays.asList("1,uuid:1", "2,uuid:2"));
		newDoc.addField("resourceType", "File");
		docs.add(newDoc);

		newDoc = new SolrInputDocument();
		newDoc.addField("title", "Second collection");
		newDoc.addField("id", "uuid:3");
		newDoc.addField("rollup", "uuid:3");
		newDoc.addField("roleGroup", "public admin");
		newDoc.addField("readGroup", "public");
		newDoc.addField("adminGroup", "admin");
		newDoc.addField("ancestorIds", "/uuid:1/uuid:3");
		newDoc.addField("ancestorPath", Arrays.asList("1,uuid:1"));
		newDoc.addField("resourceType", "Collection");
		docs.add(newDoc);

		return docs;
	}
}
