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
import org.apache.jena.riot.Lang;
import org.apache.jena.vocabulary.RDF;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;

import com.google.common.cache.CacheLoader;

import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.ObjectTypeMismatchException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.Fcrepo4Repository;
import edu.unc.lib.dl.util.EntityTag;

/**
 * Loader for cache of repository objects
 *
 * @author bbpennel
 *
 */
public class RepositoryObjectCacheLoader extends CacheLoader<PID, RepositoryObject> {

    private RepositoryObjectLoader repoObjLoader;
    private FcrepoClient client;
    private RepositoryObjectDataLoader repositoryObjectDataLoader;
    private RepositoryObjectFactory repoObjFactory;

    protected RepositoryObjectCacheLoader() {
    }

    @Override
    public RepositoryObject load(PID pid) {

        URI metadataUri = RepositoryPaths.getMetadataUri(pid);
        try (FcrepoResponse response = client.get(metadataUri)
                .accept(TURTLE_MIMETYPE)
                .perform()) {

            Model model = ModelFactory.createDefaultModel();
            model.read(response.getBody(), null, Lang.TURTLE.getName());

            String etag = response.getHeaderValue("ETag");
            if (etag != null) {
                etag = new EntityTag(etag).getValue();
            }

            return instantiateRepositoryObject(pid, model, etag);
        } catch (IOException e) {
            throw new FedoraException("Failed to read model for " + pid, e);
        } catch (FcrepoOperationFailedException e) {
            throw ClientFaultResolver.resolve(e);
        }
    }

    /**
     * @param repository the repository to set
     */
    public void setRepositoryObjectLoader(RepositoryObjectLoader repoObjLoader) {
        this.repoObjLoader = repoObjLoader;
    }

    /**
     * @param client the client to set
     */
    public void setClient(FcrepoClient client) {
        this.client = client;
    }

    public void setRepositoryObjectDataLoader(RepositoryObjectDataLoader repositoryObjectDataLoader) {
        this.repositoryObjectDataLoader = repositoryObjectDataLoader;
    }

    private RepositoryObject instantiateRepositoryObject(PID pid, Model model, String etag) {
        RepositoryObject obj = null;

        Resource resc = model.getResource(pid.getRepositoryPath());

        if (isContentPID(pid)) {
            if (resc.hasProperty(RDF.type, Cdr.Work)) {
                obj =  new WorkObject(pid, repoObjLoader, repositoryObjectDataLoader, repoObjFactory);
            } else if (resc.hasProperty(RDF.type, Cdr.FileObject)) {
                obj =  new FileObject(pid, repoObjLoader, repositoryObjectDataLoader, repoObjFactory);
            } else if (resc.hasProperty(RDF.type, Fcrepo4Repository.Binary)) {
                obj =  new BinaryObject(pid, repoObjLoader, repositoryObjectDataLoader, repoObjFactory);
            } else if (resc.hasProperty(RDF.type, Cdr.Folder)) {
                obj =  new FolderObject(pid, repoObjLoader, repositoryObjectDataLoader, repoObjFactory);
            } else if (resc.hasProperty(RDF.type, Cdr.Collection)) {
                obj =  new CollectionObject(pid, repoObjLoader, repositoryObjectDataLoader, repoObjFactory);
            } else if (resc.hasProperty(RDF.type, Cdr.ContentRoot)) {
                obj =  new ContentRootObject(pid, repoObjLoader, repositoryObjectDataLoader, repoObjFactory);
            } else if (resc.hasProperty(RDF.type, Cdr.AdminUnit)) {
                obj =  new AdminUnit(pid, repoObjLoader, repositoryObjectDataLoader, repoObjFactory);
            }
        } else if (isDepositPID(pid)) {
            if (resc.hasProperty(RDF.type, Cdr.DepositRecord)) {
                obj =  new DepositRecord(pid, repoObjLoader, repositoryObjectDataLoader, repoObjFactory);
            } else if (resc.hasProperty(RDF.type, Fcrepo4Repository.Binary)) {
                obj =  new BinaryObject(pid, repoObjLoader, repositoryObjectDataLoader, repoObjFactory);
            }
        }

        if (obj == null) {
            throw new ObjectTypeMismatchException("Requested object " + pid + " is not a repository object.");
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
}
