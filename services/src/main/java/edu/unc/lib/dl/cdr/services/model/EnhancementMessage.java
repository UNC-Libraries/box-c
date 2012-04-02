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
import edu.unc.lib.dl.cdr.services.ObjectEnhancementService;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.message.ActionMessage;

/**
 * Message object which stores attributes needed for the application of enhancements.
 * @author bbpennel
 *
 */
public class EnhancementMessage implements ActionMessage {
	protected String messageID = null;
	protected PID pid;
	protected PID depositID;
	protected String namespace = null;
	protected String action = null;
	protected String qualifiedAction = null;
	protected String serviceName = null;
	protected long timeCreated = System.currentTimeMillis();
	protected List<ObjectEnhancementService> filteredServices = null;
	
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
		return this.pid.getPid();
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

	public List<ObjectEnhancementService> getFilteredServices() {
		return filteredServices;
	}
	
	public boolean filteredServicesContains(Class<?> serviceClass){
		for (ObjectEnhancementService service: this.filteredServices){
			if (serviceClass.equals(service.getClass())){
				return true;
			}
		}
		return false;
	}

	public void setFilteredServices(List<ObjectEnhancementService> filteredServices) {
		this.filteredServices = filteredServices;
	}
	
	@Override
	public String toString(){
		return pid.getPid() + ":" + action;
	}

}
