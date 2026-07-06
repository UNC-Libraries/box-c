package edu.unc.lib.boxc.indexing.solr.filter;

import edu.unc.lib.boxc.indexing.solr.exception.IndexingException;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.boxc.indexing.solr.utils.SearchGeneratorUtil;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.services.ContentPathFactory;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.solr.models.IndexDocumentBean;
import edu.unc.lib.boxc.search.solr.services.SolrSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Sets fields representing the WCAG compliance level. Values are set for works and file objects.
 *
 * @author snluong
 *
 */
public class SetWcagComplianceFilter implements IndexDocumentFilter {
    private static final Logger log = LoggerFactory.getLogger(SetWcagComplianceFilter.class);
    private SolrSearchService solrSearchService;
    private ContentPathFactory contentPathFactory;

    @Override
    public void filter(DocumentIndexingPackage dip) throws IndexingException {
        var contentObj = dip.getContentObject();
        var doc = dip.getDocument();
        if (contentObj instanceof WorkObject) {
            log.debug("Indexing WCAG compliance of work {}", dip.getPid().getId());
            setWcagComplianceForWorkObject(doc);
        } else if (contentObj instanceof FileObject) {
            setWcagComplianceForFileObject((FileObject) contentObj, doc);
        }
    }

    private void setWcagComplianceForWorkObject(IndexDocumentBean doc) {
        var searchState = SearchGeneratorUtil.getSearchState(doc, contentPathFactory);
        searchState.setResultFields(List.of(SearchFieldKey.WCAG_COMPLIANCE.name()));
        var result = SearchGeneratorUtil.getSearchResults(searchState, solrSearchService);
        var levels = new HashSet<String>();
        for (var child: result.getResultList()) {
            if (child.getWcagComplianceLevel() != null) {
                levels.addAll(child.getWcagComplianceLevel());
            }
        }
        log.debug("Query for children of work {} had WCAG compliance levels {} from {} files",
                doc.getId(), levels, result.getResultCount());
        doc.setWcagComplianceLevel(new ArrayList<>(levels));
    }

    private void setWcagComplianceForFileObject(FileObject fileObj, IndexDocumentBean doc) {
        var resource = fileObj.getResource();
        var level = resource.hasProperty(Cdr.wcagCompliance) ?
                resource.getProperty(Cdr.wcagCompliance).getString() : null;
        if (level != null) {
            log.debug("FileObject {} has WCAG compliance level {} indexed", fileObj.getPid().getId(), level);
            doc.setWcagComplianceLevel(List.of(level));
        }
        log.debug("FileObject {} WCAG compliance level is null", fileObj.getPid().getId());
    }

    public void setSolrSearchService(SolrSearchService solrSearchService) {
        this.solrSearchService = solrSearchService;
    }
    public void setContentPathFactory(ContentPathFactory contentPathFactory) {
        this.contentPathFactory = contentPathFactory;
    }
}
