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
package edu.unc.lib.dl.ws;

import org.apache.log4j.Logger;
import org.springframework.ws.client.core.support.WebServiceGatewaySupport;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;

import edu.unc.lib.dl.agents.Agent;
import edu.unc.lib.dl.agents.AgentFactory;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.schema.DeleteObjectsRequest;
import edu.unc.lib.dl.schema.DeleteObjectsResponse;
import edu.unc.lib.dl.services.DigitalObjectManager;
import edu.unc.lib.dl.util.Constants;

@Endpoint
public class DeleteObjectsEndpoint extends WebServiceGatewaySupport {
	private final Logger logger = Logger.getLogger(getClass());
	private AgentFactory agentManager;
	private DigitalObjectManager digitalObjectManager;

	@PayloadRoot(localPart = Constants.DELETE_OBJECTS_REQUEST, namespace = Constants.NAMESPACE)	
	public DeleteObjectsResponse deleteObjects(DeleteObjectsRequest request) {

		DeleteObjectsThread thread = new DeleteObjectsThread();
		thread.setAgentManager(agentManager);
		thread.setDigitalObjectManager(digitalObjectManager);
		thread.setDeleteObjectsRequest(request);
		thread.start();
		
		DeleteObjectsResponse response = new DeleteObjectsResponse();
		
		response.setResponse("Deletion has been submitted.  Please wait until the User UI reflects the change before initiating another operation.");
		
		return response;
	}

	public DigitalObjectManager getDigitalObjectManager() {
		return digitalObjectManager;
	}

	public void setDigitalObjectManager(DigitalObjectManager digitalObjectManager) {
		this.digitalObjectManager = digitalObjectManager;
	}

	public AgentFactory getAgentManager() {
		return agentManager;
	}

	public void setAgentManager(AgentFactory agentManager) {
		this.agentManager = agentManager;
	}
	
    class DeleteObjectsThread extends Thread {
    	private final Logger logger = Logger.getLogger(getClass());
    	private AgentFactory agentManager;
    	private DigitalObjectManager digitalObjectManager;
    	private DeleteObjectsRequest deleteObjectsRequest;
    	
    	public void run() {
    		// move to threaded web service
    		try {
    			Agent mediator = agentManager.findPersonByOnyen(deleteObjectsRequest
    					.getAdmin(), false);

    			logger.debug("Delete user: "+mediator.getName());
    			
    			for (int i = 0; i < deleteObjectsRequest.getPid().size(); i++) {
    				PID pid = new PID(deleteObjectsRequest.getPid().get(i));

    				logger.debug("Deleting PID: "+pid.getPid());
    				
    				digitalObjectManager.delete(pid, mediator,
    						"Deleted through delete object UI");
    			}
    		} catch (Exception e) {
    			e.printStackTrace();
    		}
    	}
    	
    	public void setDigitalObjectManager(DigitalObjectManager digitalObjectManager) {
    		this.digitalObjectManager = digitalObjectManager;
    	}

    	public AgentFactory getAgentManager() {
    		return agentManager;
    	}

    	public void setAgentManager(AgentFactory agentManager) {
    		this.agentManager = agentManager;
    	}
		public void setDeleteObjectsRequest(DeleteObjectsRequest deleteObjectsRequest) {
			this.deleteObjectsRequest = deleteObjectsRequest;
		}
	
    }
}