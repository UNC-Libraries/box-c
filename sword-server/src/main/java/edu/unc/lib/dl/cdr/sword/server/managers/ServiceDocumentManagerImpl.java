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
package edu.unc.lib.dl.cdr.sword.server.managers;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swordapp.server.AuthCredentials;
import org.swordapp.server.ServiceDocument;
import org.swordapp.server.ServiceDocumentManager;
import org.swordapp.server.SwordAuthException;
import org.swordapp.server.SwordCollection;
import org.swordapp.server.SwordConfiguration;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;
import org.swordapp.server.SwordWorkspace;

import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.cdr.sword.server.SwordConfigurationImpl;
import edu.unc.lib.dl.cdr.sword.server.deposit.DepositHandler;
import edu.unc.lib.dl.fcrepo4.RepositoryPaths;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ErrorURIRegistry;
import edu.unc.lib.dl.util.PackagingType;

/**
 * Generates service document from all containers which are the immediate children of the starting path, given the users
 * authorization credentials.
 *
 * @author bbpennel
 */
public class ServiceDocumentManagerImpl extends AbstractFedoraManager implements ServiceDocumentManager {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceDocumentManagerImpl.class);

    private Collection<PackagingType> acceptedPackaging;

    @Override
    public ServiceDocument getServiceDocument(String sdUri, AuthCredentials auth, SwordConfiguration config)
            throws SwordError, SwordServerException, SwordAuthException {

        ServiceDocument sd = new ServiceDocument();
        SwordWorkspace workspace = new SwordWorkspace();
        SwordConfigurationImpl configImpl = (SwordConfigurationImpl) config;

        sd.setVersion(configImpl.getSwordVersion());
        if (config.getMaxUploadSize() != -1) {
            sd.setMaxUploadSize(config.getMaxUploadSize());
        }

        String pidString = null;
        PID pid = null;
        if (sdUri != null) {
            try {
                pidString = sdUri.substring(sdUri.lastIndexOf("/") + 1);
                pid = new PID(pidString);
            } catch (IndexOutOfBoundsException e) {
                // Ignore, if there is no trailing / then no pid is set.
            }
        }
        if (pidString == null || "".equals(pidString.trim())) {
            pid = RepositoryPaths.getContentRootPid();
        }

        if (!hasAccess(auth, pid, Permission.viewDescription, configImpl)) {
            LOG.debug("Insufficient privileges to access the service document for " + pid.getPid());
            throw new SwordError(ErrorURIRegistry.INSUFFICIENT_PRIVILEGES, 403,
                    "Insufficient privileges to access the service document for " + pid.getPid());
        }

        LOG.debug("Retrieving service document for " + pid);

        List<SwordCollection> collections;
        try {
            collections = this.getImmediateContainerChildren(pid, auth, configImpl);
            for (SwordCollection collection : collections) {
                workspace.addCollection(collection);
            }
            sd.addWorkspace(workspace);

            return sd;
        } catch (Exception e) {
            LOG.error("An exception occurred while generating the service document for " + pid, e);
            throw new SwordError(ErrorURIRegistry.RETRIEVAL_EXCEPTION, 500,
                    "An unexpected exception occurred while retrieving service document.");
        }
    }

    /**
     * Retrieves a list of SwordCollection objects representing all the children containers of container pid which the
     * groups in groupList have curator access to.
     *
     * @param pid
     *           pid of the container to retrieve the children of.
     * @param groupList
     *           list of permission groups
     * @param config
     * @return
     * @throws IOException
     */
    protected List<SwordCollection> getImmediateContainerChildren(PID pid, AuthCredentials auth,
            SwordConfigurationImpl config) throws IOException {
//        String query = this.readFileAsString("immediateContainerChildren.sparql");
//        query = String.format(query, tripleStoreQueryService.getResourceIndexModelUri(), pid.getURI());
//        List<SwordCollection> result = new ArrayList<>();
//
//        @SuppressWarnings({ "rawtypes", "unchecked" })
//        List<Map> bindings = (List<Map>) ((Map) tripleStoreQueryService.sendSPARQL(query)
//                .get("results")).get("bindings");
//        for (Map<?, ?> binding : bindings) {
//            SwordCollection collection = new SwordCollection();
//            PID containerPID = new PID((String) ((Map<?, ?>) binding.get("pid")).get("value"));
//            String slug = (String) ((Map<?, ?>) binding.get("slug")).get("value");
//
//            // Check that the user has curator access to this collection
//            if (hasAccess(auth, containerPID, Permission.addRemoveContents, config)) {
//                collection.setHref(config.getSwordPath() + SwordConfigurationImpl.COLLECTION_PATH + "/"
//                        + containerPID.getPid());
//                collection.setTitle(slug);
//                collection.addAccepts("application/zip");
//                collection.addAccepts("text/xml");
//                collection.addAccepts("application/xml");
//                for (PackagingType packaging : acceptedPackaging) {
//                    collection.addAcceptPackaging(packaging.getUri());
//                }
//                collection.setMediation(true);
//                //
//                IRI iri = new IRI(config.getSwordPath() + SwordConfigurationImpl.SERVICE_DOCUMENT_PATH + "/"
//                        + containerPID.getPid());
//                collection.addSubService(iri);
//                result.add(collection);
//            }
//        }
//        return result;
        return null;
    }

    public void setAcceptedPackaging(Map<PackagingType, DepositHandler> packageTypeHandlers) {
        this.acceptedPackaging = packageTypeHandlers.keySet();
    }

}
