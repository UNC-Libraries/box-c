package edu.unc.lib.boxc.services.camel.destroy;

import edu.unc.lib.boxc.operations.jms.order.MemberOrderRequestSender;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.fcrepo.client.FcrepoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.services.InheritedAclFactory;
import edu.unc.lib.boxc.fcrepo.utils.TransactionManager;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.operations.api.events.PremisLoggerFactory;
import edu.unc.lib.boxc.operations.impl.destroy.AbstractDestroyObjectsJob;
import edu.unc.lib.boxc.operations.impl.destroy.DestroyObjectsCompletelyJob;
import edu.unc.lib.boxc.operations.impl.destroy.DestroyObjectsJob;
import edu.unc.lib.boxc.operations.jms.MessageSender;
import edu.unc.lib.boxc.operations.jms.destroy.DestroyObjectsRequest;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingMessageSender;
import edu.unc.lib.boxc.persist.api.storage.StorageLocationManager;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferService;
import edu.unc.lib.boxc.search.solr.services.ObjectPathFactory;

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
    private PremisLoggerFactory premisLoggerFactory;
    private MemberOrderRequestSender memberOrderRequestSender;

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
            job.setPremisLoggerFactory(premisLoggerFactory);
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
            job.setPremisLoggerFactory(premisLoggerFactory);
            job.setMemberOrderRequestSender(memberOrderRequestSender);
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

    public void setPremisLoggerFactory(PremisLoggerFactory premisLoggerFactory) {
        this.premisLoggerFactory = premisLoggerFactory;
    }

    public void setMemberOrderRequestSender(MemberOrderRequestSender memberOrderRequestSender) {
        this.memberOrderRequestSender = memberOrderRequestSender;
    }
}
