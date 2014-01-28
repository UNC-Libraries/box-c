package edu.unc.lib.dl.data.ingest.solr.action;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.core.CoreContainer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mockito.Mockito.*;

import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackageFactory;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPipeline;
import edu.unc.lib.dl.data.ingest.solr.indexing.SolrUpdateDriver;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.IndexingActionType;
import edu.unc.lib.dl.util.TripleStoreQueryService;

public class UpdateTreeActionTest extends Assert {
	private static final Logger log = LoggerFactory.getLogger(UpdateTreeActionTest.class);
	EmbeddedSolrServer server;
	CoreContainer container;
	
	@Before
	public void setUp() throws Exception {
		// If indexing a brand-new index, you might want to delete the data directory first
		//FileUtilities.deleteDirectory("testdata/solr/access/data");

		File home = new File( "src/test/resources/config" );
		File configFile = new File( home, "solr.xml" );
		
		System.setProperty("solr.data.dir", "src/test/resources/config/data/");
		container = new CoreContainer("src/test/resources/config", configFile);

		server = new EmbeddedSolrServer(container, "access-master");
	}

	@After
	public void tearDown() throws Exception {
		server.shutdown();
		log.debug("Cleaning up data directory");
		File dataDir = new File("src/test/resources/config/data");
		FileUtils.deleteDirectory(dataDir);
	}
	
	protected UpdateTreeAction getAction() {
		return new UpdateTreeAction();
	}

