package edu.unc.lib.dl.data.ingest.solr.indexing;

import java.io.File;
import java.io.FileInputStream;

import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.junit.Assert;
import org.junit.Test;

import edu.unc.lib.dl.data.ingest.solr.exception.UnsupportedContentModelException;

public class DocumentIndexingPipelineTest extends Assert {
	@Test(expected=UnsupportedContentModelException.class)
	public void depositReceipt() throws Exception {
		DocumentIndexingPackage dip = new DocumentIndexingPackage("info:fedora/uuid:test");

		SAXBuilder builder = new SAXBuilder();
		Document foxml = builder.build(new FileInputStream(new File(
				"src/test/resources/foxml/depositReceipt.xml")));
		dip.setFoxml(foxml);

		DocumentIndexingPipeline pipeline = new DocumentIndexingPipeline();
		pipeline.process(dip);
	}
}
