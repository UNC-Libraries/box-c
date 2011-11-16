package edu.unc.lib.dl.cdr.services.processing;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import static org.mockito.Mockito.*;

import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import edu.unc.lib.dl.cdr.services.model.PIDMessage;
import edu.unc.lib.dl.cdr.services.util.JMSMessageUtil;
import edu.unc.lib.dl.data.ingest.solr.SolrDataAccessLayer;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateAction;
import edu.unc.lib.dl.data.ingest.solr.UpdateDocTransformer;
import edu.unc.lib.dl.fedora.FedoraDataService;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.service.SolrSearchService;
import edu.unc.lib.dl.util.TripleStoreQueryService;

public class SolrUpdateConductorTest extends Assert {

	SolrUpdateConductor solrUpdateConductor;
	private MessageDirector messageDirector;
	
	public SolrUpdateConductorTest() throws Exception {
		this.messageDirector = new MessageDirector();
		this.solrUpdateConductor = new SolrUpdateConductor();
		
		String simpleObjectXML = readFileAsString("fedoraObjectSimple.xml");
		SAXBuilder sb = new SAXBuilder();
		Document simpleObject = sb.build(new StringReader(simpleObjectXML));
		
		SolrDataAccessLayer solrDataAccessLayer = mock(SolrDataAccessLayer.class);
		SolrSearchService solrSearchService = mock(SolrSearchService.class);
		
		FedoraDataService fedoraDataService = mock(FedoraDataService.class);
		when(fedoraDataService.getObjectViewXML(startsWith("uuid:"))).thenReturn(simpleObject);
		
		TripleStoreQueryService tripleStoreQueryService = mock(TripleStoreQueryService.class);
		when(tripleStoreQueryService.isOrphaned(argThat(new IsMatchingPID("uuid:")))).thenReturn(false);
		when(tripleStoreQueryService.allowIndexing(argThat(new IsMatchingPID("uuid:")))).thenReturn(true);
		when(tripleStoreQueryService.fetchByRepositoryPath("/Collections")).thenReturn(new PID("collections"));
		
		when(fedoraDataService.getTripleStoreQueryService()).thenReturn(tripleStoreQueryService);
		
		UpdateDocTransformer updateDocTransformer = new UpdateDocTransformer();
		updateDocTransformer.setXslName("generateAddDoc.xsl");
		
		
		solrUpdateConductor.setSolrSearchService(solrSearchService);
		solrUpdateConductor.setSolrDataAccessLayer(solrDataAccessLayer);
		solrUpdateConductor.setFedoraDataService(fedoraDataService);
		solrUpdateConductor.setUpdateDocTransformer(updateDocTransformer);
		solrUpdateConductor.setMaxIngestThreads(3);
		
		solrUpdateConductor.init();
		
		List<MessageFilter> filters = new ArrayList<MessageFilter>();
		filters.add(new SolrUpdateMessageFilter());
		messageDirector.setFilters(filters);
		List<MessageConductor> conductorsList = new ArrayList<MessageConductor>();
		conductorsList.add(solrUpdateConductor);
		messageDirector.setConductorsList(conductorsList);
	}
	
