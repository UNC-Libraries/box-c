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
package edu.unc.lib.dl.cdr.services.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;

import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

public class JMSMessageUtil {
	public static final String fedoraMessageNamespace = JDOMNamespaceUtil.CDR_MESSAGE_NS + "/fedora";
	public static final String cdrMessageNamespace = JDOMNamespaceUtil.CDR_MESSAGE_NS + "/cdrAdmin";
	public static final String servicesMessageNamespace = JDOMNamespaceUtil.CDR_MESSAGE_NS + "/services";
	
	public String getFedoraMessageNamespace(){
		return fedoraMessageNamespace;
	}
	
	public String getCdrMessageNamespace(){
		return cdrMessageNamespace;
	}
	
	public static enum FedoraActions {
		MODIFY_DATASTREAM_BY_VALUE ("modifyDatastreamByValue"),
		MODIFY_DATASTREAM_BY_REFERENCE ("modifyDatastreamByReference"),
		ADD_DATASTREAM ("addDatastream"),
		PURGE_OBJECT ("purgeObject"),
		PURGE_DATASTREAM ("purgeDatastream"),
		ADD_RELATIONSHIP ("addRelationship"),
		PURGE_RELATIONSHIP ("purgeRelationship"),
		INGEST ("ingest");

		private String name;
		private final URI uri;

		FedoraActions(String name){
			this.name = name;
			try {
				this.uri = new URI(fedoraMessageNamespace + "/" + name);
			} catch (URISyntaxException e) {
				Error x = new ExceptionInInitializerError("Error creating URI for " + fedoraMessageNamespace + " " + name);
				x.initCause(e);
				throw x;
			}
		}
		
		public String getName(){
			return name;
		}

		public boolean equals(String value){
			return this.uri.toString().equals(value);
		}

		@Override
		public String toString(){
			return this.uri.toString();
		}
	}

	public static enum CDRActions {
		MOVE ("move"),
		REMOVE ("remove"),
		ADD ("add"),
		REORDER ("reorder"),
		REINDEX ("reindex");

		private String name;
		private final URI uri;

		CDRActions(String name){
			this.name = name;
			try {
				this.uri = new URI(cdrMessageNamespace + "/" + name);
			} catch (URISyntaxException e) {
				Error x = new ExceptionInInitializerError("Error creating URI for " + cdrMessageNamespace + " " + name);
				x.initCause(e);
				throw x;
			}
		}

		public String getName(){
			return name;
		}

		public boolean equals(String value){
			return this.uri.toString().equals(value);
		}

		@Override
		public String toString(){
			return this.uri.toString();
		}
	}
	
	public static enum ServicesActions {
		APPLY_SERVICE_STACK("APPLY_SERVICE_STACK"),
		APPLY_SERVICE("PERFORM_SERVICE");

		private String name;
		private final URI uri;

		ServicesActions(String name){
			this.name = name;
			try {
				this.uri = new URI(servicesMessageNamespace + "/" + name);
			} catch (URISyntaxException e) {
				Error x = new ExceptionInInitializerError("Error creating URI for " + servicesMessageNamespace + " " + name);
				x.initCause(e);
				throw x;
			}
		}

		public String getName(){
			return name;
		}

		public boolean equals(String value){
			return this.uri.toString().equals(value);
		}

		@Override
		public String toString(){
			return this.uri.toString();
		}
	}

	public static String getPid(Document message){
		if (message == null)
			return null;
		return message.getRootElement().getChild("summary", JDOMNamespaceUtil.ATOM_NS).getText();
	}
	
	public static String getAction(Document message){
		if (message == null)
			return null;
		return message.getRootElement().getChildTextTrim("title", JDOMNamespaceUtil.ATOM_NS);
	}
	
	/**
	 * Retrieves the affected datastream field value from the provided message.
	 *
	 * @param message
	 * @return
	 */
	public static String getDatastream(Document message){
		if (message == null)
			return null;
	 	@SuppressWarnings("unchecked")
		List<Element> categories = message.getRootElement().getChildren("category", JDOMNamespaceUtil.ATOM_NS);
	 	for (Element category: categories){
	 		String scheme = category.getAttributeValue("scheme");
	 		if ("fedora-types:dsID".equals(scheme)){
	 			return category.getAttributeValue("term");
	 		}
	 	}
	 	return null;
	 }

	/**
	 * @param message
	 * @return
	 */
	public static String getPredicate(Document message) {
		if (message == null)
			return null;
	 	@SuppressWarnings("unchecked")
		List<Element> categories = message.getRootElement().getChildren("category", JDOMNamespaceUtil.ATOM_NS);
	 	for (Element category: categories){
	 		String scheme = category.getAttributeValue("scheme");
	 		if ("fedora-types:relationship".equals(scheme)){
	 			return category.getAttributeValue("term");
	 		}
	 	}
	 	return null;
	}
}
