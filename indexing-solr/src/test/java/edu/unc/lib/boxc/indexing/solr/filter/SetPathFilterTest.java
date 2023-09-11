package edu.unc.lib.boxc.indexing.solr.filter;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import edu.unc.lib.boxc.search.solr.services.TitleRetrievalService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import edu.unc.lib.boxc.indexing.solr.exception.IndexingException;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackageDataLoader;
import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.services.ContentPathFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths;
import edu.unc.lib.boxc.search.solr.models.IndexDocumentBean;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.ContentRootObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;

/**
 *
 * @author lfarrell
 *
 */
public class SetPathFilterTest {
    private final static String UNIT_TITLE = "Administration of Boxy";
    private final static String COLLECTION_TITLE = "Collection of Boxes";

    private AutoCloseable closeable;

    @Mock
    private ContentPathFactory pathFactory;
    @Mock
    private DocumentIndexingPackageDataLoader loader;
    @Mock
    private DocumentIndexingPackage dip;
    @Mock
    private IndexDocumentBean idb;
    @Mock
    private ContentObject contentObject;
    @Mock
    private TitleRetrievalService titleRetrievalService;
    private PID pid;
    @Mock
    private FileObject fileObject;
    @Captor
    private ArgumentCaptor<List<String>> listCaptor;
    @Captor
    private ArgumentCaptor<String> stringCaptor;

    private SetPathFilter filter;

    private int WORK_OBJECT_DEPTH = 3;

    @BeforeEach
    public void setUp() throws Exception {
        closeable = openMocks(this);

        pid = PIDs.get(UUID.randomUUID().toString());

        when(dip.getDocument()).thenReturn(idb);
        when(dip.getPid()).thenReturn(pid);
        when(dip.getContentObject()).thenReturn(contentObject);
        when(contentObject.getPid()).thenReturn(pid);

        filter = new SetPathFilter();
        filter.setPathFactory(pathFactory);
        filter.setTitleRetrievalService(titleRetrievalService);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testUnitPath() throws Exception {
        // Assert that the parent unit and collection are not set
        List<PID> pids = makePidList(1);
        when(pathFactory.getAncestorPids(pid)).thenReturn(pids);
        when(idb.getResourceType()).thenReturn(ResourceType.AdminUnit.name());

        filter.filter(dip);

        verify(idb).setAncestorPath(listCaptor.capture());
        List<String> ancestorPath = listCaptor.getValue();
        ancestorPathCheck(ancestorPath, pids);
        assertEquals(1, ancestorPath.size());

        assertAncestorIds(pids, true);

        verify(idb, never()).setParentUnit(anyString());
        verify(idb, never()).setParentCollection(anyString());
    }

    @Test
    public void testCollectionPath() throws Exception {
        List<PID> pids = makePidList(2);
        when(pathFactory.getAncestorPids(pid)).thenReturn(pids);
        when(idb.getResourceType()).thenReturn(ResourceType.Collection.name());
        when(titleRetrievalService.retrieveTitle(pids.get(1))).thenReturn(UNIT_TITLE);

        filter.filter(dip);

        verify(idb).setAncestorPath(listCaptor.capture());
        List<String> ancestorPath = listCaptor.getValue();
        ancestorPathCheck(ancestorPath, pids);
        assertEquals(2, ancestorPath.size());

        assertAncestorIds(pids, true);

        verify(idb).setParentUnit(eq(UNIT_TITLE + "|" + pids.get(1).getId()));
        verify(idb, never()).setParentCollection(anyString());

    }

    @Test
    public void testWorkPath() throws Exception {
        // Assert that the rollup is the id of the work itself
        List<PID> pids = makePidList(3);
        when(pathFactory.getAncestorPids(pid)).thenReturn(pids);
        when(idb.getResourceType()).thenReturn(ResourceType.Work.name());
        when(titleRetrievalService.retrieveTitle(pids.get(1))).thenReturn(UNIT_TITLE);
        when(titleRetrievalService.retrieveTitle(pids.get(2))).thenReturn(COLLECTION_TITLE);

        filter.filter(dip);

        verify(idb).setAncestorPath(listCaptor.capture());
        List<String> ancestorPath = listCaptor.getValue();
        ancestorPathCheck(ancestorPath, pids);
        assertEquals(3, ancestorPath.size());

        assertAncestorIds(pids, true);

        verify(idb).setParentUnit(eq(UNIT_TITLE + "|" + pids.get(1).getId()));
        verify(idb).setParentCollection(eq(COLLECTION_TITLE + "|" + pids.get(2).getId()));
        verify(idb).setRollup(eq(contentObject.getPid().getId()));
    }

    @Test
    public void testContentRootPath() throws Exception {
        when(pathFactory.getAncestorPids(pid)).thenReturn(emptyList());
        ContentRootObject mockRoot = mock(ContentRootObject.class);
        when(mockRoot.getPid()).thenReturn(RepositoryPaths.getContentRootPid());
        when(dip.getContentObject()).thenReturn(mockRoot);

        filter.filter(dip);

        verify(idb).setAncestorPath(listCaptor.capture());
        List<String> ancestorPath = listCaptor.getValue();
        assertEquals(0, ancestorPath.size());
    }

    @Test
    public void testFileObjectPath() throws Exception {
        when(fileObject.getPid()).thenReturn(pid);
        when(dip.getContentObject()).thenReturn(fileObject);

        // Assert that the rollup is the id of the parent work
        List<PID> pids = makePidList(4);
        when(pathFactory.getAncestorPids(pid)).thenReturn(pids);
        when(idb.getResourceType()).thenReturn(ResourceType.File.name());
        when(titleRetrievalService.retrieveTitle(pids.get(1))).thenReturn(UNIT_TITLE);
        when(titleRetrievalService.retrieveTitle(pids.get(2))).thenReturn(COLLECTION_TITLE);

        filter.filter(dip);

        verify(idb).setAncestorPath(listCaptor.capture());
        List<String> ancestorPath = listCaptor.getValue();
        ancestorPathCheck(ancestorPath, pids);
        assertEquals(4, ancestorPath.size());

        assertAncestorIds(pids, false);

        verify(idb).setParentUnit(eq(UNIT_TITLE + "|" + pids.get(1).getId()));
        verify(idb).setParentCollection(eq(COLLECTION_TITLE + "|" + pids.get(2).getId()));
        verify(idb).setRollup(eq(pids.get(WORK_OBJECT_DEPTH).getId()));
    }

    @Test
    public void testNoAncestors() throws Exception {
        Assertions.assertThrows(IndexingException.class, () -> {
            when(pathFactory.getAncestorPids(pid)).thenReturn(Collections.emptyList());
            filter.filter(dip);
        });
    }

    private List<PID> makePidList(int numPids) {
        List<PID> pids = new ArrayList<>();
        for (int i = 0; i < numPids; i++) {
            pids.add(PIDs.get(UUID.randomUUID().toString()));
        }

        return pids;
    }

    private void ancestorPathCheck(List<String> ancestorPath, List<PID> pids) {
        for(int i = 0; i < ancestorPath.size(); i++) {
            assertTrue(ancestorPath.get(i).equals(i + 1 + "," + pids.get(i).getId()));
        }
    }

    private void assertAncestorIds(List<PID> pids, boolean includeSelf) {
        verify(idb).setAncestorIds(stringCaptor.capture());

        String joinedIds = "/" + pids.stream()
                .map(pid -> pid.getId())
                .collect(Collectors.joining("/"));

        if (includeSelf) {
            joinedIds += "/" + pid.getId();
        }

        assertEquals(joinedIds, stringCaptor.getValue());
    }
}
