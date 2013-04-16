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

import org.jdom.Document;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.JMSMessageUtil;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

public abstract class AbstractXMLEventMessage extends EnhancementMessage {
	private static final long serialVersionUID = 1L;
	protected Document messageBody = null;
	private String eventTimestamp = null;
	
	protected AbstractXMLEventMessage(){
	}
	
	protected AbstractXMLEventMessage(Document messageBody){
		this.messageBody = messageBody;
		this.pid = new PID(JMSMessageUtil.getPid(messageBody));
		setAction(JMSMessageUtil.getAction(messageBody));
		if (pid == null || action == null)
			throw new IllegalArgumentException("Unabled to find either a PID or action in the message body.");
		extractMessageID();
		extractEventTimestamp();
	}

	public Document getMessageBody() {
		return messageBody;
	}

	public void setMessageBody(Document messageBody) {
		this.messageBody = messageBody;
	}
	
	public void setAction(String action){
		this.action = action;
		setQualifiedAction();
	}
	
	public String getEventTimestamp() {
		return eventTimestamp;
	}

	public void setEventTimestamp(String eventTimestamp) {
		this.eventTimestamp = eventTimestamp;
	}

	protected void extractMessageID() {
		try {
			messageID = messageBody.getRootElement().getChildTextTrim("id", JDOMNamespaceUtil.ATOM_NS);
		} catch (NullPointerException e){
			messageID = null; 
		}
	}
	
	protected void extractEventTimestamp(){
		try {
			eventTimestamp = messageBody.getRootElement().getChildTextTrim("updated", JDOMNamespaceUtil.ATOM_NS);
		} catch (NullPointerException e){
			eventTimestamp = null; 
		}
	}
}
