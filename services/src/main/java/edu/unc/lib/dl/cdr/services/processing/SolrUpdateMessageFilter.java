package edu.unc.lib.dl.cdr.services.processing;

import edu.unc.lib.dl.cdr.services.model.PIDMessage;
import edu.unc.lib.dl.cdr.services.util.JMSMessageUtil;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateAction;

public class SolrUpdateMessageFilter extends MessageFilter {
	
	public SolrUpdateMessageFilter(){
		MessageFilter.conductor = SolrUpdateConductor.identifier;
	}
	
	public boolean filter(PIDMessage msg) {
		if (msg.getNamespace() != null && msg.getNamespace().equals(SolrUpdateAction.namespace)){
			return true;
		}
		String action = msg.getAction();
		if (JMSMessageUtil.CDRActions.MOVE.equals(action) || JMSMessageUtil.CDRActions.ADD.equals(action)
				|| JMSMessageUtil.CDRActions.REORDER.equals(action) || JMSMessageUtil.CDRActions.REINDEX.equals(action)
				|| JMSMessageUtil.FedoraActions.PURGE_OBJECT.equals(action)) {
			return true;
		}
		return false;
	}
}
