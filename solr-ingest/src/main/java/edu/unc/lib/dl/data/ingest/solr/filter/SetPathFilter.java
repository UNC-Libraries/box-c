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
package edu.unc.lib.dl.data.ingest.solr.filter;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fcrepo4.FileObject;
import edu.unc.lib.dl.fedora.ContentPathFactory;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;

/**
 * Indexing filter which extracts and stores hierarchical path information for
 * the object being processed.
 *
 * Sets: ancestorPath, parentCollection, parentUnit, rollup
 *
 * @author lfarrell
 *
 */
public class SetPathFilter implements IndexDocumentFilter {
    private static final Logger log = LoggerFactory.getLogger(SetPathFilter.class);

    private static final int COLLECTION_DEPTH = 2;
    private static final int UNIT_DEPTH = 1;

    private ContentPathFactory pathFactory;

    @Override
    public void filter(DocumentIndexingPackage dip) throws IndexingException {
        log.debug("Performing set path filter for {}", dip.getPid());

        IndexDocumentBean idb = dip.getDocument();
        List<PID> pids = pathFactory.getAncestorPids(dip.getPid());

        if (pids.size() == 0) {
            throw new IndexingException("Object " + dip.getPid() + " has no known ancestors");
        }

        List<String> ancestorPath = new ArrayList<>();

        int i = 1;
        for (PID ancestorPid : pids) {
            ancestorPath.add(i + "," + ancestorPid.getId());
            i++;
        }

        idb.setAncestorPath(ancestorPath);

        // TODO
        // idb.setAncestorIds(ancestorIds);

        if (pids.size() > COLLECTION_DEPTH) {
            idb.setParentCollection(pids.get(COLLECTION_DEPTH).getId());
        }

        if (pids.size() > UNIT_DEPTH) {
            idb.setParentUnit(pids.get(UNIT_DEPTH).getId());
        }

        ContentObject contentObject = dip.getContentObject();

        String rollup;

        if (contentObject instanceof FileObject) {
            rollup = pids.get(pids.size() - 1).getId();
        } else {
            rollup = contentObject.getPid().getId();
        }

        idb.setRollup(rollup);
    }

    /**
     * Set path factory
     *
     * @param pathFactory
     */
    public void setPathFactory(ContentPathFactory pathFactory) {
        this.pathFactory = pathFactory;
    }
}
