package edu.unc.lib.boxc.model.fcrepo.objects;

import java.net.URI;
import java.util.Date;
import java.util.List;

import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.api.event.PremisLog;
import edu.unc.lib.boxc.model.api.exceptions.FedoraException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.rdf.Fcrepo4Repository;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.services.RepositoryObjectDriver;

/**
 * A generic repository object, with properties common to objects in the repository.
 *
 * @author bbpennel
 * @author harring
 *
 */
public abstract class AbstractRepositoryObject implements RepositoryObject {

    // Loader for lazy loading data about this object when requested
    protected RepositoryObjectDriver driver;
    protected RepositoryObjectFactory repoObjFactory;

    // The identifier and path information for this object
    protected PID pid;

    protected Model model;

    protected Date lastModified;
    protected Date created;
    protected String etag;

    protected List<String> types;

    protected PremisLog premisLog;

    protected AbstractRepositoryObject(PID pid, RepositoryObjectDriver driver,
            RepositoryObjectFactory repoObjFactory) {
        this.pid = pid;
        this.driver = driver;
        this.repoObjFactory = repoObjFactory;
    }

    /**
     * Get the URI of this object in the repository
     *
     * @return
     */
    @Override
    public URI getUri() {
        return pid.getRepositoryUri();
    }

    /**
     * Get a model containing properties and relations held by this object.
     *
     * @return
     * @throws FedoraException
     */
    @Override
    public Model getModel() throws FedoraException {
        driver.loadModel(this, false);
        return model;
    }

    @Override
    public Model getModel(boolean checkForUpdates) throws FedoraException {
        driver.loadModel(this, checkForUpdates);
        return model;
    }

    /**
     * Return true if the model for this object has been populated
     *
     * @return
     */
    @Override
    public boolean hasModel() {
        return model != null;
    }

    /**
     * Store the relationships and properties belonging to this object
     *
     * @param model
     * @return
     */
    @Override
    public void storeModel(Model model) {
        this.model = model;
        // Clear the cached types list
        this.types = null;
    }

    @Override
    public Resource getResource() throws FedoraException {
        return getResource(false);
    }

    @Override
    public Resource getResource(boolean checkForUpdates) throws FedoraException {
        return getModel(checkForUpdates).getResource(getUri().toString());
    }

    /**
     * Get the PREMIS event log for this object
     *
     * @return
     */
    @Override
    public PremisLog getPremisLog() {
        if (premisLog == null) {
            premisLog = driver.getPremisLog(this);
        }
        return premisLog;
    }

    /**
     * @return the ResourceType which describes this object
     */
    @Override
    public ResourceType getResourceType() {
        return ResourceType.getResourceTypeForUris(getTypes());
    }

    /**
     * Throws a Fedora exception if the object does not match the expected RDF types
     *
     * @return
     * @throws FedoraException
     */
    @Override
    public abstract RepositoryObject validateType() throws FedoraException;

    /**
     * Get the PID of this object
     *
     * @return
     */
    @Override
    public PID getPid() {
        return pid;
    }

    /**
     * Set the PID
     *
     * @param pid
     */
    public void setPid(PID pid) {
        this.pid = pid;
    }

    /**
     * Get the parent of the current object
     * @return
     */
    @Override
    public abstract RepositoryObject getParent();

    /**
     * @return pid of the parent of the current object
     */
    @Override
    public PID getParentPid() {
        return driver.getParentPid(this);
    }

    /**
     * Get the last modified date
     *
     * @return
     */
    @Override
    public Date getLastModified() {
        if (lastModified == null) {
            lastModified = ((XSDDateTime) getResource().getProperty(Fcrepo4Repository.lastModified).getLiteral()
                    .getValue()).asCalendar().getTime();
        }
        return lastModified;
    }

    /**
     * Set the last modified date
     *
     * @param lastModified
     */
    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    /**
     * Get the creation date for this object
     * @return
     */
    @Override
    public Date getCreatedDate() {
        if (created == null) {
            created = ((XSDDateTime) getResource().getProperty(Fcrepo4Repository.created)
                    .getLiteral().getValue()).asCalendar().getTime();
        }
        return created;
    }

    /**
     * Get the ETag representing the version of the object retrieved
     *
     * @return
     */
    @Override
    public String getEtag() {
        return etag;
    }

    /**
     * Set the etag
     *
     * @param etag
     */
    @Override
    public void setEtag(String etag) {
        this.etag = etag;
    }

    /**
     * Get a list of the RDF types for this object
     *
     * @return
     * @throws FedoraException
     */
    @Override
    public List<String> getTypes() throws FedoraException {
        if (types == null) {
            driver.loadTypes(this);
        }
        return types;
    }

    /**
     * Set the list of RDF types
     *
     * @param types
     */
    public void setTypes(List<String> types) {
        this.types = types;
    }

    /**
     * The URI where RDF metadata about this object can be retrieved from.
     *
     * @return
     */
    @Override
    public URI getMetadataUri() {
        return pid.getRepositoryUri();
    }

    /**
     * Returns true if this object  is unmodified according to etag by
     * verifying if the locally held etag matches the current one in the
     * repository
     *
     * @return
     */
    @Override
    public boolean isUnmodified() {
        if (getEtag() == null) {
            return false;
        }

        String remoteEtag = driver.getEtag(this);
        if (remoteEtag == null) {
            return false;
        }
        return remoteEtag.equals(getEtag());
    }

    /**
     * Indicate that the state of this object should be refreshed
     */
    @Override
    public void shouldRefresh() {
        model = null;
        premisLog = null;
        types = null;
        etag = null;
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof AbstractRepositoryObject) {
            RepositoryObject repoObj = (RepositoryObject) object;
            return repoObj.getPid().equals(pid);
        } else {
            return false;
        }
    }
}
