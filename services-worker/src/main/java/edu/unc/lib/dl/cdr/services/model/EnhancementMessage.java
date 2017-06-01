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

import java.util.List;

import org.jdom2.Document;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.message.ActionMessage;

/**
 * Message object which stores attributes needed for the application of enhancements.
 * @author bbpennel
 *
 */
public class EnhancementMessage implements ActionMessage {
	private static final long serialVersionUID = 1L;
	
	protected String messageID = null;
	protected PID pid;
	protected String targetLabel;
	protected PID depositID;
	protected String namespace = null;
	protected String action = null;
	protected String qualifiedAction = null;
	protected String serviceName = null;
	protected String activeService = null;
	protected long timeCreated = System.currentTimeMillis();
	protected long timeFinished = -1;
	protected boolean force = false;
	protected List<String> filteredServices = null;
	protected List<String> executedServices = null;
	protected List<String> completedServices = null;
	protected Document foxml;
	
	protected EnhancementMessage(){
	}
	
	public EnhancementMessage(String pid, String namespace, String action){
		this(pid, namespace, action, null);
	}
	
	public EnhancementMessage(PID pid, String namespace, String action){
		this(pid, namespace, action, null);
	}
	
	public EnhancementMessage(String pid, String namespace, String action, String service){
		this(new PID(pid), namespace, action, service);
	}
	
	public EnhancementMessage(PID pid, String namespace, String action, String service){
		if (pid == null || action == null)
			throw new IllegalArgumentException("Both a target pid and an action are required.");
		this.pid = pid;
		this.namespace = namespace;
		setAction(action);
		this.serviceName = service;
	}
	
	protected void setQualifiedAction(){
		if (action != null && action.length() > 0 && this.namespace != null){
			this.qualifiedAction = this.namespace + "/" + action;
		} else {
			this.qualifiedAction = action;
		}
	}
	
	public PID getPid() {
		return pid;
	}

	public void setPid(PID pid) {
		this.pid = pid;
	}

	@Override
	public String getQualifiedAction() {
		return this.qualifiedAction;
	}
	
	public void setAction(String action){
		this.action = action;
		setQualifiedAction();
	}
	
	@Override
	public String getAction() {
		return action;
	}
	
	public void setNamespace(String namespace) {
		this.namespace = namespace;
		setQualifiedAction();
	}
	
	@Override
	public String getNamespace() {
		return namespace;
	}
	
	@Override
	public String getMessageID() {
		return messageID;
	}

	public void setMessageID(String messageID) {
		this.messageID = messageID;
	}
	
	public PID getDepositID() {
		return depositID;
	}

	public void setDepositID(PID depositID) {
		this.depositID = depositID;
	}

	@Override
	public String getTargetID() {
		return this.pid.getPidAsString();
	}
	
	@Override
	public String getTargetLabel() {
		return this.targetLabel;
	}
	
	@Override
	public void setTargetLabel(String targetLabel) {
		this.targetLabel = targetLabel;
	}
	
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getServiceName() {
		return serviceName;
	}

	@Override
	public long getTimeCreated() {
		return timeCreated;
	}

	public void setTimeCreated(long timeCreated) {
		this.timeCreated = timeCreated;
	}

	public long getTimeFinished() {
		return timeFinished;
	}

	public void setTimeFinished(long timeFinished) {
		this.timeFinished = timeFinished;
	}

	public List<String> getFilteredServices() {
		return filteredServices;
	}
	
	public boolean filteredServicesContains(Class<?> serviceClass){
		return this.filteredServices.contains(serviceClass.getName());
	}

	public void setFilteredServices(List<String> filteredServices) {
		this.filteredServices = filteredServices;
	}
	
	public String getActiveService() {
		return activeService;
	}

	public void setActiveService(String activeService) {
		this.activeService = activeService;
	}

	public List<String> getCompletedServices() {
		return completedServices;
	}

	public void setCompletedServices(List<String> completedServices) {
		this.completedServices = completedServices;
	}

	public Document getFoxml() {
		return foxml;
	}

	public void setFoxml(Document foxml) {
		this.foxml = foxml;
	}

	public void setQualifiedAction(String qualifiedAction) {
		this.qualifiedAction = qualifiedAction;
	}

	public boolean isForce() {
		return force;
	}

	public void setForce(boolean force) {
		this.force = force;
	}

	@Override
	public String toString(){
		return pid.getPidAsString() + ":" + action;
	}
}
