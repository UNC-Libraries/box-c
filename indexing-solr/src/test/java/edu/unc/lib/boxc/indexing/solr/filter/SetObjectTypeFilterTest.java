package edu.unc.lib.boxc.indexing.solr.filter;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import edu.unc.lib.boxc.indexing.solr.exception.IndexingException;
import edu.unc.lib.boxc.indexing.solr.filter.SetObjectTypeFilter;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.Fcrepo4Repository;
import edu.unc.lib.boxc.search.solr.models.IndexDocumentBean;
import edu.unc.lib.boxc.model.api.objects.ContentObject;

/**
 *
 * @author bbpennel
 *
 */
public class SetObjectTypeFilterTest {

    private SetObjectTypeFilter filter;

    @Mock
    private DocumentIndexingPackage dip;
    @Mock
    private IndexDocumentBean idb;
    @Mock
    private ContentObject contentObj;

    @Mock
    private PID pid;

    @BeforeEach
    public void setup() throws Exception {
        initMocks(this);

        when(dip.getDocument()).thenReturn(idb);
        when(dip.getPid()).thenReturn(pid);

        when(dip.getContentObject()).thenReturn(contentObj);

        filter = new SetObjectTypeFilter();
    }

    @Test
    public void testWorkResourceType() throws Exception {
        when(contentObj.getTypes()).thenReturn(Arrays.asList(Cdr.Work.getURI()));

        filter.filter(dip);

        verify(idb).setResourceType(eq(ResourceType.Work.name()));
        verify(idb).setResourceTypeSort(eq(ResourceType.Work.getDisplayOrder()));
    }

    @Test
    public void testMultipleRdfTypes() throws Exception {
        when(contentObj.getTypes()).thenReturn(Arrays.asList(
                Fcrepo4Repository.Resource.getURI(),
                Fcrepo4Repository.Container.getURI(),
                Cdr.AdminUnit.getURI()));

        filter.filter(dip);

        verify(idb).setResourceType(eq(ResourceType.AdminUnit.name()));
        verify(idb).setResourceTypeSort(eq(ResourceType.AdminUnit.getDisplayOrder()));
    }

    @Test
    public void testNoResourceType() throws Exception {
        Assertions.assertThrows(IndexingException.class, () -> {
            when(contentObj.getTypes()).thenReturn(Collections.emptyList());

            filter.filter(dip);
        });
    }

    @Test
    public void testBadResourceType() throws Exception {
        Assertions.assertThrows(IndexingException.class, () -> {
            when(contentObj.getTypes()).thenReturn(Arrays.asList("http://example.com/bad"));

            filter.filter(dip);
        });
    }
}
