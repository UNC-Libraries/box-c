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
    public String getPid();

    /**
     * @return URI representation of the persistent identifer
     */
    @Deprecated
    public String getURI();

    /**
     * @return
     */
    @Deprecated
    public String getPath();

    /**
     * @return The UUID portion of this PID
     */
    public String getUUID();

    /**
     * Get the unique identifier for this object.
     *
     * @return the unique identifier for this object
     */
    public String getId();

    /**
     * Get the object type path qualifier for this object.
     *
     * @return the object type path qualifier for this object.
     */
    public String getQualifier();

    /**
     * Get the qualified unique identifier for this object, containing the
     * formatted qualifier and id.
     *
     * @return the qualified id
     */
    public String getQualifiedId();

    /**
     * Get the component path, which is the portion of the repository path identifying
     * a specific component of the digital object
     *
     * @return the component path
     */
    public String getComponentPath();

    /**
     * Get the unique identifier for this object, including the component path if present
     *
     * @return
     */
    public String getComponentId();

    /**
     * Returns true if the provided pid is a component of the this pid
     *
     * @param pid
     * @return
     */
    public boolean containsComponent(PID pid);

    /**
     * Returns the full repository uri for this object or component.
     *
     * @return
     */
    public URI getRepositoryUri();

    /**
     * Returns the repository URI for this pid as a string
     *
     * @return
     */
    public String getRepositoryPath();
}

