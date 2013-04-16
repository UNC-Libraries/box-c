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
package edu.unc.lib.dl.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.parser.Parser;
import org.jdom.output.XMLOutputter;
import org.junit.Assert;
import org.junit.Test;

import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

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
		
		//this.outputDatastreams(datastreamMap);
	}
	
	@Test
	public void testDCOnlyExtraction() throws Exception{
		InputStream entryPart = new FileInputStream(new File("src/test/resources/atompub/metadataDC.xml"));
		Abdera abdera = new Abdera();
		Parser parser = abdera.getParser();
		Document<Entry> entryDoc = parser.parse(entryPart);
		Entry entry = entryDoc.getRoot();
		Map<String,org.jdom.Element> datastreamMap = AtomPubMetadataParserUtil.extractDatastreams(entry);
		
		org.jdom.Element dcDS = datastreamMap.get(AtomPubMetadataParserUtil.ATOM_DC_DATASTREAM);
		org.jdom.Element modsDS = datastreamMap.get(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName());
		org.jdom.Element relsExtDS = datastreamMap.get(ContentModelHelper.Datastream.RELS_EXT.getName());
		//Atom DC should be set
		assertNotNull(dcDS);
		assertNull(relsExtDS);
		//Results should contain MD_DESCRIPTIVE entry, but null content
		assertTrue(datastreamMap.containsKey(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName()));
		assertNull(modsDS);
		
		//Make sure everything has been unwrapped
		assertTrue(dcDS.getName().equals("dc"));
	}
	
	private void outputDatastreams(Map<String,org.jdom.Element> datastreamMap) throws IOException {
		XMLOutputter outputter = new XMLOutputter();
		java.util.Iterator<java.util.Map.Entry<String,org.jdom.Element>> it = datastreamMap.entrySet().iterator();
		
		while (it.hasNext()){
			java.util.Map.Entry<String,org.jdom.Element> element = it.next();
			System.out.println(element.getKey() + ":\n");
			outputter.output(element.getValue(), System.out);
		}
	}
	
	@Test
	public void aclAndRELSExt() throws Exception {
		InputStream entryPart = new FileInputStream(new File("src/test/resources/atompub/metadataRELSEXTAndACL.xml"));
		Abdera abdera = new Abdera();
		Parser parser = abdera.getParser();
		Document<Entry> entryDoc = parser.parse(entryPart);
		Entry entry = entryDoc.getRoot();
		Map<String,org.jdom.Element> datastreamMap = AtomPubMetadataParserUtil.extractDatastreams(entry);
		
		org.jdom.Element relsExtDS = datastreamMap.get(ContentModelHelper.Datastream.RELS_EXT.getName());
		org.jdom.Element description = relsExtDS.getChild("Description", JDOMNamespaceUtil.RDF_NS);
		
		assertEquals(4, description.getChildren().size());
		assertEquals(4, description.getChildren(ContentModelHelper.FedoraProperty.hasModel.name(), JDOMNamespaceUtil.FEDORA_MODEL_NS).size());
		
		assertTrue(datastreamMap.containsKey("ACL"));
		assertNotNull(datastreamMap.get("ACL"));
	}
	
	@Test
	public void aclNoRELSEXT() throws Exception {
		InputStream entryPart = new FileInputStream(new File("src/test/resources/atompub/metadataUnpublish.xml"));
		Abdera abdera = new Abdera();
		Parser parser = abdera.getParser();
		Document<Entry> entryDoc = parser.parse(entryPart);
		Entry entry = entryDoc.getRoot();
		Map<String,org.jdom.Element> datastreamMap = AtomPubMetadataParserUtil.extractDatastreams(entry);
		
		assertTrue(datastreamMap.containsKey(ContentModelHelper.Datastream.RELS_EXT.getName()));
		assertNull(datastreamMap.get(ContentModelHelper.Datastream.RELS_EXT.getName()));
		
		assertTrue(datastreamMap.containsKey("ACL"));
		assertNotNull(datastreamMap.get("ACL"));
	}
}