	@Test
	public void testVerifyUpdated() throws SolrServerException, IOException {
		UpdateTreeAction action = initializeOrphanSet(populateChildren());
		
		ModifiableSolrParams params = new ModifiableSolrParams();
		params.set("q", "*:*");
		params.set("fl", "id,_version_");
		QueryResponse qResp = server.query(params);
		SolrDocumentList docListBefore = qResp.getResults();
		
		action.performAction(new SolrUpdateRequest(new PID("uuid:2"), IndexingActionType.RECURSIVE_ADD, "1", null));
		server.commit();
		
		qResp = server.query(params);
		SolrDocumentList docListAfter = qResp.getResults();
		
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
	public void danglingContains() throws SolrServerException, IOException {
		Map<String, List<PID>> children = populateChildren();
		children.put("uuid:4", Arrays.asList(new PID("uuid:doesnotexist")));
		UpdateTreeAction action = initializeOrphanSet(children);
		
		ModifiableSolrParams params = new ModifiableSolrParams();
		params.set("q", "*:*");
		params.set("fl", "id,_version_");
		QueryResponse qResp = server.query(params);
		SolrDocumentList docListBefore = qResp.getResults();
		
		action.performAction(new SolrUpdateRequest(new PID("uuid:2"), IndexingActionType.RECURSIVE_ADD, "1", null));
		server.commit();
		
		qResp = server.query(params);
		SolrDocumentList docListAfter = qResp.getResults();
		
		// Verify that all appropriate objects were updated, and that the dangling contains didn't create a record
		for (SolrDocument docAfter : docListAfter) {
			String id = (String) docAfter.getFieldValue("id");
			if ("uuid:doesnotexist".equals(id))
				fail("Record for dangling exists");
			for (SolrDocument docBefore : docListBefore) {
				if (id.equals(docBefore.getFieldValue("id"))) {
					if ("uuid:1".equals(id) || "uuid:3".equals(id) || "uuid:5".equals(id))
						assertTrue(docAfter.getFieldValue("_version_").equals(docBefore.getFieldValue("_version_")));
					else assertFalse(docAfter.getFieldValue("_version_").equals(docBefore.getFieldValue("_version_")));
				}
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	protected UpdateTreeAction initializeOrphanSet(final Map<String, List<PID>> children) throws SolrServerException, IOException {
		server.add(populate());
		server.commit();
		
		TripleStoreQueryService tsqs = mock(TripleStoreQueryService.class);
		when(tsqs.queryResourceIndex(anyString())).thenReturn(Arrays.asList(Arrays.asList("3")));
		
		DocumentIndexingPipeline pipeline = mock(DocumentIndexingPipeline.class);
		
		SolrUpdateDriver driver = new SolrUpdateDriver();
		driver.setSolrServer(server);
		
		DocumentIndexingPackageFactory dipFactory = mock(DocumentIndexingPackageFactory.class);
		when(dipFactory.createDocumentIndexingPackage(any(PID.class))).thenAnswer(new Answer<DocumentIndexingPackage>() {
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
		
		UpdateTreeAction action = getAction();
		
		action.setTsqs(tsqs);
		action.setPipeline(pipeline);
		action.setSolrUpdateDriver(driver);
		action.setDipFactory(dipFactory);
		action.setAddDocumentMode(false);
		action.init(); 
		
		return action;
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
		newDoc.addField("ancestorNames", "");
		newDoc.addField("resourceType", "Folder");
		docs.add(newDoc);
		
		newDoc = new SolrInputDocument();
		newDoc.addField("title", "A collection");
		newDoc.addField("id", "uuid:2");
		newDoc.addField("rollup", "uuid:2");
		newDoc.addField("roleGroup", "public admin");
		newDoc.addField("readGroup", "public");
		newDoc.addField("adminGroup", "admin");
		newDoc.addField("ancestorNames", "/Collections/A_collection");
		newDoc.addField("ancestorPath", Arrays.asList("1,uuid:1,Collections"));
		newDoc.addField("resourceType", "Collection");
		docs.add(newDoc);
		
		newDoc = new SolrInputDocument();
		newDoc.addField("title", "Subfolder 1");
		newDoc.addField("id", "uuid:4");
		newDoc.addField("rollup", "uuid:4");
		newDoc.addField("roleGroup", "public admin");
		newDoc.addField("readGroup", "public");
		newDoc.addField("adminGroup", "admin");
		newDoc.addField("ancestorNames", "/Collections/A_collection/Subfolder_1");
		newDoc.addField("ancestorPath", Arrays.asList("1,uuid:1,Collections", "2,uuid:2,A collection"));
		newDoc.addField("resourceType", "Folder");
		docs.add(newDoc);
		
		newDoc = new SolrInputDocument();
		newDoc.addField("title", "Orphaned");
		newDoc.addField("id", "uuid:5");
		newDoc.addField("rollup", "uuid:5");
		newDoc.addField("roleGroup", "public admin");
		newDoc.addField("readGroup", "public");
		newDoc.addField("adminGroup", "admin");
		newDoc.addField("ancestorNames", "/Collections/A_collection");
		newDoc.addField("ancestorPath", Arrays.asList("1,uuid:1,Collections", "2,uuid:2,A collection"));
		newDoc.addField("resourceType", "File");
		docs.add(newDoc);
		
		newDoc = new SolrInputDocument();
		newDoc.addField("title", "File");
		newDoc.addField("id", "uuid:6");
		newDoc.addField("rollup", "uuid:6");
		newDoc.addField("roleGroup", "public admin");
		newDoc.addField("readGroup", "public");
		newDoc.addField("adminGroup", "admin");
		newDoc.addField("ancestorNames", "/Collections/A_collection");
		newDoc.addField("ancestorPath", Arrays.asList("1,uuid:1,Collections", "2,uuid:2,A collection"));
		newDoc.addField("resourceType", "File");
		docs.add(newDoc);
		
		newDoc = new SolrInputDocument();
		newDoc.addField("title", "Second collection");
		newDoc.addField("id", "uuid:3");
		newDoc.addField("rollup", "uuid:3");
		newDoc.addField("roleGroup", "public admin");
		newDoc.addField("readGroup", "public");
		newDoc.addField("adminGroup", "admin");
		newDoc.addField("ancestorNames", "/Collections/Second_collection");
		newDoc.addField("ancestorPath", Arrays.asList("1,uuid:1,Collections"));
		newDoc.addField("resourceType", "Collection");
		docs.add(newDoc);
		
		return docs;
	}
}
