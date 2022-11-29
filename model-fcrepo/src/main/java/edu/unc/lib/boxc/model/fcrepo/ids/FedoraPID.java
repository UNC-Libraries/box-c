package edu.unc.lib.boxc.model.fcrepo.ids;

import java.io.Serializable;
import java.net.URI;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants;

/**
 * Persistent identifier for a fedora repository object.  Provides both externally
 * facing URI information as well as the internal Fedora repository URI.
 *
 * @author bbpennel
 *
 */
public class FedoraPID implements PID, Serializable {
    private static final long serialVersionUID = 7627752253405069756L;

    private String id;
    private String qualifier;
    private String qualifiedId;
    private String componentId;
    private String componentPath;
    private URI repositoryUri;
    private String repositoryPath;

    public FedoraPID(String id, String qualifier, String componentPath, URI repositoryUri) {
        this.id = id;
        this.qualifier = qualifier;
        this.componentPath = componentPath;
        this.repositoryUri = repositoryUri;
        this.repositoryPath = repositoryUri.toString();
        this.qualifiedId = qualifier + "/" + id;
        if (componentPath != null) {
            this.qualifiedId += "/" + componentPath;
            this.componentId = id + "/" + componentPath;
        } else {
            this.componentId = id;
        }
    }

    /**
     * Get the unique identifier for this object.
     *
     * @return the unique identifier for this object
     */
    @Override
    public String getId() {
        return id;
    }

    /**
     * Get the object type path qualifier for this object.
     *
     * @return the object type path qualifier for this object.
     */
    @Override
    public String getQualifier() {
        return qualifier;
    }

    /**
     * Get the qualified unique identifier for this object, containing the
     * formatted qualifier and id.
     *
     * @return the qualified id
     */
    @Override
    public String getQualifiedId() {
        return qualifiedId;
    }

    /**
     * Get the component path, which is the portion of the repository path identifying
     * a specific component of the digital object
     *
     * @return the component path
     */
    @Override
    public String getComponentPath() {
        return componentPath;
    }

    @Override
    public String getComponentId() {
        return componentId;
    }

    /**
     * The passed in PID is a component of this pid if its repository path contains
     * this object but is not the same path.
     */
    @Override
    public boolean containsComponent(PID pid) {
        return pid.getRepositoryPath().startsWith(repositoryPath) &&
                !repositoryPath.equals(pid.getRepositoryPath());
    }

    @Override
    public String getURI() {
        return getRepositoryPath();
    }

    /**
     * Get the repository uri for this object or component, which is the full URI of the object in Fedora
     *
     * @return
     */
    @Override
    public URI getRepositoryUri() {
        return repositoryUri;
    }

    @Override
    public String getRepositoryPath() {
        return repositoryPath;
    }

    /**
     * Get the persistent identifier for this object
     */
    @Override
    public String getPid() {
        // Special case for content paths for legacy purposes
        if (RepositoryPathConstants.CONTENT_BASE.equals(qualifier)) {
            if (componentPath == null) {
                return "uuid:" + id;
            }
            return "uuid:" + id + "/" + componentPath;
        }
        return getQualifiedId();
    }

    @Override
    public String getUUID() {
        return getId();
    }

    @Override
    public String toString() {
        return getRepositoryPath();
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof PID) {
            PID other = (PID) object;
            return repositoryPath.equals(other.getRepositoryPath());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return repositoryPath.hashCode();
    }

    @Override
    public String getPath() {
        return null;
    }
}
