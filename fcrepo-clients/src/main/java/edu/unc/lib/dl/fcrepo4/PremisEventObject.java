package edu.unc.lib.dl.fcrepo4;

import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.PID;

/**
 * 
 * @author bbpennel
 *
 */
public class PremisEventObject extends RepositoryObject {

	public PremisEventObject(PID pid, Repository repository, RepositoryObjectDataLoader dataLoader) {
		super(pid, repository, dataLoader);
	}

	@Override
	public PremisEventObject validateType() throws FedoraException {
		return this;
	}
}
