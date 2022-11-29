package edu.unc.lib.boxc.model.fcrepo.objects;

import java.util.List;
import java.util.stream.Collectors;

import edu.unc.lib.boxc.model.api.exceptions.ObjectTypeMismatchException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.ContentContainerObject;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.services.RepositoryObjectDriver;

/**
 * Represents a generic repository object within the main content tree which can
 * contain other ContentObjects.
 *
 * @author bbpennel
 *
 */
public abstract class AbstractContentContainerObject extends AbstractContentObject implements ContentContainerObject {

    protected AbstractContentContainerObject(PID pid, RepositoryObjectDriver driver,
            RepositoryObjectFactory repoObjFactory) {
        super(pid, driver, repoObjFactory);
    }

    /**
     * Add a ContentObject as a member to this container
     *
     * @param member
     * @return this ContentContainerObject
     * @throws ObjectTypeMismatchException
     *             Thrown if the new member is not of a type supported by this
     *             container
     */
    @Override
    public abstract ContentContainerObject addMember(ContentObject member) throws ObjectTypeMismatchException;

    /**
     * Retrieve a list of member content objects for this object.
     *
     * @return
     */
    @Override
    public List<ContentObject> getMembers() {
        return driver.listMembers(this).stream()
                .map(m -> (ContentObject) driver.getRepositoryObject(m))
                .collect(Collectors.toList());
    }
}
