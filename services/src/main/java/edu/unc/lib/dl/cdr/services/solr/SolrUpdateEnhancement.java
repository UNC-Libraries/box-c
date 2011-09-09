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
package edu.unc.lib.dl.cdr.services.solr;

import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.cdr.services.Enhancement;
import edu.unc.lib.dl.cdr.services.JMSMessageUtil;
import edu.unc.lib.dl.cdr.services.exception.EnhancementException;
import edu.unc.lib.dl.cdr.services.model.PIDMessage;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateAction;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;

/**
 * Enhancement tells the solr ingest service to update the affected pids.
 * 
 * $URL: https://vcs.lib.unc.edu/cdr/cdr-master/trunk/services/src/main/java/edu/unc/lib/dl/cdr/services/solr/SolrUpdateEnhancement.java $ 
 * $Id: SolrUpdateEnhancement.java 2736 2011-08-08 20:04:52Z count0 $
 * @author bbpennel 
 */
public class SolrUpdateEnhancement extends Enhancement<Element> {
	private static final Logger LOG = LoggerFactory.getLogger(SolrUpdateEnhancement.class);
	SolrUpdateEnhancementService service = null;

	@Override
	public Element call() throws EnhancementException {
		Element result = null;
		LOG.debug("Called Solr update service for " + pid.getPID());
		
		if (pid.getMessage() == null){
			//If there was no message, then perform a single item update
			service.getSolrUpdateService().offer(new SolrUpdateRequest(pid.getPIDString(), SolrUpdateAction.ADD));
		} else {
			//Determine update event based on the type of message being processed.
			String action = pid.getAction();
			if (JMSMessageUtil.FedoraActions.PURGE_OBJECT.equals(action)){
				//Delete item event.
				service.getSolrUpdateService().offer(pid.getPIDString(), SolrUpdateAction.DELETE_SOLR_TREE);
			} else if (JMSMessageUtil.CDRActions.MOVE.equals(action) || JMSMessageUtil.CDRActions.ADD.equals(action)
					|| JMSMessageUtil.CDRActions.REORDER.equals(action)){
				//Custom messages which contain lists of items for reloading
				pid.generateCDRMessageContent();
				if (JMSMessageUtil.CDRActions.MOVE.equals(action) || JMSMessageUtil.CDRActions.ADD.equals(action)){
					//Move and add are both recursive adds of all subjects, plus a nonrecursive update for reordered children.
					for (String pidString: pid.getCDRMessageContent().getSubjects()){
						service.getSolrUpdateService().offer(new SolrUpdateRequest(pidString, SolrUpdateAction.RECURSIVE_ADD));
					}
				}
				// Reorder is a non-recursive add.
				for (String pidString: pid.getCDRMessageContent().getReordered()){
					service.getSolrUpdateService().offer(new SolrUpdateRequest(pidString, SolrUpdateAction.ADD));
				}
			} else if (JMSMessageUtil.CDRActions.REINDEX.equals(action)){
				//Determine which kind of reindex to perform based on the mode
				pid.generateCDRMessageContent();
				if (pid.getCDRMessageContent().getMode().equals("inplace")){
					service.getSolrUpdateService().offer(new SolrUpdateRequest(pid.getCDRMessageContent().getParent(), SolrUpdateAction.RECURSIVE_REINDEX));
				} else {
					service.getSolrUpdateService().offer(new SolrUpdateRequest(pid.getCDRMessageContent().getParent(), SolrUpdateAction.CLEAN_REINDEX));
				}
			} else {
				//For all other message types, do a single record update
				service.getSolrUpdateService().offer(pid.getPIDString());
			}
		}
		return result;
	}

	public SolrUpdateEnhancement(SolrUpdateEnhancementService service, PIDMessage pid) {
		super(pid);
		this.service = service;
	}
}
