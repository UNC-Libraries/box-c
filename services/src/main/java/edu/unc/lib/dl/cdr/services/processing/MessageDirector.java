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

package edu.unc.lib.dl.cdr.services.processing;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.unc.lib.dl.message.ActionMessage;

public class MessageDirector {
	private Map<String,MessageConductor> conductors = null;
	private List<MessageFilter> filters = null;
	
	public MessageDirector(){
	}
	
	/**
	 * Packages and forwards the message to the appropriate message conductor if
	 * it passes the associated prefilter.
	 * @param message
	 */
	public void direct(ActionMessage message){
		for (MessageFilter filter: filters){
			if (filter.filter(message)){
				conductors.get(filter.getConductor()).add(message);
			}
		}
	}
	
	public Map<String, MessageConductor> getConductors() {
		return conductors;
	}

	public void setConductors(Map<String, MessageConductor> conductors) {
		this.conductors = conductors;
	}
	
	public void setConductorsList(List<MessageConductor> conductorsList){
		this.conductors = new HashMap<String, MessageConductor>();
		for (MessageConductor conductor: conductorsList){
			this.conductors.put(conductor.getIdentifier(), conductor);
		}
	}

	public List<MessageFilter> getFilters() {
		return filters;
	}

	public void setFilters(List<MessageFilter> filters) {
		this.filters = filters;
	}
}
