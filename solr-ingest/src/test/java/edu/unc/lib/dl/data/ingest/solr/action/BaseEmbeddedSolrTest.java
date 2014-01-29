package edu.unc.lib.dl.data.ingest.solr.action;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreContainer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseEmbeddedSolrTest extends Assert {
	private static final Logger log = LoggerFactory.getLogger(BaseEmbeddedSolrTest.class);
	
	protected EmbeddedSolrServer server;
	protected CoreContainer container;
	
	@Before
	public void setUp() throws Exception {
		
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
}
