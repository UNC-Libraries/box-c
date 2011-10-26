package edu.unc.lib.dl.cdr.services.processing;

import edu.unc.lib.dl.cdr.services.model.PIDMessage;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateService;

public class SolrUpdateConductor extends SolrUpdateService implements MessageConductor {
	public static final String identifier = "SOLRUPDATE";

	@Override
	public void add(PIDMessage message) {
		// TODO Auto-generated method stub
		//this.off
	}

	@Override
	public String getIdentifier() {
		return identifier;
	}

}
