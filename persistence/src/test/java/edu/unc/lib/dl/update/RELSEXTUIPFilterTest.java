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
package edu.unc.lib.dl.update;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.parser.Parser;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.junit.Assert;
import org.junit.Test;

import edu.unc.lib.dl.fedora.DatastreamPID;
import edu.unc.lib.dl.util.AtomPubMetadataParserUtil;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

public class RELSEXTUIPFilterTest extends Assert {
	
	@Test
	public void replaceTest() throws Exception {
		InputStream entryPart = new FileInputStream(new File("src/test/resources/atompub/metadataFullRELSEXTAndACL.xml"));
		Abdera abdera = new Abdera();
		Parser parser = abdera.getParser();
		Document<Entry> entryDoc = parser.parse(entryPart);
		Entry entry = entryDoc.getRoot();
		Map<String, org.jdom.Element> originalMap = new HashMap<String, org.jdom.Element>();
		
		SAXBuilder builder = new SAXBuilder();
		InputStream inStream = new FileInputStream(new File("src/test/resources/fedora/exampleRELSEXT.xml"));
		org.jdom.Document doc = builder.build(inStream);
		org.jdom.Element baseElement = doc.detachRootElement();
		
		originalMap.put(ContentModelHelper.Datastream.RELS_EXT.getName(), baseElement);
		Map<String, org.jdom.Element> datastreamMap = AtomPubMetadataParserUtil.extractDatastreams(entry);

		MetadataUIP uip = mock(MetadataUIP.class);
		when(uip.getPID()).thenReturn(new DatastreamPID("uuid:test"));
		when(uip.getOperation()).thenReturn(UpdateOperation.REPLACE);
		when(uip.getOriginalData()).thenReturn(originalMap);
		when(uip.getModifiedData()).thenReturn(new HashMap<String, org.jdom.Element>());
		when(uip.getIncomingData()).thenReturn(datastreamMap);

		RELSEXTUIPFilter filter = new RELSEXTUIPFilter();
		filter.doFilter(uip);

		org.jdom.Element relsExtDS = uip.getModifiedData().get(ContentModelHelper.Datastream.RELS_EXT.getName());
		org.jdom.Element description = relsExtDS.getChild("Description", JDOMNamespaceUtil.RDF_NS);

		//this.outputDatastreams(datastreamMap);
		assertEquals(11, description.getChildren().size());
		assertEquals("Very small mp3", description.getChildText("slug", JDOMNamespaceUtil.CDR_NS));

	}
	
	protected void outputDatastreams(Map<String, org.jdom.Element> datastreamMap) throws IOException {
		if (datastreamMap == null)
			return;
		XMLOutputter outputter = new XMLOutputter();
		java.util.Iterator<java.util.Map.Entry<String, org.jdom.Element>> it = datastreamMap.entrySet().iterator();

		while (it.hasNext()) {
			java.util.Map.Entry<String, org.jdom.Element> element = it.next();
			if (element.getValue() == null) {
				System.out.println(element.getKey() + ": null");
				continue;
			}
			System.out.println(element.getKey() + ":\n");
			outputter.output(element.getValue(), System.out);
		}
	}
}
