package edu.unc.lib.dl.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.parser.Parser;
import org.jdom.output.XMLOutputter;
import org.junit.Assert;
import org.junit.Test;

public class AtomPubMetadataParserTest extends Assert {

	@Test
	public void testDatastreamExtraction() throws Exception{
		InputStream entryPart = new FileInputStream(new File("src/test/resources/atompub/metadataUpdateMultipleDS.xml"));
		Abdera abdera = new Abdera();
		Parser parser = abdera.getParser();
		Document<Entry> entryDoc = parser.parse(entryPart);
		Entry entry = entryDoc.getRoot();
		Map<String,org.jdom.Element> datastreamMap = AtomPubMetadataParserUtil.extractDatastreams(entry);
		
		org.jdom.Element dcDS = datastreamMap.get(AtomPubMetadataParserUtil.ATOM_DC_DATASTREAM);
		org.jdom.Element modsDS = datastreamMap.get(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName());
		org.jdom.Element relsExtDS = datastreamMap.get(ContentModelHelper.Datastream.RELS_EXT.getName());
		assertNotNull(modsDS);
		assertNotNull(dcDS);
		assertNotNull(relsExtDS);
		
		//Make sure everything has been unwrapped
		assertTrue(modsDS.getName().equals("mods"));
		assertTrue(dcDS.getName().equals("dc"));
		assertTrue(relsExtDS.getName().equals("RDF"));
		
		XMLOutputter outputter = new XMLOutputter();
		java.util.Iterator<java.util.Map.Entry<String,org.jdom.Element>> it = datastreamMap.entrySet().iterator();
		
		while (it.hasNext()){
			java.util.Map.Entry<String,org.jdom.Element> element = it.next();
			System.out.println(element.getKey() + ":\n");
			outputter.output(element.getValue(), System.out);
		}
	}
}
