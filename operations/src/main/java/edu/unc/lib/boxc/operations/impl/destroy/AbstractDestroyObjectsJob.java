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

import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.FCR_TOMBSTONE;
import static edu.unc.lib.boxc.operations.jms.destroy.DestroyObjectsMessageHelpers.makeDestroyOperationBody;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;
import org.jdom2.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.common.util.URIUtil;
import edu.unc.lib.boxc.fcrepo.exceptions.ServiceException;
import edu.unc.lib.boxc.fcrepo.utils.TransactionManager;
import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.api.events.PremisLoggerFactory;
import edu.unc.lib.boxc.operations.jms.MessageSender;
import edu.unc.lib.boxc.operations.jms.destroy.DestroyObjectsRequest;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingMessageSender;
import edu.unc.lib.boxc.persist.api.storage.StorageLocation;
import edu.unc.lib.boxc.persist.api.storage.StorageLocationManager;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferException;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferService;
import edu.unc.lib.boxc.persist.api.transfer.MultiDestinationTransferSession;

/**
 * @author bbpennel
 */
public abstract class AbstractDestroyObjectsJob implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(AbstractDestroyObjectsJob.class);

    protected List<PID> objsToDestroy;
    protected AgentPrincipals agent;

    protected MultiDestinationTransferSession transferSession;

    protected List<URI> cleanupBinaryUris;

    protected RepositoryObjectFactory repoObjFactory;
    protected RepositoryObjectLoader repoObjLoader;
    protected IndexingMessageSender indexingMessageSender;
    protected FcrepoClient fcrepoClient;
    protected MessageSender binaryDestroyedMessageSender;
    protected AccessControlService aclService;
    protected StorageLocationManager locManager;
    protected BinaryTransferService transferService;
    protected PremisLoggerFactory premisLoggerFactory;

    protected TransactionManager txManager;


    protected AbstractDestroyObjectsJob(DestroyObjectsRequest request) {
        this.objsToDestroy = stream(request.getIds()).map(PIDs::get).collect(toList());
        this.agent = request.getAgent();
        this.cleanupBinaryUris = new ArrayList<>();
    }

    protected void sendBinariesDestroyedMsg(RepositoryObject repoObj, List<URI> binaryUris) {
        Map<String, String> metadata = new HashMap<>();
        PID pid;

        if (repoObj instanceof FileObject) {
            FileObject fileObj = (FileObject) repoObj;
            BinaryObject binaryObj = fileObj.getOriginalFile();
            String mimetype = binaryObj.getMimetype();
            metadata.put("mimeType", mimetype);
            pid = binaryObj.getPid();
        } else {
            pid = repoObj.getPid();
        }

        setCommonMetadata(metadata, repoObj, pid);

        Document destroyMsg = makeDestroyOperationBody(agent.getUsername(), binaryUris, metadata);
        binaryDestroyedMessageSender.sendMessage(destroyMsg);
    }

    private Map<String, String> setCommonMetadata(Map<String, String> metadata, RepositoryObject repoObj, PID pid) {
        String objType = ResourceType.getResourceTypeForUris(repoObj.getTypes()).getUri();
        metadata.put("objType", objType);
        metadata.put("pid", pid.getQualifiedId());

        return metadata;
    }

    protected void destroyBinaries() {
        if (cleanupBinaryUris.isEmpty()) {
            return;
        }

        if (transferSession == null) {
            transferSession = transferService.getSession();
        }
        cleanupBinaryUris.forEach(contentUri -> {
            try {
                log.error("Deleting destroyed binary {}", contentUri);
                StorageLocation storageLoc = locManager.getStorageLocationForUri(contentUri);
                transferSession.forDestination(storageLoc)
                        .delete(contentUri);
            } catch (BinaryTransferException e) {
                String message = e.getCause() == null ? e.getMessage() : e.getCause().getMessage();
                log.error("Failed to cleanup binary {} for destroyed object: {}", contentUri, message);
            }
        });
    }

    /**
     * Remove the specified object and its fcr:tombstone from fedora
     * @param objUri
     */
    protected void purgeObject(String objUri) {
        log.debug("Deleting object {} from fedora", objUri);
        try (FcrepoResponse resp = fcrepoClient.delete(URI.create(objUri)).perform()) {
        } catch (FcrepoOperationFailedException | IOException e) {
            throw new ServiceException("Unable to clean up child object " + objUri, e);
        }

        URI tombstoneUri = URI.create(URIUtil.join(objUri, FCR_TOMBSTONE));
        try (FcrepoResponse resp = fcrepoClient.delete(tombstoneUri).perform()) {
        } catch (FcrepoOperationFailedException | IOException e) {
            throw new ServiceException("Unable to clean up child tombstone object " + objUri, e);
        }
    }

    public void setRepoObjFactory(RepositoryObjectFactory repoObjFactory) {
        this.repoObjFactory = repoObjFactory;
    }

    public void setRepoObjLoader(RepositoryObjectLoader repoObjLoader) {
        this.repoObjLoader = repoObjLoader;
    }

    public void setTransactionManager(TransactionManager txManager) {
        this.txManager = txManager;
    }

    public void setFcrepoClient(FcrepoClient fcrepoClient) {
        this.fcrepoClient = fcrepoClient;
    }

    public void setAclService(AccessControlService aclService) {
        this.aclService = aclService;
    }

    public void setPremisLoggerFactory(PremisLoggerFactory premisLoggerFactory) {
        this.premisLoggerFactory = premisLoggerFactory;
    }

    public void setStorageLocationManager(StorageLocationManager locManager) {
        this.locManager = locManager;
    }

    public void setBinaryTransferService(BinaryTransferService transferService) {
        this.transferService = transferService;
    }

    public void setIndexingMessageSender(IndexingMessageSender indexingMessageSender) {
        this.indexingMessageSender = indexingMessageSender;
    }

    public void setBinaryDestroyedMessageSender(MessageSender binaryDestroyedMessageSender) {
        this.binaryDestroyedMessageSender = binaryDestroyedMessageSender;
    }
}
