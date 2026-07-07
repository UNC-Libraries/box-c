package edu.unc.lib.boxc.indexing.solr.filter;

import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackageDataLoader;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.FolderObject;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.services.ContentPathFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.facets.CutoffFacet;
import edu.unc.lib.boxc.search.api.requests.SearchRequest;
import edu.unc.lib.boxc.search.solr.facets.CutoffFacetImpl;
import edu.unc.lib.boxc.search.solr.models.ContentObjectSolrRecord;
import edu.unc.lib.boxc.search.solr.models.IndexDocumentBean;
import edu.unc.lib.boxc.search.solr.responses.SearchResultResponse;
import edu.unc.lib.boxc.search.solr.services.SolrSearchService;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class SetWcagComplianceFilterTest {
    private SetWcagComplianceFilter filter;
    private DocumentIndexingPackage dip;
    private PID pid;
    private AutoCloseable closeable;
    private IndexDocumentBean idb;
    private CutoffFacet ancestorPath;
    @Mock
    private FileObject fileObj;
    @Mock
    private WorkObject workObj;
    @Mock
    private FolderObject folderObject;
    @Mock
    private Resource resource;
    @Mock
    private Statement statement;
    @Mock
    private SolrSearchService solrSearchService;
    @Mock
    private ContentPathFactory contentPathFactory;
    @Mock
    private SearchResultResponse searchResultResponse;
    @Mock
    private DocumentIndexingPackageDataLoader documentIndexingPackageDataLoader;

    @BeforeEach
    public void setup() throws Exception {
        closeable = openMocks(this);

        pid = PIDs.get(UUID.randomUUID().toString());
        dip = new DocumentIndexingPackage(pid, null, documentIndexingPackageDataLoader);
        dip.setPid(pid);
        idb = dip.getDocument();
        when(fileObj.getResource()).thenReturn(resource);

        ancestorPath = new CutoffFacetImpl(SearchFieldKey.ANCESTOR_PATH.name(), Arrays.asList(
                "1,1ed05130-d25f-4890-9086-02d98625275f", "2,5aa1ad67-c494-48dc-839e-241826559abb"), 0);
        when(solrSearchService.getSearchResults(any(SearchRequest.class))).thenReturn(searchResultResponse);
        when(solrSearchService.getAncestorPath(pid.getId(), null)).thenReturn(ancestorPath);

        filter = new SetWcagComplianceFilter();
        filter.setSolrSearchService(solrSearchService);
        filter.setContentPathFactory(contentPathFactory);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testGetWcagComplianceFileObject() {
        var level = "WCAG 1.0 Level A";
        when(fileObj.getPid()).thenReturn(pid);
        when(resource.hasProperty(eq(Cdr.wcagCompliance))).thenReturn(true);
        when(resource.getProperty(eq(Cdr.wcagCompliance))).thenReturn(statement);
        when(statement.getString()).thenReturn(level);
        dip.setContentObject(fileObj);

        filter.filter(dip);
        assertHasLevels(idb, level);
    }

    @Test
    public void testGetWcagComplianceWorkObject() {
        dip.setContentObject(workObj);

        var fileRec1 = new ContentObjectSolrRecord();
        fileRec1.setWcagComplianceLevel(List.of("WCAG 1.0 Level A"));
        var fileRec2 = new ContentObjectSolrRecord();
        fileRec2.setWcagComplianceLevel(List.of("WCAG 1.0 Level AA"));
        when(searchResultResponse.getResultList()).thenReturn(List.of(fileRec1, fileRec2));

        filter.filter(dip);
        assertHasLevels(idb, "WCAG 1.0 Level A", "WCAG 1.0 Level AA");
    }

    @Test
    public void testGetWcagComplianceNonFileOrWorkObject() {
        dip.setContentObject(folderObject);

        filter.filter(dip);
        assertNull(idb.getWcagComplianceLevel());
    }

    @Test
    public void testWorkWithNoFiles() {
        dip.setContentObject(workObj);
        when(searchResultResponse.getResultList()).thenReturn(Collections.emptyList());

        filter.filter(dip);
        assertEquals(List.of(), idb.getWcagComplianceLevel());
    }

    private void assertHasLevels(IndexDocumentBean idb, String... expectedLevels) {
        for (var expected: expectedLevels) {
            assertTrue(idb.getWcagComplianceLevel().contains(expected),
                    "Object did not have expected type " + expected + ", types were: " + idb.getWcagComplianceLevel());
        }
        assertEquals(expectedLevels.length, idb.getWcagComplianceLevel().size(),
                "Incorrect number of types, expected: " + Arrays.toString(expectedLevels) +
                        ", found: " + idb.getWcagComplianceLevel());
    }
}
