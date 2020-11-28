/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.fcrepo4;

import static edu.unc.lib.dl.util.RDFModelUtil.TURTLE_MIMETYPE;

import java.io.IOException;
import java.net.URI;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.Lang;
import org.apache.jena.vocabulary.RDF;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheLoader;

import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.ObjectTypeMismatchException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.Ldp;
import edu.unc.lib.dl.util.EntityTag;

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

    protected RepositoryObjectCacheLoader() {
    }

    @Override
    public RepositoryObject load(PID pid) {
        log.debug("Loading repository object {} for cache", pid);

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
            model.read(modelResp.getBody(), null, Lang.TURTLE.getName());
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

    private BinaryObject instantiateBinaryObject(PID pid, URI contentUri, String etag) {
        BinaryObject obj = new BinaryObject(pid, contentUri, repositoryObjectDriver, repoObjFactory);
        obj.setEtag(etag);
        return obj;
    }

    private RepositoryObject instantiateRepositoryObject(PID pid, Model model, String etag) {
        RepositoryObject obj = null;

        Resource resc = model.getResource(pid.getRepositoryPath());

        if (isContentPID(pid)) {
            if (resc.hasProperty(RDF.type, Cdr.Tombstone)) {
                obj = new Tombstone(pid, repositoryObjectDriver, repoObjFactory);
            } else if (resc.hasProperty(RDF.type, Cdr.Work)) {
                obj = new WorkObject(pid, repositoryObjectDriver, repoObjFactory);
            } else if (resc.hasProperty(RDF.type, Cdr.FileObject)) {
                obj = new FileObject(pid, repositoryObjectDriver, repoObjFactory);
            } else if (resc.hasProperty(RDF.type, Ldp.NonRdfSource)) {
                obj = new BinaryObject(pid, repositoryObjectDriver, repoObjFactory);
            } else if (resc.hasProperty(RDF.type, Cdr.Folder)) {
                obj = new FolderObject(pid, repositoryObjectDriver, repoObjFactory);
            } else if (resc.hasProperty(RDF.type, Cdr.Collection)) {
                obj = new CollectionObject(pid, repositoryObjectDriver, repoObjFactory);
            } else if (resc.hasProperty(RDF.type, Cdr.ContentRoot)) {
                obj = new ContentRootObject(pid, repositoryObjectDriver, repoObjFactory);
            } else if (resc.hasProperty(RDF.type, Cdr.AdminUnit)) {
                obj = new AdminUnit(pid, repositoryObjectDriver, repoObjFactory);
            }
        } else if (isDepositPID(pid)) {
            if (resc.hasProperty(RDF.type, Cdr.DepositRecord)) {
                obj = new DepositRecord(pid, repositoryObjectDriver, repoObjFactory);
            } else if (resc.hasProperty(RDF.type, Ldp.NonRdfSource)) {
                obj = new BinaryObject(pid, repositoryObjectDriver, repoObjFactory);
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
