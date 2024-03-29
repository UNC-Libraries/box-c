package edu.unc.lib.boxc.indexing.solr.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import edu.unc.lib.boxc.search.solr.facets.FilterableDisplayValueFacet;
import edu.unc.lib.boxc.search.solr.services.TitleRetrievalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.indexing.solr.exception.IndexingException;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.boxc.model.api.ids.ContentPathConstants;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.ContentRootObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.services.ContentPathFactory;
import edu.unc.lib.boxc.search.solr.models.IndexDocumentBean;

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

    private ContentPathFactory pathFactory;
    private TitleRetrievalService titleRetrievalService;

    @Override
    public void filter(DocumentIndexingPackage dip) throws IndexingException {
        log.debug("Performing set path filter for {}", dip.getPid());

        IndexDocumentBean idb = dip.getDocument();
        List<PID> pids = pathFactory.getAncestorPids(dip.getPid());

        if (pids.size() == 0 && !(dip.getContentObject() instanceof ContentRootObject)) {
            throw new IndexingException("Object " + dip.getPid() + " has no known ancestors");
        }

        List<String> ancestorPath = new ArrayList<>();

        // Construct ancestorPath with all objects leading up to this object
        int i = 1;
        for (PID ancestorPid : pids) {
            ancestorPath.add(i + "," + ancestorPid.getId());
            i++;
        }

        idb.setAncestorPath(ancestorPath);

        // Construct ancestorIds with all ancestors plus itself if it is a container
        String ancestorIds = "/" + pids.stream()
                .map(pid -> pid.getId())
                .collect(Collectors.joining("/"));
        if (!(dip.getContentObject() instanceof FileObject)) {
            ancestorIds += "/" + dip.getPid().getId();
        }
        idb.setAncestorIds(ancestorIds);

        if (pids.size() > ContentPathConstants.COLLECTION_DEPTH) {
            idb.setParentCollection(buildParentValue(pids.get(ContentPathConstants.COLLECTION_DEPTH)));
        }

        if (pids.size() > ContentPathConstants.UNIT_DEPTH) {
            idb.setParentUnit(buildParentValue(pids.get(ContentPathConstants.UNIT_DEPTH)));
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

    private String buildParentValue(PID pid) {
        var title = titleRetrievalService.retrieveTitle(pid);
        return FilterableDisplayValueFacet.buildValue(title, pid.getId());
    }

    /**
     * Set path factory
     *
     * @param pathFactory
     */
    public void setPathFactory(ContentPathFactory pathFactory) {
        this.pathFactory = pathFactory;
    }

    public void setTitleRetrievalService(TitleRetrievalService titleRetrievalService) {
        this.titleRetrievalService = titleRetrievalService;
    }
}
