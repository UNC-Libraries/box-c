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
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.unc.lib.dl.data.ingest.solr.ExecutionTimer;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateService;
import edu.unc.lib.dl.data.ingest.solr.UpdateDocTransformer;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.data.ingest.solr.util.JDOMQueryUtil;
import edu.unc.lib.dl.fedora.ClientUtils;
import edu.unc.lib.dl.fedora.FedoraDataService;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadataBean;
import edu.unc.lib.dl.search.solr.model.SimpleIdRequest;
import edu.unc.lib.dl.util.IndexingActionType;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

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
		solrIngestService.offer("uuid:2cc0ad2f-ce71-4a79-ba19-7e254f56d635", IndexingActionType.ADD);
		solrIngestService.offer("uuid:64826711-afe8-410a-ab4a-9e710e5e3b0b", IndexingActionType.ADD);
		solrIngestService.offer("uuid:2cc0ad2f-ce71-4a79-ba19-7e254f56d635", IndexingActionType.ADD);
		solrIngestService.offer("uuid:ff456a7c-3434-4cd1-956e-f419b0d78b2c", IndexingActionType.ADD);

		solrIngestService.offer("uuid:2cc0ad2f-ce71-4a79-ba19-7e254f56d635", IndexingActionType.ADD);
		System.out.println("0:" + solrIngestService.statusString());
		solrIngestService.offer("uuid:6b163ea0-b91e-4658-b1a9-fc62d36db778", IndexingActionType.ADD);
		solrIngestService.offer("uuid:abe485b4-9411-4402-b0aa-f88bf5dd690b", IndexingActionType.ADD);
		solrIngestService.offer("uuid:2cc0ad2f-ce71-4a79-ba19-7e254f56d635", IndexingActionType.ADD);
		System.out.println("1:" + solrIngestService.statusString());
		solrIngestService.offer("uuid:c5ace0aa-cb3e-4eb3-9be8-f5c30253c275", IndexingActionType.ADD);
		solrIngestService.offer("uuid:94a5b617-b2bb-4fb1-a5ef-aeb70ecde0bc", IndexingActionType.ADD);
		try {
			System.out.println("Processing tests");
			Thread.sleep(5000L); // one second
		} catch (Exception e) {
		}
		System.out.println("2:" + solrIngestService.statusString());
		solrIngestService.offer("uuid:353323d2-253c-48eb-94d0-bee7ab596ead", IndexingActionType.ADD);
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
		solrIngestService.offer("uuid:48c47dfc-88a6-4429-8d3b-1f6f0fa52f29", IndexingActionType.ADD);
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
		solrIngestService.offer(pid, IndexingActionType.RECURSIVE_ADD);
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
		//CutoffFacetNode facet = new HierarchicalFacet.HierarchicalFacetTier("4,uuid:5e037fa7-cb29-42ff-8f69-4d05c57ab8d6,Trip Files");
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
		solrIngestService.offer(pid, IndexingActionType.RECURSIVE_REINDEX);
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
		solrIngestService.offer(pid, IndexingActionType.CLEAN_REINDEX);
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
	
	@Test
	public void testXMLSpeed() throws Exception {
		long start = System.currentTimeMillis();
		for (int i = 0; i < 20000; i++) {
			Document result = null;
			SAXBuilder builder = new SAXBuilder();
			result = builder.build(new FileInputStream(new File("src/test/resources/foxml/aggregateSplitDepartments.xml")));
		}
		System.out.println("Completed in " + (System.currentTimeMillis() - start));
	}
	
	@Test
	public void testXMLSpeed2() throws Exception {
		long start = System.currentTimeMillis();
		SAXBuilder builder = new SAXBuilder();
		for (int i = 0; i < 20000; i++) {
			Document result = null;
			
			result = builder.build(new FileInputStream(new File("src/test/resources/foxml/aggregateSplitDepartments.xml")));
		}
		System.out.println("Completed in " + (System.currentTimeMillis() - start));
	}
	
	@Test
	public void testXpathSpeed() throws Exception {
		
		SAXBuilder builder = new SAXBuilder();
		Document result = builder.build(new FileInputStream(new File("src/test/resources/foxml/aggregateSplitDepartments.xml")));
		
		Element modsDS = JDOMQueryUtil.getChildByAttribute(result.getRootElement(),
				"datastream", JDOMNamespaceUtil.FOXML_NS, "ID", "MD_DESCRIPTIVE");
		Element modsVersion = JDOMQueryUtil.getMostRecentDatastreamVersion(modsDS.getChildren("datastreamVersion", JDOMNamespaceUtil.FOXML_NS));
		
		XPath contentModelXpath = XPath.newInstance("/foxml:digitalObject/foxml:datastream[@ID='RELS-EXT']/"
				+ "foxml:datastreamVersion/foxml:xmlContent/rdf:RDF/"
				+ "rdf:Description");
		contentModelXpath.addNamespace("foxml", "info:fedora/fedora-system:def/foxml#");
		contentModelXpath.addNamespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
		contentModelXpath.addNamespace("ns6", "info:fedora/fedora-system:def/model#");
		/*Object value = contentModelXpath.selectSingleNode(result);
		System.out.println(value);*/
		
		XPath valueXpath = XPath.newInstance("ns6:hasModel/@rdf:resource");
		valueXpath.addNamespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
		valueXpath.addNamespace("ns6", "info:fedora/fedora-system:def/model#");

		//System.out.println(value);
		
		/*XPath contentModelXpath = XPath.newInstance("/*[local-name() = 'digitalObject']/*[local-name() = 'datastream' and @ID='RELS-EXT']/"
				+ "*[local-name() = 'datastreamVersion']/*[local-name() = 'xmlContent']/*[local-name() = 'RDF']/"
				+ "*[local-name() = 'Description']");*/
		long start = System.currentTimeMillis();
		Element relsExt = (Element) contentModelXpath.selectSingleNode(result);
		for (int i = 0; i < 100000; i++) {
			Object value = valueXpath.selectSingleNode(relsExt);
			if (value == null)
				System.out.println("NULL");
		}
		System.out.println("Completed in " + (System.currentTimeMillis() - start));
	}
	
	@Test
	public void testXpathSpeed2() throws Exception {
		
		SAXBuilder builder = new SAXBuilder();
		Document result = builder.build(new FileInputStream(new File("src/test/resources/foxml/aggregateSplitDepartments.xml")));
		XPath modsXpath = XPath.newInstance("/foxml:digitalObject/foxml:datastream[@ID='MD_DESCRIPTIVE']/"
			+ "foxml:datastreamVersion/foxml:xmlContent/mods:mods");
		modsXpath.addNamespace("foxml", "info:fedora/fedora-system:def/foxml#");
		modsXpath.addNamespace(JDOMNamespaceUtil.MODS_V3_NS);
		
		XPath nameXPath = XPath.newInstance("mods:name");
		nameXPath.addNamespace(JDOMNamespaceUtil.MODS_V3_NS);
		
		Element modsEl = (Element)modsXpath.selectSingleNode(result);
		for (int i = 0; i < 1000000; i++) {
			List<?> names = nameXPath.selectNodes(modsEl);
		}
		
		long start = System.currentTimeMillis();
		for (int i = 0; i < 1000000; i++) {
			List<?> names = nameXPath.selectNodes(modsEl);
		}
		System.out.println("Completed in " + (System.currentTimeMillis() - start));
		
		start = System.currentTimeMillis();
		for (int i = 0; i < 1000000; i++) {
			List<?> names = modsEl.getChildren("name", JDOMNamespaceUtil.MODS_V3_NS);
		}
		System.out.println("Completed in " + (System.currentTimeMillis() - start));
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
