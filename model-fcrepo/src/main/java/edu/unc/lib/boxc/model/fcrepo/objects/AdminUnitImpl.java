package edu.unc.lib.boxc.model.fcrepo.objects;

import edu.unc.lib.boxc.model.api.exceptions.FedoraException;
import edu.unc.lib.boxc.model.api.exceptions.ObjectTypeMismatchException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.AdminUnit;
import edu.unc.lib.boxc.model.api.objects.CollectionObject;
import edu.unc.lib.boxc.model.api.objects.ContentContainerObject;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.PcdmModels;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.services.RepositoryObjectDriver;

/**
 * Represents the highest-level container in the repository.
 *
 * @author harring
 *
 */
public class AdminUnitImpl extends AbstractContentContainerObject implements AdminUnit {

    protected AdminUnitImpl(PID pid, RepositoryObjectDriver driver,
            RepositoryObjectFactory repoObjFactory) {
        super(pid, driver, repoObjFactory);
    }

    @Override
    public ContentContainerObject addMember(ContentObject member) throws ObjectTypeMismatchException {
        if (!(member instanceof CollectionObject)) {
            throw new ObjectTypeMismatchException("Cannot add object of type " + member.getClass().getName()
                    + " as a member of AdminUnit " + pid.getQualifiedId());
        }
        repoObjFactory.addMember(this, member);
        member.shouldRefresh();
        return this;
    }

    @Override
    public AdminUnit validateType() throws FedoraException {
        if (!isType(Cdr.AdminUnit.getURI())) {
            throw new ObjectTypeMismatchException("Object " + pid + " is not an AdminUnit.");
        }
        if (!isType(PcdmModels.Collection.getURI())) {
            throw new ObjectTypeMismatchException("Object " + pid + " is not a PCDM Collection.");
        }
        return this;
    }

}
