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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.abdera.model.Element;
import org.apache.abdera.model.Entry;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

public class AtomPubMetadataParserUtil {

	public static final String ATOM_DC_DATASTREAM = "ATOM_DC";
	private static final QName datastreamQName = new QName("http://cdr.lib.unc.edu/", "datastream");
	private static final QName modsQName = new QName("http://www.loc.gov/mods/v3", "mods");
	private static final String dcNamespace = "http://purl.org/dc/terms/";
	
	/**
	 * Returns a map containing the metadata content as jdom elements associated with their datastream id.  The content
	 * is extracted from an Atom Pub abdera entry.  The entry can contain root level qualified dublin core tags or
	 * a MODS entry, as well as any number of cdr:datastream tags containing specific metadata streams to extract.
	 * 
	 * If a datastream tag contains more than one root element, only the first element will be retained
	 * @param entry abdera Atom Pub entry containing metadata for extraction.
	 * @return
	 * @throws IOException
	 * @throws JDOMException
	 */
	public static Map<String,org.jdom.Element> extractDatastreams(Entry entry) throws IOException, JDOMException{
		if (entry == null || entry.getElements().size() == 0){
			return null;
		}
		Map<String,org.jdom.Element> datastreamMap = new HashMap<String,org.jdom.Element>();
		//Outstream containing the compiled default dublin core tags
		ByteArrayOutputStream dcOutStream = null;

		try {
			for (Element element: entry.getElements()){
				if (datastreamQName.equals(element.getQName())){
					//Create new datastream entry
					String id = element.getAttributeValue("id");
					if (id != null){
						org.jdom.Element jdomElement = abderaToJDOM(element);
						org.jdom.Element dsContentElement = null;
						//Store the first child of the datastream tag as the content for this DS
						if (jdomElement.getChildren().size() > 0){
							dsContentElement = ((org.jdom.Element)jdomElement.getChildren().get(0));
							datastreamMap.put(id, (org.jdom.Element)dsContentElement.detach());
						}
					}
				} else if (modsQName.equals(element.getQName())){
					//Create the default mods datastream, taking precedence over the stub from DC terms
					org.jdom.Element modsElement = abderaToJDOM(element);
					datastreamMap.put(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName(), modsElement);
				} else if (dcNamespace.equals(element.getQName().getNamespaceURI())){
					//Populate dublin core properties from the default entry metadata
					if (dcOutStream == null){
						// Add in a stub for MD_DESCRIPTIVE if no MODS have been added yet.
						if (!datastreamMap.containsKey(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName())){
							datastreamMap.put(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName(), null);
						}
						dcOutStream = new ByteArrayOutputStream();
						dcOutStream.write("<dcterms:dc xmlns:dcterms=\"http://purl.org/dc/terms/\">".getBytes("UTF-8"));
					}
					element.writeTo(dcOutStream);
				}
			}
			
			//Create the atom dublin core default datastream if it's populated
			if (dcOutStream != null){
				dcOutStream.write("</dcterms:dc>".getBytes("UTF-8"));
				SAXBuilder saxBuilder = new SAXBuilder();
				ByteArrayInputStream inStream = new ByteArrayInputStream(dcOutStream.toByteArray());
				org.jdom.Document jdomDocument = saxBuilder.build(inStream);
				datastreamMap.put(ATOM_DC_DATASTREAM, jdomDocument.detachRootElement());
				inStream.close();
			}
		} finally {
			if (dcOutStream != null)
				dcOutStream.close();
		}
		return datastreamMap;
	}

	/**
	 * Converts an abdera element to a jdom element by converting it back to raw xml.
	 * @param element
	 * @return
	 * @throws JDOMException
	 * @throws IOException
	 */
	public static org.jdom.Element abderaToJDOM(Element element) throws JDOMException, IOException{
		SAXBuilder saxBuilder = new SAXBuilder();
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		ByteArrayInputStream inStream = null;
		try {
			element.writeTo(outStream);
			inStream = new ByteArrayInputStream(outStream.toByteArray());
			org.jdom.Document jdomDocument = saxBuilder.build(inStream);
			return jdomDocument.detachRootElement();
		} finally {
			outStream.close();
			if (inStream != null)
				inStream.close();
		}
	}
}
