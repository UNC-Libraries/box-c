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
package edu.unc.lib.dl.cdr.services.model;

import java.util.ArrayList;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;

import edu.unc.lib.dl.util.JMSMessageUtil;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

/**
 * Stores the data extracted from CDR specific JMS events
 * @author bbpennel
 */
public class CDREventMessage extends AbstractXMLEventMessage {
	private static final long serialVersionUID = 1L;
	private List<String> subjects;
	private List<String> reordered;
	private String parent;
	private List<String> oldParents;
	//Additional mode information for affecting the default behavior of the given action
	private String mode;
	private String operation;
	
	public CDREventMessage(Document messageBody) {
		super(messageBody);
		
		this.setNamespace(JMSMessageUtil.cdrMessageNamespace);
		
		Element content = messageBody.getRootElement().getChild("content", JDOMNamespaceUtil.ATOM_NS);
		if (content == null || content.getChildren().size() == 0)
			return;
		Element contentBody = (Element)content.getChildren().get(0);
		if (contentBody == null)
			return;
		operation = contentBody.getName();
		subjects = populateList("subjects", contentBody);
		reordered = populateList("reordered", contentBody);
		oldParents = populateList("oldParents", contentBody);
		
		parent = contentBody.getChildText("parent", JDOMNamespaceUtil.CDR_MESSAGE_NS);
		mode = contentBody.getChildText("mode", JDOMNamespaceUtil.CDR_MESSAGE_NS);
	}
	
	private List<String> populateList(String field, Element contentBody){
		@SuppressWarnings("unchecked")
		List<Element> children = (List<Element>)contentBody.getChildren(field, JDOMNamespaceUtil.CDR_MESSAGE_NS);
		if (children == null || children.size() == 0)
			return null;
		List<String> list = new ArrayList<String>();
		for (Object node: children){
			Element element = (Element)node;
			for (Object pid: element.getChildren()){
				Element pidElement = (Element)pid;
				list.add(pidElement.getTextTrim());
			}
		}
		return list;
	}

	public List<String> getSubjects() {
		return subjects;
	}

	public void setSubjects(List<String> subjects) {
		this.subjects = subjects;
	}

	public List<String> getReordered() {
		return reordered;
	}

	public void setReordered(List<String> reordered) {
		this.reordered = reordered;
	}

	public String getParent() {
		return parent;
	}

	public void setParent(String parent) {
		this.parent = parent;
	}

	public List<String> getOldParents() {
		return oldParents;
	}

	public void setOldParents(List<String> oldParents) {
		this.oldParents = oldParents;
	}

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	public String getOperation() {
		return operation;
	}

	public void setOperation(String operation) {
		this.operation = operation;
	}
}
