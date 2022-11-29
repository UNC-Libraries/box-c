package edu.unc.lib.boxc.model.fcrepo.objects;

import edu.unc.lib.boxc.model.api.exceptions.FedoraException;
import edu.unc.lib.boxc.model.api.exceptions.ObjectTypeMismatchException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.Tombstone;
import edu.unc.lib.boxc.model.api.rdf.Fcrepo4Repository;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.services.RepositoryObjectDriver;

/**
 * Represents a tombstone object within the repository
 *
 * @author harring
 *
 */
public class TombstoneImpl extends AbstractContentObject implements Tombstone {

    public TombstoneImpl(PID pid, RepositoryObjectDriver driver, RepositoryObjectFactory repoObjFactory) {
        super(pid, driver, repoObjFactory);
    }

    @Override
    public RepositoryObject validateType() throws FedoraException {
        if (!isType(Fcrepo4Repository.Tombstone.toString())) {
            throw new ObjectTypeMismatchException("Object " + pid + " is not a tombstone");
        }
        return this;
    }

    @Override
    public RepositoryObject getParent() {
        // tombstone is not in the hierarchy, so doesn't have a parent
        return null;
    }

    @Override
    public PID getParentPid() {
        return null;
    }
}
