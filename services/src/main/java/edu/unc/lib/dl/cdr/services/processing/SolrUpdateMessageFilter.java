package edu.unc.lib.dl.cdr.services.processing;

import edu.unc.lib.dl.cdr.services.model.PIDMessage;

public class SolrUpdateMessageFilter extends MessageFilter {
	public boolean filter(PIDMessage msg) {
		return false;
	}
}
