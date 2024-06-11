package edu.unc.lib.boxc.indexing.solr.filter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.List;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.search.api.FacetConstants;
import edu.unc.lib.boxc.search.solr.models.IndexDocumentBean;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.FolderObject;
import edu.unc.lib.boxc.model.api.objects.WorkObject;

/**
 * @author harring
 */
public class SetContentStatusFilterTest {
    private AutoCloseable closeable;
    @Mock
    private DocumentIndexingPackage dip;
    @Mock
    private PID pid;
    @Mock
    private FileObject fileObj;
    @Mock
    private WorkObject workObj;
    @Mock
    private FolderObject folderObj;
    @Mock
    private IndexDocumentBean idb;
    @Mock
    private Resource resc, fileResc;
    @Captor
    private ArgumentCaptor<List<String>> listCaptor;

    private SetContentStatusFilter filter;

    @BeforeEach
    public void setUp() throws Exception {
        closeable = openMocks(this);

        when(dip.getDocument()).thenReturn(idb);
        when(dip.getPid()).thenReturn(pid);
        // by default an object in this test suite has only FacetConstants.CONTENT_NOT_DESCRIBED set
        when(resc.hasProperty(any(Property.class))).thenReturn(false);

        when(fileObj.getParent()).thenReturn(workObj);
        filter = new SetContentStatusFilter();
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testDescribedWork() throws Exception {
        when(dip.getContentObject()).thenReturn(workObj);
        when(workObj.getResource()).thenReturn(resc);
        when(resc.hasProperty(Cdr.hasMods)).thenReturn(true);

        filter.filter(dip);

        verify(idb).setContentStatus(listCaptor.capture());
        assertTrue(listCaptor.getValue().contains(FacetConstants.CONTENT_DESCRIBED));
        assertFalse(listCaptor.getValue().contains(FacetConstants.CONTENT_NOT_DESCRIBED));
    }

    @Test
    public void testNotDescribedWork() throws Exception {
        when(dip.getContentObject()).thenReturn(workObj);
        when(workObj.getResource()).thenReturn(resc);

        filter.filter(dip);

        verify(idb).setContentStatus(listCaptor.capture());
        assertTrue(listCaptor.getValue().contains(FacetConstants.CONTENT_NOT_DESCRIBED));
        assertFalse(listCaptor.getValue().contains(FacetConstants.CONTENT_DESCRIBED));
    }

    @Test
    public void testWorkNoPrimaryObject() throws Exception {
        when(dip.getContentObject()).thenReturn(workObj);
        when(workObj.getResource()).thenReturn(resc);

        filter.filter(dip);

        verify(idb).setContentStatus(listCaptor.capture());
        assertTrue(listCaptor.getValue().contains(FacetConstants.NO_PRIMARY_OBJECT));
    }

    @Test
    public void testFolderNoPrimaryObject() throws Exception {
        when(dip.getContentObject()).thenReturn(folderObj);
        when(folderObj.getResource()).thenReturn(resc);

        filter.filter(dip);

        verify(idb).setContentStatus(listCaptor.capture());
        assertFalse(listCaptor.getValue().contains(FacetConstants.NO_PRIMARY_OBJECT));
    }

    @Test
    public void testWorkWithPrimaryObject() throws Exception {
        when(dip.getContentObject()).thenReturn(workObj);
        when(workObj.getResource()).thenReturn(resc);
        when(resc.hasProperty(Cdr.primaryObject)).thenReturn(true);

        filter.filter(dip);

        verify(idb).setContentStatus(listCaptor.capture());
        assertTrue(listCaptor.getValue().contains(FacetConstants.HAS_PRIMARY_OBJECT));
        assertFalse(listCaptor.getValue().contains(FacetConstants.NO_PRIMARY_OBJECT));
    }

    @Test
    public void testIsPrimaryObject() throws Exception {
        when(workObj.getResource()).thenReturn(resc);
        when(resc.hasProperty(Cdr.primaryObject, fileResc)).thenReturn(true);

        when(dip.getContentObject()).thenReturn(fileObj);
        when(fileObj.getResource()).thenReturn(fileResc);

        filter.filter(dip);

        verify(idb).setContentStatus(listCaptor.capture());
        assertTrue(listCaptor.getValue().contains(FacetConstants.IS_PRIMARY_OBJECT));
    }

    @Test
    public void testUnpublishedFileObject() throws Exception {
        when(workObj.getResource()).thenReturn(resc);

        when(dip.getContentObject()).thenReturn(fileObj);
        when(fileObj.getResource()).thenReturn(fileResc);
        when(fileResc.hasProperty(Cdr.unpublished)).thenReturn(true);

        filter.filter(dip);

        verify(idb).setContentStatus(listCaptor.capture());
    }

    @Test
    public void testWorkWithMemberOrder() {
        when(dip.getContentObject()).thenReturn(workObj);
        when(workObj.getResource()).thenReturn(resc);
        when(resc.hasProperty(Cdr.memberOrder)).thenReturn(true);

        filter.filter(dip);

        verify(idb).setContentStatus(listCaptor.capture());
        assertTrue(listCaptor.getValue().contains(FacetConstants.MEMBERS_ARE_ORDERED));
        assertFalse(listCaptor.getValue().contains(FacetConstants.MEMBERS_ARE_UNORDERED));
    }

    @Test
    public void testWorkWithoutMemberOrder() {
        when(dip.getContentObject()).thenReturn(workObj);
        when(workObj.getResource()).thenReturn(resc);

        filter.filter(dip);

        verify(idb).setContentStatus(listCaptor.capture());
        assertTrue(listCaptor.getValue().contains(FacetConstants.MEMBERS_ARE_UNORDERED));
        assertFalse(listCaptor.getValue().contains(FacetConstants.MEMBERS_ARE_ORDERED));
    }

    @Test
    public void testWorkWithAssignedThumbnail() throws Exception {
        when(dip.getContentObject()).thenReturn(workObj);
        when(workObj.getResource()).thenReturn(resc);
        when(resc.hasProperty(Cdr.useAsThumbnail)).thenReturn(true);

        filter.filter(dip);

        verify(idb).setContentStatus(listCaptor.capture());
        assertTrue(listCaptor.getValue().contains(FacetConstants.HAS_THUMBNAIL_ASSIGNED));
        assertFalse(listCaptor.getValue().contains(FacetConstants.NO_THUMBNAIL_ASSIGNED));
    }

    @Test
    public void testWorkNoAssignedThumbnail() throws Exception {
        when(dip.getContentObject()).thenReturn(workObj);
        when(workObj.getResource()).thenReturn(resc);

        filter.filter(dip);

        verify(idb).setContentStatus(listCaptor.capture());
        assertTrue(listCaptor.getValue().contains(FacetConstants.NO_THUMBNAIL_ASSIGNED));
    }

    @Test
    public void testIsAssignedThumbnail() throws Exception {
        when(workObj.getResource()).thenReturn(resc);
        when(resc.hasProperty(Cdr.useAsThumbnail, fileResc)).thenReturn(true);

        when(dip.getContentObject()).thenReturn(fileObj);
        when(fileObj.getResource()).thenReturn(fileResc);

        filter.filter(dip);

        verify(idb).setContentStatus(listCaptor.capture());
        assertTrue(listCaptor.getValue().contains(FacetConstants.ASSIGNED_AS_THUMBNAIL));
    }
}
