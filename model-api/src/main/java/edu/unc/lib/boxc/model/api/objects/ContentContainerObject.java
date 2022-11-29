package edu.unc.lib.boxc.model.api.objects;

import java.util.List;

import edu.unc.lib.boxc.model.api.exceptions.ObjectTypeMismatchException;


/**
 * Represents a repository object within the main content tree which can
 * contain other ContentObjects.
 * @author bbpennel
 */
public interface ContentContainerObject extends ContentObject {

    /**
     * Add a ContentObject as a member to this container
     *
     * @param member
     * @return this ContentContainerObject
     * @throws ObjectTypeMismatchException
     *             Thrown if the new member is not of a type supported by this
     *             container
     */
    ContentContainerObject addMember(ContentObject member) throws ObjectTypeMismatchException;

    /**
     * Retrieve a list of member content objects for this object.
     *
     * @return
     */
    List<ContentObject> getMembers();

}