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
package edu.unc.lib.dl.data.ingest.solr.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.unc.lib.dl.data.ingest.solr.ExecutionTimer;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateAction;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateService;
import edu.unc.lib.dl.data.ingest.solr.UpdateDocTransformer;
import edu.unc.lib.dl.fedora.ClientUtils;
import edu.unc.lib.dl.fedora.FedoraDataService;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadataBean;
import edu.unc.lib.dl.search.solr.model.HierarchicalFacet;
import edu.unc.lib.dl.search.solr.model.SimpleIdRequest;

//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(locations = { "/services-context.xml" })
public class SolrIngestByHand {

	@Autowired
	private SolrUpdateService solrIngestService;
	@Autowired
	private FedoraDataService fedoraDataService;
	@Autowired
	private UpdateDocTransformer updateDocTransformer;

	@Before
	public void setUp() throws Exception {

	}

	public void test() {
		ExecutionTimer t = new ExecutionTimer();
		t.start();
		solrIngestService.offer("uuid:2cc0ad2f-ce71-4a79-ba19-7e254f56d635", SolrUpdateAction.ADD);
		solrIngestService.offer("uuid:64826711-afe8-410a-ab4a-9e710e5e3b0b", SolrUpdateAction.ADD);
		solrIngestService.offer("uuid:2cc0ad2f-ce71-4a79-ba19-7e254f56d635", SolrUpdateAction.ADD);
		solrIngestService.offer("uuid:ff456a7c-3434-4cd1-956e-f419b0d78b2c", SolrUpdateAction.ADD);

		solrIngestService.offer("uuid:2cc0ad2f-ce71-4a79-ba19-7e254f56d635", SolrUpdateAction.ADD);
		System.out.println("0:" + solrIngestService.statusString());
		solrIngestService.offer("uuid:6b163ea0-b91e-4658-b1a9-fc62d36db778", SolrUpdateAction.ADD);
		solrIngestService.offer("uuid:abe485b4-9411-4402-b0aa-f88bf5dd690b", SolrUpdateAction.ADD);
		solrIngestService.offer("uuid:2cc0ad2f-ce71-4a79-ba19-7e254f56d635", SolrUpdateAction.ADD);
		System.out.println("1:" + solrIngestService.statusString());
		solrIngestService.offer("uuid:c5ace0aa-cb3e-4eb3-9be8-f5c30253c275", SolrUpdateAction.ADD);
		solrIngestService.offer("uuid:94a5b617-b2bb-4fb1-a5ef-aeb70ecde0bc", SolrUpdateAction.ADD);
		try {
			System.out.println("Processing tests");
			Thread.sleep(5000L); // one second
		} catch (Exception e) {
		}
		System.out.println("2:" + solrIngestService.statusString());
		solrIngestService.offer("uuid:353323d2-253c-48eb-94d0-bee7ab596ead", SolrUpdateAction.ADD);
		System.out.println("3:" + solrIngestService.statusString());
		do {
			try {
				System.out.println("Processing tests");
				Thread.sleep(200L); // one second
			} catch (Exception e) {
			}
		} while (solrIngestService.queueSize() > 0);
		t.end();
		System.out.println("4:" + solrIngestService.statusString());
		System.out.println("Ingest completed in: " + t.duration());
		solrIngestService.shutdown();
		Assert.assertTrue(true);
	}
	
	//@Test
	public void testAdd(){
		solrIngestService.offer("uuid:48c47dfc-88a6-4429-8d3b-1f6f0fa52f29", SolrUpdateAction.ADD);
		do {
			try {
				System.out.println("Processing tests");
				Thread.sleep(200L); // one second
			} catch (Exception e) {
			}
		} while (solrIngestService.queueSize() > 0 || solrIngestService.collisionSize() > 0 || solrIngestService.lockedSize() > 0 || solrIngestService.activeThreadsCount() > 0);
		solrIngestService.shutdown();
	}

