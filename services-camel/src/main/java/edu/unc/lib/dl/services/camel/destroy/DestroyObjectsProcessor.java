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
package edu.unc.lib.dl.services.camel.destroy;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.fcrepo.client.FcrepoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.dl.acl.fcrepo4.InheritedAclFactory;
import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.fcrepo4.TransactionManager;
import edu.unc.lib.dl.persist.api.storage.StorageLocationManager;
import edu.unc.lib.dl.persist.api.transfer.BinaryTransferService;
import edu.unc.lib.dl.persist.services.destroy.AbstractDestroyObjectsJob;
import edu.unc.lib.dl.persist.services.destroy.DestroyObjectsCompletelyJob;
import edu.unc.lib.dl.persist.services.destroy.DestroyObjectsJob;
import edu.unc.lib.dl.persist.services.destroy.DestroyObjectsRequest;
import edu.unc.lib.dl.search.solr.service.ObjectPathFactory;
import edu.unc.lib.dl.services.IndexingMessageSender;
import edu.unc.lib.dl.services.MessageSender;

/**
 * Processor which handles messages requesting the destruction of objects.
 *
 * @author bbpennel
 *
 */
public class DestroyObjectsProcessor implements Processor {

    final Logger log = LoggerFactory.getLogger(DestroyObjectsProcessor.class);

    private static final ObjectReader MAPPER = new ObjectMapper().readerFor(DestroyObjectsRequest.class);

    private AccessControlService aclService;
    private RepositoryObjectFactory repoObjFactory;
    private RepositoryObjectLoader repoObjLoader;
    private TransactionManager txManager;
    private ObjectPathFactory pathFactory;
    private FcrepoClient fcrepoClient;
    private InheritedAclFactory inheritedAclFactory;
    private StorageLocationManager locManager;
    private BinaryTransferService transferService;
    private IndexingMessageSender indexingMessageSender;
    private MessageSender binaryDestroyedMessageSender;

    @Override
    public void process(Exchange exchange) throws Exception {
        log.debug("Processing xml import request");
        final Message in = exchange.getIn();

        DestroyObjectsRequest request = MAPPER.readValue((String) in.getBody());
        AbstractDestroyObjectsJob job = createJob(request);
        job.run();
    }

    private AbstractDestroyObjectsJob createJob(DestroyObjectsRequest request) {
        if (request.isDestroyCompletely()) {
            DestroyObjectsCompletelyJob job = new DestroyObjectsCompletelyJob(request);
            job.setFcrepoClient(fcrepoClient);
            job.setRepoObjFactory(repoObjFactory);
            job.setRepoObjLoader(repoObjLoader);
            job.setTransactionManager(txManager);
            job.setAclService(aclService);
            job.setBinaryTransferService(transferService);
            job.setStorageLocationManager(locManager);
            job.setIndexingMessageSender(indexingMessageSender);
            job.setBinaryDestroyedMessageSender(binaryDestroyedMessageSender);
            return job;
        } else {
            DestroyObjectsJob job = new DestroyObjectsJob(request);
            job.setFcrepoClient(fcrepoClient);
            job.setPathFactory(pathFactory);
            job.setRepoObjFactory(repoObjFactory);
            job.setRepoObjLoader(repoObjLoader);
            job.setTransactionManager(txManager);
            job.setAclService(aclService);
            job.setInheritedAclFactory(inheritedAclFactory);
            job.setBinaryTransferService(transferService);
            job.setStorageLocationManager(locManager);
            job.setIndexingMessageSender(indexingMessageSender);
            job.setBinaryDestroyedMessageSender(binaryDestroyedMessageSender);
            return job;
        }
    }

    public void setAclService(AccessControlService aclService) {
        this.aclService = aclService;
    }

    public void setRepositoryObjectFactory(RepositoryObjectFactory repoObjFactory) {
        this.repoObjFactory = repoObjFactory;
    }

    public void setRepositoryObjectLoader(RepositoryObjectLoader repoObjLoader) {
        this.repoObjLoader = repoObjLoader;
    }

    public void setTransactionManager(TransactionManager txManager) {
        this.txManager = txManager;
    }

    public void setObjectPathFactory(ObjectPathFactory pathFactory) {
        this.pathFactory = pathFactory;
    }

    public void setFcrepoClient(FcrepoClient fcrepoClient) {
        this.fcrepoClient = fcrepoClient;
    }

    public void setInheritedAclFactory(InheritedAclFactory inheritedAclFactory) {
        this.inheritedAclFactory = inheritedAclFactory;
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
