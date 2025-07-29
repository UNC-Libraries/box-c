package edu.unc.lib.boxc.model.api.ids;

import java.net.URI;

/**
 * A persistent identifier for a repository resource
 * @author bbpennel
 *
 */
public interface PID {
    /**
     * @return String representation of the PID
     */
    @Deprecated
    String getPid();

    /**
     * @return URI representation of the persistent identifer
     */
    @Deprecated
    String getURI();

    /**
     * @return
     */
    @Deprecated
    String getPath();

    /**
     * @return The UUID portion of this PID
     */
    String getUUID();

    /**
     * Get the unique identifier for this object.
     *
     * @return the unique identifier for this object
     */
    String getId();

    /**
     * Get the object type path qualifier for this object.
     *
     * @return the object type path qualifier for this object.
     */
    String getQualifier();

    /**
     * Get the qualified unique identifier for this object, containing the
     * formatted qualifier and id.
     *
     * @return the qualified id
     */
    String getQualifiedId();

    /**
     * Get the component path, which is the portion of the repository path identifying
     * a specific component of the digital object
     *
     * @return the component path
     */
    String getComponentPath();

    /**
     * Get the unique identifier for this object, including the component path if present
     *
     * @return
     */
    String getComponentId();

    /**
     * Returns true if the provided pid is a component of the this pid
     *
     * @param pid
     * @return
     */
    boolean containsComponent(PID pid);

    /**
     * Returns the full repository uri for this object or component.
     *
     * @return
     */
    URI getRepositoryUri();

    /**
     * Returns the repository URI for this pid as a string
     *
     * @return
     */
    String getRepositoryPath();
}

