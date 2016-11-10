package edu.unc.lib.dl.fcrepo4;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Premis;

/**
 * 
 * @author bbpennel
 *
 */
public class PremisEventObject extends RepositoryObject implements Comparable<PremisEventObject> {
	private static final Logger log = LoggerFactory.getLogger(PremisEventObject.class);

	public PremisEventObject(PID pid, Repository repository, RepositoryObjectDataLoader dataLoader) {
		super(pid, repository, dataLoader);
	}

	@Override
	public PremisEventObject validateType() throws FedoraException {
		return this;
	}

	/**
	 * Default sort order for events is chronological by event date.
	 */
	@Override
	public int compareTo(PremisEventObject o) {
		try {
			String d1 = getResource().getProperty(Premis.hasEventDateTime).getString();
			String d2 = o.getResource().getProperty(Premis.hasEventDateTime).getString();
			return d1.compareTo(d2);
		} catch (FedoraException e) {
			log.error("Failed to parse event date while ordering", e);
			return 0;
		}
	}

	/**
	 * Override to assume that the remote version will not change after creation
	 * for PREMIS events to support offline creation
	 */
	@Override
	public boolean isUnmodified() {
		return true;
	}
}
