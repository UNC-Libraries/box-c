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
package edu.unc.lib.dl.cdr.services.jmx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.cdr.services.model.PIDMessage;
import edu.unc.lib.dl.cdr.services.processing.MessageDirector;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateAction;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateService;

/**
 * Service which provides methods for directly issuing Solr update requests to allow JMX
 * console usage.
 * @author bbpennel
 *
 */
public class SolrJMXService {
	private static final Logger LOG = LoggerFactory.getLogger(SolrJMXService.class);
	private MessageDirector messageDirector;
	private boolean active;
	
	public String getTargetAllSelector(){
		return SolrUpdateService.TARGET_ALL;
	}
	
	public void update(String pid){
		if (!active) return;
		messageDirector.direct(new PIDMessage(pid, SolrUpdateAction.namespace, SolrUpdateAction.ADD.getName()));
	}
	
	public void reindex(String pid, boolean inplace){
		if (!active) return;
		LOG.info("Issuing request to recursively reindex " + pid + ", to be performed inplace:" + inplace + ".");
		if (inplace){
			messageDirector.direct(new PIDMessage(pid, SolrUpdateAction.namespace, SolrUpdateAction.RECURSIVE_REINDEX.getName()));
		} else {
			messageDirector.direct(new PIDMessage(pid, SolrUpdateAction.namespace, SolrUpdateAction.CLEAN_REINDEX.getName()));
		}
	}
	
	public void clearIndex(String confirmation){
		if (!active || !confirmation.equalsIgnoreCase("yes")) return;
		LOG.info("Issuing request to delete all Solr index contents.");
		messageDirector.direct(new PIDMessage("", SolrUpdateAction.namespace, SolrUpdateAction.CLEAR_INDEX.getName()));
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public MessageDirector getMessageDirector() {
		return messageDirector;
	}

	public void setMessageDirector(MessageDirector messageDirector) {
		this.messageDirector = messageDirector;
	}
	
}
