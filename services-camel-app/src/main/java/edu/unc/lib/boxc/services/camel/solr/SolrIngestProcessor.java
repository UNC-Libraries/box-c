package edu.unc.lib.boxc.services.camel.solr;

import static edu.unc.lib.boxc.common.metrics.TimerFactory.createTimerForClass;
import static edu.unc.lib.boxc.fcrepo.FcrepoJmsConstants.RESOURCE_TYPE;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;

import java.util.ArrayList;
import java.util.List;

import edu.unc.lib.boxc.operations.jms.MessageSender;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.indexing.solr.SolrUpdateRequest;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackageFactory;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPipeline;
import edu.unc.lib.boxc.indexing.solr.indexing.SolrUpdateDriver;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.rdf.Fcrepo4Repository;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import io.dropwizard.metrics5.Timer;

/**
 * Index newly ingested objects in Solr
 *
 * @author lfarrell
 *
 */
public class SolrIngestProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(SolrIngestProcessor.class);
    private static final Timer timer = createTimerForClass(SolrIngestProcessor.class);

    private DocumentIndexingPackageFactory factory;
    private DocumentIndexingPipeline pipeline;
    private SolrUpdateDriver solrUpdateDriver;
    private RepositoryObjectLoader repoObjLoader;
    private MessageSender updateWorkSender;

    public SolrIngestProcessor(DocumentIndexingPackageFactory factory,
            DocumentIndexingPipeline pipeline, SolrUpdateDriver solrUpdateDriver,
            RepositoryObjectLoader repoObjLoader) {
        this.factory = factory;
        this.pipeline = pipeline;
        this.solrUpdateDriver = solrUpdateDriver;
        this.repoObjLoader = repoObjLoader;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        try (Timer.Context context = timer.time()) {
            final Message in = exchange.getIn();
            String fcrepoUri = (String) in.getHeader(FCREPO_URI);

            log.debug("Processing solr request for {}", fcrepoUri);

            List<PID> targetPids = new ArrayList<>();
            PID targetPid = PIDs.get(fcrepoUri);
            String resourceTypes = (String) in.getHeader(RESOURCE_TYPE);

            PID grandParentWorkPid = null;
            // for binaries, need to index the file and work objects which contain it
            if (resourceTypes != null && resourceTypes.contains(Fcrepo4Repository.Binary.getURI())) {
                targetPid = PIDs.get(targetPid.getId());
                FileObject parentFile = repoObjLoader.getFileObject(targetPid);
                RepositoryObject grandParent = parentFile.getParent();
                // Trigger indexing of the work containing this file object
                if (grandParent instanceof WorkObject) {
                    grandParentWorkPid = grandParent.getPid();
                }
            }

            targetPids.add(targetPid);

            log.debug("Indexing objects {}", targetPids);
            for (PID pid: targetPids) {
                SolrUpdateRequest updateRequest = new SolrUpdateRequest(
                        pid.getRepositoryPath(), IndexingActionType.ADD);

                DocumentIndexingPackage dip = factory.createDip(pid);
                updateRequest.setDocumentIndexingPackage(dip);

                pipeline.process(dip);
                solrUpdateDriver.addDocument(dip.getDocument());
            }

            if (grandParentWorkPid != null) {
                log.debug("Requesting indexing of work {} containing file {}", grandParentWorkPid.getId(), targetPid);
                updateWorkSender.sendMessage(grandParentWorkPid.getQualifiedId());
            }
        }
    }

    public void setUpdateWorkSender(MessageSender updateWorkSender) {
        this.updateWorkSender = updateWorkSender;
    }
}