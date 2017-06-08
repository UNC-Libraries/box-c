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
package edu.unc.lib.dl.data.ingest.solr.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;

/**
 * Filter which retrieves an item's default display order from its parent if the parent has an MD_CONTENTS datastream.
 *
 * @author bbpennel
 *
 */
public class SetDisplayOrderFilter implements IndexDocumentFilter {
    private static final Logger log = LoggerFactory.getLogger(SetDisplayOrderFilter.class);
    @Override
    public void filter(DocumentIndexingPackage dip) throws IndexingException {
        IndexDocumentBean idb = dip.getDocument();

        DocumentIndexingPackage parentDIP = dip.getParentDocument();

        try {
            Long order = parentDIP.getDisplayOrder(dip.getPid().getPid());
            idb.setDisplayOrder(order);
            if (order == null)
                log.debug("No parent MD contents, display order is null");
        } catch (NumberFormatException e) {
            throw new IndexingException("Unable to parse order number for " + dip.getPid().getPid(), e);
        }
    }
}
