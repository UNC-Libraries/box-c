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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.abdera.i18n.iri.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.swordapp.server.AuthCredentials;
import org.swordapp.server.ServiceDocument;
import org.swordapp.server.ServiceDocumentManager;
import org.swordapp.server.SwordAuthException;
import org.swordapp.server.SwordCollection;
import org.swordapp.server.SwordConfiguration;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;
import org.swordapp.server.SwordWorkspace;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.ContentContainerObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths;
import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.cdr.sword.server.SwordConfigurationImpl;
import edu.unc.lib.dl.persist.api.ingest.DepositHandler;
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
    @Autowired
    private RepositoryObjectLoader repositoryObjectLoader;

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
                pid = PIDs.get(pidString);
            } catch (IndexOutOfBoundsException e) {
                // Ignore, if there is no trailing / then no pid is set.
            }
        }
        if (pidString == null || "".equals(pidString.trim())) {
            pid = RepositoryPaths.getContentRootPid();
        }

        assertHasAccess("Insufficient privileges to access the service document for " + pid.getRepositoryPath(),
                pid, Permission.viewMetadata);

        LOG.debug("Retrieving service document for {}", pid);

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
        RepositoryObject repoObj = repositoryObjectLoader.getRepositoryObject(pid);
        ContentContainerObject containerObj;
        if (repoObj instanceof ContentContainerObject) {
            containerObj = (ContentContainerObject) repoObj;
        } else {
            return Collections.emptyList();
        }

        AgentPrincipals agent = AgentPrincipals.createFromThread();

        return containerObj.getMembers().stream().map(child -> {
            PID childPid = child.getPid();
            if (!aclService.hasAccess(childPid, agent.getPrincipals(), Permission.ingest)) {
                return (SwordCollection) null;
            }

            SwordCollection collection = new SwordCollection();
            collection.setHref(config.getSwordPath() + SwordConfigurationImpl.COLLECTION_PATH + "/"
                      + childPid.getId());
            collection.setTitle(childPid.getId());
            collection.addAccepts("application/zip");
            collection.addAccepts("text/xml");
            collection.addAccepts("application/xml");
            for (PackagingType packaging : acceptedPackaging) {
                collection.addAcceptPackaging(packaging.getUri());
            }
            collection.setMediation(true);

            IRI iri = new IRI(config.getSwordPath() + SwordConfigurationImpl.SERVICE_DOCUMENT_PATH + "/"
                    + childPid.getId());
            collection.addSubService(iri);
            return collection;
        }).collect(Collectors.toList());
    }

    public void setAcceptedPackaging(Map<PackagingType, DepositHandler> packageTypeHandlers) {
        this.acceptedPackaging = packageTypeHandlers.keySet();
    }

}
