package edu.unc.lib.dl.cdr.services;

import java.util.List;
import java.util.Map;

import edu.unc.lib.dl.cdr.services.exception.EnhancementException;
import edu.unc.lib.dl.cdr.services.model.EnhancementApplication;
import edu.unc.lib.dl.cdr.services.model.EnhancementMessage;
import edu.unc.lib.dl.cdr.services.model.FedoraEventMessage;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.JMSMessageUtil;

public abstract class AbstractDatastreamEnhancementService extends AbstractIrodsObjectEnhancementService {

	protected String lastAppliedQuery;
	protected String applicableNoDSQuery;
	protected String applicableStaleDSQuery;
	
	@Override
	public boolean prefilterMessage(EnhancementMessage message) throws EnhancementException {
		String action = message.getQualifiedAction();

		if (JMSMessageUtil.ServicesActions.APPLY_SERVICE_STACK.equals(action))
			return true;
		if (JMSMessageUtil.ServicesActions.APPLY_SERVICE.equals(action))
			return this.getClass().getName().equals(message.getServiceName());

		// If its not a Fedora message at this point, then its not going to match anything else
		if (!(message instanceof FedoraEventMessage))
			return false;

		if (JMSMessageUtil.FedoraActions.INGEST.equals(action))
			return true;
		if (!JMSMessageUtil.FedoraActions.MODIFY_DATASTREAM_BY_REFERENCE.equals(action)
				&& !JMSMessageUtil.FedoraActions.ADD_DATASTREAM.equals(action)
				&& !JMSMessageUtil.FedoraActions.MODIFY_DATASTREAM_BY_VALUE.equals(action))
			return false;
		String datastream = ((FedoraEventMessage) message).getDatastream();

		return ContentModelHelper.Datastream.DATA_FILE.equals(datastream);
	}

	@Override
	public boolean isStale(PID pid) throws EnhancementException {
		return false;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public EnhancementApplication getLastApplied(PID pid) throws EnhancementException {
		// replace model URI and PID tokens
		String query = String.format(this.lastAppliedQuery, this.getTripleStoreQueryService().getResourceIndexModelUri(),
				pid.getURI());

		@SuppressWarnings("unchecked")
		List<Map> bindings = (List<Map>) ((Map) this.getTripleStoreQueryService().sendSPARQL(query).get("results"))
				.get("bindings");
		if (bindings.size() == 0)
			return null;

		EnhancementApplication lastApplied = new EnhancementApplication();
		String lastModified = (String) ((Map) bindings.get(0).get("lastModified")).get("value");
		lastApplied.setLastAppliedFromISO8601(lastModified);
		lastApplied.setPid(pid);
		lastApplied.setEnhancementClass(this.getClass());

		return lastApplied;
	}

}
