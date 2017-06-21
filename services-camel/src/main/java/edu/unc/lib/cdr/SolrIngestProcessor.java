/**
 * Copyright 2017 The University of North Carolina at Chapel Hill
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

package edu.unc.lib.cdr;

import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;

import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackageFactory;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPipeline;
import edu.unc.lib.dl.data.ingest.solr.indexing.SolrUpdateDriver;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.Repository;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.IndexingActionType;

public class SolrIngestProcessor implements Processor {
    private final Repository repository;
    private final int maxRetries;
    private final long retryDelay;
    private DocumentIndexingPackageFactory factory = new DocumentIndexingPackageFactory();
    private DocumentIndexingPipeline pipeline = new DocumentIndexingPipeline();
    private SolrUpdateDriver solrUpdateDriver = new SolrUpdateDriver();

    public SolrIngestProcessor(Repository repository, int maxRetries, long retryDelay) {
        this.repository = repository;
        this.maxRetries = maxRetries;
        this.retryDelay = retryDelay;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        final Message in = exchange.getIn();
        String fcrepoBinaryUri = (String) in.getHeader(FCREPO_URI);
        PID pid = PIDs.get(fcrepoBinaryUri);

        SolrUpdateRequest updateRequest = new SolrUpdateRequest(pid, IndexingActionType.ADD);
        DocumentIndexingPackage dip = updateRequest.getDocumentIndexingPackage();
        
        if (dip == null) {
            dip = factory.createDip(updateRequest.getPid());
            updateRequest.setDocumentIndexingPackage(dip);
        }

        pipeline.process(dip);
        solrUpdateDriver.addDocument(dip.getDocument());
    }
}