	@Test
	public void queueOperations(){
		int numberTestMessages = 10;
		solrUpdateConductor.setAutoCommit(false);
		
		//Add messages and check that they all ran
		for (int i=0; i<numberTestMessages; i++){
			PIDMessage message = new PIDMessage("uuid:" + i, SolrUpdateAction.namespace, 
					SolrUpdateAction.ADD.getName());
			messageDirector.direct(message);
		}
		while (!solrUpdateConductor.isEmpty());
		
		assertEquals(solrUpdateConductor.getQueueSize(), 0);
		assertEquals(solrUpdateConductor.getLockedPids().size(), 0);
		assertEquals(solrUpdateConductor.getUpdateDocTransformer().getDocumentCount(), numberTestMessages);
		solrUpdateConductor.getUpdateDocTransformer().clearDocs();
		
		//Test that nothing processes while paused
		solrUpdateConductor.pause();
		for (int i=0; i<numberTestMessages; i++){
			PIDMessage message = new PIDMessage("uuid:" + i, SolrUpdateAction.namespace, 
					SolrUpdateAction.ADD.getName());
			messageDirector.direct(message);
		}
		assertEquals(solrUpdateConductor.getQueueSize(), numberTestMessages);
		assertEquals(solrUpdateConductor.getLockedPids().size(), 0);
		assertFalse(solrUpdateConductor.isEmpty());
		assertTrue(solrUpdateConductor.isReady());
		assertTrue(solrUpdateConductor.isIdle());
		assertEquals(solrUpdateConductor.getUpdateDocTransformer().getDocumentCount(), 0);
		
		//Ensure paused items process when unpaused
		solrUpdateConductor.resume();
		while (!solrUpdateConductor.isEmpty());
		assertEquals(solrUpdateConductor.getUpdateDocTransformer().getDocumentCount(), numberTestMessages);
		assertTrue(solrUpdateConductor.isEmpty());
		
		solrUpdateConductor.getUpdateDocTransformer().clearDocs();
		
		//Check that collision list gets populated
		solrUpdateConductor.pause();
		for (int i=0; i<numberTestMessages; i++){
			PIDMessage message = new PIDMessage("uuid:" + i, SolrUpdateAction.namespace, 
					SolrUpdateAction.ADD.getName());
			messageDirector.direct(message);
			messageDirector.direct(message);
			messageDirector.direct(message);
		}
		assertEquals(solrUpdateConductor.getQueueSize(), numberTestMessages * 3);
		
		solrUpdateConductor.resume();
		while (solrUpdateConductor.getLockedPids().size() < solrUpdateConductor.getMaxIngestThreads());
		solrUpdateConductor.pause();
		
		assertEquals(solrUpdateConductor.getLockedPids().size(), solrUpdateConductor.getMaxIngestThreads());
		//Collision list should contain 
		/*assertEquals(solrUpdateConductor.getCollisionList().size(), 
				(solrUpdateConductor.getMaxIngestThreads() - 1) * 2 - solrUpdateConductor.getUpdateDocTransformer().getDocumentCount());*/
		assertEquals(solrUpdateConductor.getThreadPoolExecutor().getQueue().size(), numberTestMessages * 3
				- (solrUpdateConductor.getLockedPids().size() + solrUpdateConductor.getUpdateDocTransformer().getDocumentCount()));
		
		solrUpdateConductor.clearState();
		assertTrue(solrUpdateConductor.getPidQueue().size() == 0);
		assertTrue(solrUpdateConductor.getCollisionList().size() == 0);
		assertTrue(solrUpdateConductor.getLockedPids().size() == 0);
		assertTrue(solrUpdateConductor.getThreadPoolExecutor().getQueue().size() == 0);
	}
	
	class IsMatchingPID extends ArgumentMatcher<PID> {
		private String pid;
		
		public IsMatchingPID(String pid){
			this.pid = pid;
		}
		
      public boolean matches(Object pid) {
      	return ((PID) pid).getPid().startsWith(this.pid);
      }
   }
	
	protected String readFileAsString(String filePath) throws java.io.IOException {
		StringBuffer fileData = new StringBuffer(1000);
		java.io.InputStream inStream = this.getClass().getResourceAsStream(filePath);
		java.io.InputStreamReader inStreamReader = new InputStreamReader(inStream);
		BufferedReader reader = new BufferedReader(inStreamReader);
		// BufferedReader reader = new BufferedReader(new
		// InputStreamReader(this.getClass().getResourceAsStream(filePath)));
		char[] buf = new char[1024];
		int numRead = 0;
		while ((numRead = reader.read(buf)) != -1) {
			String readData = String.valueOf(buf, 0, numRead);
			fileData.append(readData);
			buf = new char[1024];
		}
		reader.close();
		return fileData.toString();
	}
}