	//@Test
	public void testRecursiveAdd(){
		//uuid:48c47dfc-88a6-4429-8d3b-1f6f0fa52f29
		String pid = "uuid:aac4a586-8afa-4144-b39a-a2f19908065e";//"uuid:0a045247-996e-4bfc-a2db-11925e5bad58";
		solrIngestService.offer(pid, SolrUpdateAction.RECURSIVE_ADD);
		do {
			try {
				System.out.println("Processing tests");
				Thread.sleep(200L); // one second
			} catch (Exception e) {
			}
		} while (solrIngestService.queueSize() > 0 || solrIngestService.collisionSize() > 0 || solrIngestService.lockedSize() > 0 || solrIngestService.activeThreadsCount() > 0);
		System.out.println("Shutting down Solr Update Service");
		solrIngestService.shutdown();
	}
	
	//@Test
	public void testHierarchical(){
		HierarchicalFacet.HierarchicalFacetTier facet = new HierarchicalFacet.HierarchicalFacetTier("4,uuid:5e037fa7-cb29-42ff-8f69-4d05c57ab8d6,Trip Files");
	}
	
	//@Test
	public void testTimezone(){
		String dateTime = "2011-04-14T13:31:19.809";
		List<String> formats = new ArrayList<String>();
		formats.add("yyyy-MM-dd'T'hh:mm:ss.SSS");
		System.out.println("Original: " + dateTime);
		try {
			System.out.println("After: " + org.apache.solr.common.util.DateUtil.parseDate(dateTime));
			System.out.println("After: " + org.apache.solr.common.util.DateUtil.parseDate(dateTime, formats));
			//System.out.println("After: " + edu.unc.lib.dl.search.solr.util.StringUtil.getFormattedDate(dateTime, true, false));
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss'Z'z");
			//formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
			System.out.println("Formatted: " + formatter.format(org.apache.solr.common.util.DateUtil.parseDate(dateTime, formats)));
			System.out.println("Formatted 2: " + org.apache.solr.common.util.DateUtil.getThreadLocalDateFormat().format(org.apache.solr.common.util.DateUtil.parseDate(dateTime, formats)));
			
			SimpleDateFormat formatter2 = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");
			Date edtTime = formatter2.parse("2011-04-14T13:31:19.809");
			System.out.println("edt: " + edtTime);
			System.out.println("edt formatted: " + formatter.format(edtTime));
			
		} catch (Exception e) {
			System.out.println(e);
		}
	}
	
	//@Test
	public void testRecursiveReindex(){
		String pid = "uuid:96f9821e-e9ed-410c-ab88-2e293bd3d1b5";
		//String pid = "uuid:353323d2-253c-48eb-94d0-bee7ab596ead";
		solrIngestService.offer(pid, SolrUpdateAction.RECURSIVE_REINDEX);
		try {
			System.out.println("Processing tests");
			Thread.sleep(25000L); // one second
		} catch (Exception e) {
		}
		do {
			try {
				System.out.println("Processing tests " + solrIngestService.statusString());
				Thread.sleep(4000L); // one second
			} catch (Exception e) {
			}
		} while (solrIngestService.queueSize() > 0 || solrIngestService.collisionSize() > 0 || solrIngestService.lockedSize() > 0 || solrIngestService.activeThreadsCount() > 0);
		solrIngestService.shutdown();
	}
	
	//@Test
	public void testCleanReindex(){
		String pid = "uuid:96f9821e-e9ed-410c-ab88-2e293bd3d1b5";
		//String pid = "uuid:353323d2-253c-48eb-94d0-bee7ab596ead";
		solrIngestService.offer(pid, SolrUpdateAction.CLEAN_REINDEX);
		try {
			System.out.println("Processing tests");
			Thread.sleep(25000L); // one second
		} catch (Exception e) {
		}
		do {
			try {
				System.out.println("Processing tests " + solrIngestService.statusString());
				Thread.sleep(4000L); // one second
			} catch (Exception e) {
			}
		} while (solrIngestService.queueSize() > 0 || solrIngestService.collisionSize() > 0 || solrIngestService.lockedSize() > 0 || solrIngestService.activeThreadsCount() > 0);
		solrIngestService.shutdown();
	}
	
