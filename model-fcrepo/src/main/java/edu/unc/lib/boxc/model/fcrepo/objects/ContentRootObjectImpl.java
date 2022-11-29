package edu.unc.lib.boxc.model.fcrepo.objects;

import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.api.exceptions.FedoraException;
import edu.unc.lib.boxc.model.api.exceptions.ObjectTypeMismatchException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.AdminUnit;
import edu.unc.lib.boxc.model.api.objects.ContentContainerObject;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.ContentRootObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.services.RepositoryObjectDriver;

/**
 * A repository object which represents the root of the content tree. Can only
 * contain AdminUnit objects as direct children.
 *
 * @author bbpennel
 *
 */
public class ContentRootObjectImpl extends AbstractContentContainerObject implements ContentRootObject {

    protected ContentRootObjectImpl(PID pid, RepositoryObjectDriver driver,
            RepositoryObjectFactory repoObjFactory) {
        super(pid, driver, repoObjFactory);
    }

    @Override
    public ContentContainerObject addMember(ContentObject member) throws ObjectTypeMismatchException {
        if (!(member instanceof AdminUnit)) {
            throw new ObjectTypeMismatchException("Cannot add object of type " + member.getClass().getName()
                    + " as a member of ContentRootObject " + pid.getQualifiedId());
        }

        repoObjFactory.addMember(this, member);
        member.shouldRefresh();
        return this;
    }

    @Override
    public RepositoryObject validateType() throws FedoraException {
        if (!isType(Cdr.ContentRoot.toString())) {
            throw new ObjectTypeMismatchException("Object " + pid + " is not the ContentRoot object.");
        }
        return this;
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.ContentRoot;
    }
}
