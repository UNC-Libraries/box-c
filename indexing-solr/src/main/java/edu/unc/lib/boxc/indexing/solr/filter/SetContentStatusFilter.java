package edu.unc.lib.boxc.indexing.solr.filter;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.indexing.solr.exception.IndexingException;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.search.api.FacetConstants;

/**
 * Sets content-related status tags
 *
 * @author harring
 *
 */
public class SetContentStatusFilter implements IndexDocumentFilter{
    private static final Logger log = LoggerFactory.getLogger(SetContentStatusFilter.class);
    @Override
    public void filter(DocumentIndexingPackage dip) throws IndexingException {
        List<String> contentStatus = determineContentStatus(dip);
        dip.getDocument().setContentStatus(contentStatus);

        log.debug("Content status for {} set to {}", dip.getPid().toString(), contentStatus);
    }

    private List<String> determineContentStatus(DocumentIndexingPackage dip)
            throws IndexingException {

        List<String> status = new ArrayList<>();
        ContentObject obj = dip.getContentObject();
        Resource resc = obj.getResource();

        if (resc.hasProperty(Cdr.hasMods)) {
            status.add(FacetConstants.CONTENT_DESCRIBED);
        } else {
            status.add(FacetConstants.CONTENT_NOT_DESCRIBED);
        }

        if (obj instanceof WorkObject) {
            addWorkObjectStatuses(status, resc);
        }

        if (obj instanceof FileObject) {
            Resource parentResc = obj.getParent().getResource();
            if (parentResc.hasProperty(Cdr.primaryObject, resc)) {
                status.add(FacetConstants.IS_PRIMARY_OBJECT);
            }
        }

        return status;
    }

    private void addWorkObjectStatuses(List<String> status, Resource resource) {
        if (resource.hasProperty(Cdr.primaryObject)) {
            status.add(FacetConstants.HAS_PRIMARY_OBJECT);
        } else {
            status.add(FacetConstants.NO_PRIMARY_OBJECT);
        }

        if (resource.hasProperty(Cdr.memberOrder)) {
            status.add(FacetConstants.MEMBERS_ARE_ORDERED);
        } else {
            status.add(FacetConstants.MEMBERS_ARE_UNORDERED);
        }
    }
}
