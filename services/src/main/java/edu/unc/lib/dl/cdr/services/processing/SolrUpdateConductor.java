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

import edu.unc.lib.dl.cdr.services.model.PIDMessage;
import edu.unc.lib.dl.cdr.services.util.JMSMessageUtil;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateAction;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateService;

public class SolrUpdateConductor extends SolrUpdateService implements MessageConductor {
	public static final String identifier = "SOLR_UPDATE";

	@Override
	public void add(PIDMessage message) {
		String namespace = message.getNamespace();
		String action = message.getAction();
		if (namespace != null && action != null){
			if (namespace.equals(SolrUpdateAction.namespace)){
				SolrUpdateAction actionEnum = SolrUpdateAction.getAction(action);
				if (action != null){
					this.offer(message.getPIDString(), actionEnum);
				}
			} else {
				if (JMSMessageUtil.FedoraActions.PURGE_OBJECT.equals(action)){
					this.offer(message.getPIDString(), SolrUpdateAction.DELETE_SOLR_TREE);
				} else if (JMSMessageUtil.CDRActions.MOVE.equals(action) || JMSMessageUtil.CDRActions.ADD.equals(action)
						|| JMSMessageUtil.CDRActions.REORDER.equals(action)){
					message.generateCDRMessageContent();
					if (JMSMessageUtil.CDRActions.MOVE.equals(action) || JMSMessageUtil.CDRActions.ADD.equals(action)){
						//Move and add are both recursive adds of all subjects, plus a nonrecursive update for reordered children.
						for (String pidString: message.getCDRMessageContent().getSubjects()){
							this.offer(new SolrUpdateRequest(pidString, SolrUpdateAction.RECURSIVE_ADD));
						}
					}
					// Reorder is a non-recursive add.
					for (String pidString: message.getCDRMessageContent().getReordered()){
						this.offer(new SolrUpdateRequest(pidString, SolrUpdateAction.ADD));
					}
				} else if (JMSMessageUtil.CDRActions.REINDEX.equals(action)){
					//Determine which kind of reindex to perform based on the mode
					message.generateCDRMessageContent();
					if (message.getCDRMessageContent().getMode().equals("inplace")){
						this.offer(new SolrUpdateRequest(message.getCDRMessageContent().getParent(), SolrUpdateAction.RECURSIVE_REINDEX));
					} else {
						this.offer(new SolrUpdateRequest(message.getCDRMessageContent().getParent(), SolrUpdateAction.CLEAN_REINDEX));
					}
				} else {
					//For all other message types, do a single record update
					this.offer(message.getPIDString());
				}
			}
		}
	}

	@Override
	public String getIdentifier() {
		return identifier;
	}

}
