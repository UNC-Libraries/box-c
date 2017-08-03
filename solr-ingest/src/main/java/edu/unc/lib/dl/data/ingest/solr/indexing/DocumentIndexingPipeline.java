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
package edu.unc.lib.dl.data.ingest.solr.indexing;

import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.filter.IndexDocumentFilter;

/**
 *
 * @author bbpennel
 *
 */
public class DocumentIndexingPipeline implements DocumentFilteringPipeline {
    private static final Logger log = LoggerFactory.getLogger(DocumentIndexingPipeline.class);
    protected Collection<IndexDocumentFilter> filters;

    @Override
    public void process(DocumentIndexingPackage dip) throws IndexingException {

        for (IndexDocumentFilter filter : filters) {
            log.info("filter {} executed on pid {}", filter.getClass().getName(), dip.getPid());
            filter.filter(dip);
        }
    }

    @Override
    public void setFilters(List<IndexDocumentFilter> filters) {
        this.filters = filters;
    }
}