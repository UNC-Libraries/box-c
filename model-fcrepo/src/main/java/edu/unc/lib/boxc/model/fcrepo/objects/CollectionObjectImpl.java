package edu.unc.lib.boxc.model.fcrepo.objects;

import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.api.exceptions.FedoraException;
import edu.unc.lib.boxc.model.api.exceptions.ObjectTypeMismatchException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.CollectionObject;
import edu.unc.lib.boxc.model.api.objects.ContentContainerObject;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.FolderObject;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.PcdmModels;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.services.RepositoryObjectDriver;

/**
 * Represents a collection within the repository. This is a second-level container to which
 * folders and works can be added.
 *
 * @author harring
 *
 */
public class CollectionObjectImpl extends AbstractContentContainerObject implements CollectionObject {

    protected CollectionObjectImpl(PID pid, RepositoryObjectDriver driver,
            RepositoryObjectFactory repoObjFactory) {
        super(pid, driver, repoObjFactory);
    }

    @Override
    public CollectionObjectImpl validateType() throws FedoraException {
        if (!isType(Cdr.Collection.getURI())) {
            throw new ObjectTypeMismatchException("Object " + pid + " is not a Collection.");
        }
        if (!isType(PcdmModels.Object.getURI())) {
            throw new ObjectTypeMismatchException("Object " + pid + " is not a PCDM Object.");
        }
        return this;
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.Collection;
    }

    @Override
    public ContentContainerObject addMember(ContentObject member) throws ObjectTypeMismatchException {
        if (!(member instanceof FolderObject || member instanceof WorkObject)) {
            throw new ObjectTypeMismatchException("Cannot add object of type " + member.getClass().getName()
                    + " as a member of CollectionObject " + pid.getQualifiedId());
        }

        repoObjFactory.addMember(this, member);
        member.shouldRefresh();
        return this;
    }

}
