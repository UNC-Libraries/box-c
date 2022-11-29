package edu.unc.lib.boxc.model.fcrepo.objects;

import static edu.unc.lib.boxc.model.api.rdf.RDFModelUtil.TURTLE_MIMETYPE;

import java.io.IOException;
import java.net.URI;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheLoader;

import edu.unc.lib.boxc.common.http.EntityTag;
import edu.unc.lib.boxc.fcrepo.utils.ClientFaultResolver;
import edu.unc.lib.boxc.model.api.exceptions.FedoraException;
import edu.unc.lib.boxc.model.api.exceptions.ObjectTypeMismatchException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.Ldp;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.services.RepositoryObjectDriver;

/**
 * Loader for cache of repository objects
 *
 * @author bbpennel
 *
 */
public class RepositoryObjectCacheLoader extends CacheLoader<PID, RepositoryObject> {

    private static final Logger log = LoggerFactory.getLogger(RepositoryObjectCacheLoader.class);

    private static final URI BINARY_TYPE_URI = URI.create(Ldp.NonRdfSource.getURI());

    private FcrepoClient client;
    private RepositoryObjectDriver repositoryObjectDriver;
    private RepositoryObjectFactory repoObjFactory;

    public RepositoryObjectCacheLoader() {
    }

    @Override
    public RepositoryObject load(PID pid) {
        long start = System.nanoTime();
        try {
            return loadObject(pid);
        } finally {
            log.debug("Loaded repository object {} in {}s", pid, (System.nanoTime() - start) / 1e9);
        }
    }

    public RepositoryObject loadObject(PID pid) {
        String etag;
        try (FcrepoResponse response = client.head(pid.getRepositoryUri())
                .perform()) {

            boolean isBinary = response.hasType(BINARY_TYPE_URI);
            etag = response.getHeaderValue("ETag");
            if (etag != null) {
                etag = new EntityTag(etag).getValue();
            }

            // For binaries, pull out location of content and immediately instantiate binary obj
            if (isBinary) {
                log.debug("Loading object for binary {}", pid);
                String contentLoc = response.getHeaderValue("Content-Location");
                URI contentUri = null;
                if (contentLoc != null) {
                    contentUri = URI.create(contentLoc);
                }
                return instantiateBinaryObject(pid, contentUri, etag);
            }
        } catch (IOException e) {
            throw new FedoraException("Failed to read model for " + pid, e);
        } catch (FcrepoOperationFailedException e) {
            throw ClientFaultResolver.resolve(e);
        }

        log.debug("Loading object for RDF resource {}", pid);
        // For non-binaries, retrieve the metadata body before instantiation
        URI metadataUri = pid.getRepositoryUri();

        Model model;
        try (FcrepoResponse modelResp = client.get(metadataUri)
                .accept(TURTLE_MIMETYPE)
                .perform()) {

            model = ModelFactory.createDefaultModel();
            model.read(modelResp.getBody(), null, "TURTLE");
        } catch (IOException e) {
            throw new FedoraException("Failed to read model for " + pid, e);
        } catch (FcrepoOperationFailedException e) {
            throw ClientFaultResolver.resolve(e);
        }

        return instantiateRepositoryObject(pid, model, etag);
    }

    /**
     * @param client the client to set
     */
    public void setClient(FcrepoClient client) {
        this.client = client;
    }

    /**
     * @param repositoryObjectDriver the repository object data loader to set
     */
    public void setRepositoryObjectDriver(RepositoryObjectDriver repositoryObjectDriver) {
        this.repositoryObjectDriver = repositoryObjectDriver;
    }

    /**
     * @param repoObjFactory the repository object data loader to set
     */
    public void setRepositoryObjectFactory(RepositoryObjectFactory repoObjFactory) {
        this.repoObjFactory = repoObjFactory;
    }

    private BinaryObjectImpl instantiateBinaryObject(PID pid, URI contentUri, String etag) {
        BinaryObjectImpl obj = new BinaryObjectImpl(pid, contentUri, repositoryObjectDriver, repoObjFactory);
        obj.setEtag(etag);
        return obj;
    }

    private RepositoryObject instantiateRepositoryObject(PID pid, Model model, String etag) {
        RepositoryObject obj = null;

        Resource resc = model.getResource(pid.getRepositoryPath());

        if (isContentPID(pid)) {
            if (resc.hasProperty(RDF.type, Cdr.Tombstone)) {
                obj = new TombstoneImpl(pid, repositoryObjectDriver, repoObjFactory);
            } else if (resc.hasProperty(RDF.type, Cdr.Work)) {
                obj = new WorkObjectImpl(pid, repositoryObjectDriver, repoObjFactory);
            } else if (resc.hasProperty(RDF.type, Cdr.FileObject)) {
                obj = new FileObjectImpl(pid, repositoryObjectDriver, repoObjFactory);
            } else if (resc.hasProperty(RDF.type, Ldp.NonRdfSource)) {
                obj = new BinaryObjectImpl(pid, repositoryObjectDriver, repoObjFactory);
            } else if (resc.hasProperty(RDF.type, Cdr.Folder)) {
                obj = new FolderObjectImpl(pid, repositoryObjectDriver, repoObjFactory);
            } else if (resc.hasProperty(RDF.type, Cdr.Collection)) {
                obj = new CollectionObjectImpl(pid, repositoryObjectDriver, repoObjFactory);
            } else if (resc.hasProperty(RDF.type, Cdr.ContentRoot)) {
                obj = new ContentRootObjectImpl(pid, repositoryObjectDriver, repoObjFactory);
            } else if (resc.hasProperty(RDF.type, Cdr.AdminUnit)) {
                obj = new AdminUnitImpl(pid, repositoryObjectDriver, repoObjFactory);
            }
        } else if (isDepositPID(pid)) {
            if (resc.hasProperty(RDF.type, Cdr.DepositRecord)) {
                obj = new DepositRecordImpl(pid, repositoryObjectDriver, repoObjFactory);
            } else if (resc.hasProperty(RDF.type, Ldp.NonRdfSource)) {
                obj = new BinaryObjectImpl(pid, repositoryObjectDriver, repoObjFactory);
            }
        }

        if (obj == null) {
            StringBuilder types = new StringBuilder();
            StmtIterator typesIt = resc.listProperties(RDF.type);
            while (typesIt.hasNext()) {
                types.append(typesIt.nextStatement().getResource().getURI()).append("\n");
            }
            throw new ObjectTypeMismatchException("Requested object " + pid + " is not a repository object."
                    + "\nHad types:\n" + types.toString());
        }

        obj.setEtag(etag);
        obj.storeModel(model);

        return obj;
    }

    private boolean isContentPID(PID pid) {
        return pid.getQualifier().equals(RepositoryPathConstants.CONTENT_BASE);
    }

    private boolean isDepositPID(PID pid) {
        return pid.getQualifier().equals(RepositoryPathConstants.DEPOSIT_RECORD_BASE);
    }

    /**
     * @param repoObjFactory the repoObjFactory to set
     */
    public void setRepoObjFactory(RepositoryObjectFactory repoObjFactory) {
        this.repoObjFactory = repoObjFactory;
    }
}
