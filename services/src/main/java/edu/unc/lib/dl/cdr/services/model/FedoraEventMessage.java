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

/**
 * Extracts and stores data specific to Fedora JMS event messages.
 * @author bbpennel
 *
 */
public class FedoraEventMessage extends AbstractXMLEventMessage {
	private static final long serialVersionUID = 1L;
	private String datastream = null;
	private String relationPredicate = null;
	private String relationObject = null;
	
	public FedoraEventMessage(Document messageBody) {
		super(messageBody);
		this.setNamespace(JMSMessageUtil.fedoraMessageNamespace);
		extractDatastream();
		extractRelation();
	}
	
	public FedoraEventMessage(String pid, String action){
		this(pid, JMSMessageUtil.fedoraMessageNamespace, action, null, null);
	}
	
	public FedoraEventMessage(String pid, String namespace, String action, String datastream, String relation){
		if (pid == null || action == null)
			throw new IllegalArgumentException("Both a target pid and an action are required.");
		this.setNamespace(JMSMessageUtil.fedoraMessageNamespace);
		this.pid = new PID(pid);
		this.datastream = datastream;
		setAction(action);
		this.relationPredicate = relation;
	}
	
	protected void extractDatastream(){
		try {
			datastream = JMSMessageUtil.getDatastream(messageBody);
		} catch (NullPointerException e){
			datastream = null; 
		}
	}
	
	public String getDatastream() {
    	return datastream;
	}
	
	public void setDatastream(String datastream) {
		this.datastream = datastream;
	}
	
	protected void extractRelation(){
		try {
			relationPredicate = JMSMessageUtil.getPredicate(messageBody);
			if (relationPredicate != null){
				relationObject = JMSMessageUtil.getObject(messageBody);
			} else {
				relationObject = null;
			}
		} catch (NullPointerException e){
			relationPredicate = null;
			relationObject = null;
		}
	}

	public void setRelationPredicate(String relationPredicate) {
		this.relationPredicate = relationPredicate;
	}

	public String getRelationObject() {
		return relationObject;
	}

	public void setRelationObject(String relationObject) {
		this.relationObject = relationObject;
	}

	public String getRelationPredicate() {
		return relationPredicate;
	}
}