	//@Test
	public void testFedoraDataService() {
		Document doc;
		try {
			//doc = fedoraDataService.getObjectViewXML("uuid:0a045247-996e-4bfc-a2db-11925e5bad58");
			doc = fedoraDataService.getObjectViewXML("uuid:757ac9c1-c90f-42bb-9476-6dda92850b5c");
			
			XMLOutputter out = new XMLOutputter();
			out.output(doc, System.out);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Test
	public void testTransform() throws IOException {
		InputStream inputStream = null;
		try {
			Document doc = new SAXBuilder().build(new FileInputStream(new File("src/test/resources/biomedObjectView.xml")));
			
			XMLOutputter out = new XMLOutputter();
		   out.output(doc, System.out);
		   
		   UpdateDocTransformer transformer = new UpdateDocTransformer();
		   transformer.init();
		   transformer.setXslName("generateAddDoc.xsl");
		   
		   transformer.addDocument(doc);
			
			System.out.println(transformer.toString());
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (inputStream != null)
				inputStream.close();
		}
	}
	
	//@Test
	public void testAllowIndexing(){
		PID pid = new PID("uuid:b56b8fe0-aba3-41d6-8ff8-484129206f3d");
		try {
			
			//List<PID> response = fedoraDataService.getTripleStoreQueryService().fetchByPredicateAndLiteral(ContentModelHelper.CDRProperty.allowIndexing.getURI().toString(), "yes");
			/*String query = String.format("select $pid from <%1$s> where $pid <%3$s> 'yes' "
					+ " and $pid <http://mulgara.org/mulgara#is> <%2$s>",
					fedoraDataService.getTripleStoreQueryService().getResourceIndexModelUri(), pid.getURI(), ContentModelHelper.CDRProperty.allowIndexing.getURI());
			List<List<String>> response = fedoraDataService.getTripleStoreQueryService().queryResourceIndex(query);
			
			PID responsePID = fedoraDataService.getTripleStoreQueryService().verify(pid);*/
			//
			System.out.println("Allow indexing " + fedoraDataService.getTripleStoreQueryService().allowIndexing(pid));
			/*String query = String.format("select ?pid from <%1$s> where {" + 
					"?pid <%2$s> 'yes' " + 
					"filter (?pid = <%3$s>) }", 
					fedoraDataService.getTripleStoreQueryService().getResourceIndexModelUri(), ContentModelHelper.CDRProperty.allowIndexing.getURI(), pid.getURI());
			@SuppressWarnings({ "unchecked", "rawtypes" })
			List response = (List<Map>) ((Map) fedoraDataService.getTripleStoreQueryService().sendSPARQL(query).get("results")).get("bindings");
			*/
			System.out.println("Done");		
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	//@Test
	public void testHierarchicalFacet() {
		try {
			BriefObjectMetadataBean metadata = solrIngestService.getSolrSearchService().getObjectById(new SimpleIdRequest("uuid:b803dc0a-6952-47f8-97ee-9bd56947e443", solrIngestService.getAccessGroups()));
			
			HierarchicalFacet facet = metadata.getPath();
			System.out.println(metadata.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	//@Test
	public void dateTest(){
		int windowSizeHours = 14;
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		String windowEnd = formatter.format(calendar.getTime());
		
		calendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY) - windowSizeHours);
		String windowStart = formatter.format(calendar.getTime());
		
		System.out.println("Window start: " + windowStart);
		System.out.println("Window end: " + windowEnd);
	}

	public SolrUpdateService getSolrIngestService() {
		return solrIngestService;
	}

	public void setSolrIngestService(SolrUpdateService solrIngestService) {
		this.solrIngestService = solrIngestService;
	}

	public FedoraDataService getFedoraDataService() {
		return fedoraDataService;
	}

	public void setFedoraDataService(FedoraDataService fedoraDataService) {
		this.fedoraDataService = fedoraDataService;
	}

	public UpdateDocTransformer getUpdateDocTransformer() {
		return updateDocTransformer;
	}

	public void setUpdateDocTransformer(UpdateDocTransformer updateDocTransformer) {
		this.updateDocTransformer = updateDocTransformer;
	}
}
