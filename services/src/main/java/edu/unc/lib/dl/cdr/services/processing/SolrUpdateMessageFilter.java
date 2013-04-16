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

import edu.unc.lib.dl.cdr.services.model.FedoraEventMessage;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.message.ActionMessage;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.JMSMessageUtil;

public class SolrUpdateMessageFilter implements MessageFilter {
	
	public SolrUpdateMessageFilter(){
	}
	
	@Override
	public String getConductor(){
		return SolrUpdateConductor.identifier;
	}
	
	@Override
	public boolean filter(ActionMessage message) {
		if (message == null)
			return false;
		
		if (message instanceof SolrUpdateRequest)
			return true;
		
		String action = message.getQualifiedAction();
		if (JMSMessageUtil.CDRActions.MOVE.equals(action) || JMSMessageUtil.CDRActions.ADD.equals(action)
				|| JMSMessageUtil.CDRActions.REORDER.equals(action) || JMSMessageUtil.CDRActions.REINDEX.equals(action)
				|| JMSMessageUtil.CDRActions.PUBLISH.equals(action) || JMSMessageUtil.CDRActions.INDEX.equals(action)) {
			return true;
		}
		if (!(message instanceof FedoraEventMessage))
			return false;
		if (JMSMessageUtil.FedoraActions.PURGE_OBJECT.equals(action))
			return true;
		String datastream = ((FedoraEventMessage)message).getDatastream();
		return ContentModelHelper.Datastream.MD_DESCRIPTIVE.equals(datastream)
				&& (JMSMessageUtil.FedoraActions.MODIFY_DATASTREAM_BY_REFERENCE.equals(action)
				|| JMSMessageUtil.FedoraActions.MODIFY_DATASTREAM_BY_VALUE.equals(action)
				|| JMSMessageUtil.FedoraActions.PURGE_DATASTREAM.equals(action)
				|| JMSMessageUtil.FedoraActions.ADD_DATASTREAM.equals(action));
	}
}
