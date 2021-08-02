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
package edu.unc.lib.boxc.operations.impl.destroy;

import static edu.unc.lib.boxc.model.api.rdf.RDFModelUtil.TURTLE_MIMETYPE;
import static edu.unc.lib.boxc.operations.jms.destroy.DestroyObjectsHelper.assertCanDestroy;
import static edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType.DELETE_SOLR_TREE;
import static org.springframework.http.HttpHeaders.CONTENT_LOCATION;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.riot.Lang;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.model.api.exceptions.FedoraException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.ContentContainerObject;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.FolderObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.rdf.Ldp;
import edu.unc.lib.boxc.operations.jms.destroy.DestroyObjectsRequest;
import edu.unc.lib.dl.fcrepo4.ClientFaultResolver;
import edu.unc.lib.dl.fedora.ServiceException;

/**
 * A job for destroying objects from the repository, which does not leave behind
 * tombstones and cleans up all metadata files.
 *
 * @author bbpennel
 */
public class DestroyObjectsCompletelyJob extends AbstractDestroyObjectsJob {
    private static final Logger log = LoggerFactory.getLogger(DestroyObjectsCompletelyJob.class);

    private static final URI BINARY_TYPE_URI = URI.create(Ldp.NonRdfSource.getURI());

    public DestroyObjectsCompletelyJob(DestroyObjectsRequest request) {
        super(request);
    }

    @Override
    public void run() {
        for (PID pid : objsToDestroy) {
            RepositoryObject repoObj = repoObjLoader.getRepositoryObject(pid);
            destroyTree(repoObj);

            indexingMessageSender.sendIndexingOperation(agent.getUsername(), pid, DELETE_SOLR_TREE);
        }

        // Defer binary cleanup until after fedora destroy transaction completes
        destroyBinaries();
    }

    private void destroyTree(RepositoryObject rootOfTree) {
        if (!(rootOfTree instanceof WorkObject || rootOfTree instanceof FileObject
                || rootOfTree instanceof FolderObject)) {
            throw new ServiceException("Refusing to destroy object " + rootOfTree.getPid()
                    + " of type " + rootOfTree.getResourceType());
        }

        assertCanDestroy(agent, rootOfTree, aclService);

        log.info("Completely destroying object {}", rootOfTree.getPid());
        if (rootOfTree instanceof ContentContainerObject) {
            ContentContainerObject container = (ContentContainerObject) rootOfTree;
            List<ContentObject> members = container.getMembers();

            for (ContentObject member : members) {
                destroyTree(member);
            }
        }

        List<URI> binaryUris = new ArrayList<>();
        addBinariesForCleanup(rootOfTree.getModel(true), binaryUris);
        cleanupBinaryUris.addAll(binaryUris);

        sendBinariesDestroyedMsg(rootOfTree, binaryUris);

        purgeObject(rootOfTree.getPid().getRepositoryPath());
    }

    private void addBinariesForCleanup(Model model, List<URI> uris) {
        NodeIterator iter = model.listObjectsOfProperty(Ldp.contains);
        while (iter.hasNext()) {
            RDFNode obj = iter.next();
            String objPath = obj.asResource().getURI();
            URI objUri = URI.create(objPath);

            try (FcrepoResponse resp = fcrepoClient.head(objUri).perform()) {
                if (resp.hasType(BINARY_TYPE_URI)) {
                    String contentLoc = resp.getHeaderValue(CONTENT_LOCATION);
                    uris.add(URI.create(contentLoc));
                    continue;
                }
            } catch (IOException e) {
                throw new FedoraException("Failed to make HEAD request for " + objUri, e);
            } catch (FcrepoOperationFailedException e) {
                throw ClientFaultResolver.resolve(e);
            }

            try (FcrepoResponse resp = fcrepoClient.get(objUri)
                    .accept(TURTLE_MIMETYPE)
                    .perform()) {

                Model childModel = ModelFactory.createDefaultModel();
                childModel.read(resp.getBody(), null, Lang.TURTLE.getName());
                addBinariesForCleanup(childModel, uris);
            } catch (IOException e) {
                throw new FedoraException("Failed to read model for " + objUri, e);
            } catch (FcrepoOperationFailedException e) {
                throw ClientFaultResolver.resolve(e);
            }
        }
    }
}
