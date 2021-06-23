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

package edu.unc.lib.dl.services.camel.solr;

import static edu.unc.lib.dl.fcrepo4.FcrepoJmsConstants.RESOURCE_TYPE;
import static edu.unc.lib.boxc.common.metrics.TimerFactory.createTimerForClass;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackageFactory;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPipeline;
import edu.unc.lib.dl.data.ingest.solr.indexing.SolrUpdateDriver;
import edu.unc.lib.dl.fcrepo4.FileObject;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fcrepo4.WorkObject;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Fcrepo4Repository;
import edu.unc.lib.dl.util.IndexingActionType;
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

            // for binaries, need to index the file and work objects which contain it
            if (resourceTypes != null && resourceTypes.contains(Fcrepo4Repository.Binary.getURI())) {
                targetPid = PIDs.get(targetPid.getId());
                FileObject parentFile = repoObjLoader.getFileObject(targetPid);
                RepositoryObject grandParent = parentFile.getParent();
                // Index both the parent file and work
                if (grandParent instanceof WorkObject) {
                    targetPids.add(grandParent.getPid());
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
        }
    }
}