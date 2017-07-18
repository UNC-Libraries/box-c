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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackageDataLoader;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fcrepo4.FileObject;
import edu.unc.lib.dl.fedora.ContentPathFactory;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;

/**
 *
 * @author lfarrell
 *
 */
public class SetPathFilterTest {
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
    private PID pid;
    @Mock
    private FileObject fileObject;
    @Captor
    private ArgumentCaptor<List<String>> listCaptor;

    private SetPathFilter filter;

    private int WORK_OBJECT_DEPTH = 4;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        when(pid.getPid()).thenReturn("uuid:" + UUID.randomUUID().toString());

        when(dip.getDocument()).thenReturn(idb);
        when(dip.getPid()).thenReturn(pid);
        when(dip.getContentObject()).thenReturn(contentObject);
        when(contentObject.getPid()).thenReturn(pid);

        filter = new SetPathFilter();
        filter.setPathFactory(pathFactory);
    }

    @Test
    public void testUnitPath() throws Exception {
        // Assert that the parent unit and collection are not set
        List<PID> pids = makePidList(1);
        when(pathFactory.getAncestorPids(pid)).thenReturn(pids);
        filter.filter(dip);

        verify(idb).setAncestorPath(listCaptor.capture());
        List<String> ancestorPath = listCaptor.getValue();
        ancestorPathCheck(ancestorPath, pids);
        assertEquals(1, ancestorPath.size());

        verify(idb, never()).setParentUnit(anyString());
        verify(idb, never()).setParentCollection(anyString());
    }

    @Test
    public void testCollectionPath() throws Exception {
        List<PID> pids = makePidList(2);

        when(pathFactory.getAncestorPids(pid)).thenReturn(pids);
        filter.filter(dip);

        verify(idb).setAncestorPath(listCaptor.capture());
        List<String> ancestorPath = listCaptor.getValue();
        ancestorPathCheck(ancestorPath, pids);
        assertEquals(2, ancestorPath.size());

        verify(idb).setParentUnit(eq(pids.get(1).getId()));
        verify(idb, never()).setParentCollection(anyString());

    }

    @Test
    public void testWorkPath() throws Exception {
        // Assert that the rollup is the id of the work itself
        List<PID> pids = makePidList(4);
        when(pathFactory.getAncestorPids(pid)).thenReturn(pids);
        filter.filter(dip);

        verify(idb).setAncestorPath(listCaptor.capture());
        List<String> ancestorPath = listCaptor.getValue();
        ancestorPathCheck(ancestorPath, pids);
        assertEquals(4, ancestorPath.size());

        verify(idb).setRollup(eq(contentObject.getPid().getId()));
    }

    @Test
    public void testFileObjectPath() throws Exception {
        // Assert that the rollup is the id of the parent work
        List<PID> pids = makePidList(5);
        when(pathFactory.getAncestorPids(pid)).thenReturn(pids);
        when(dip.getContentObject()).thenReturn(fileObject);
        filter.filter(dip);

        verify(idb).setAncestorPath(listCaptor.capture());
        List<String> ancestorPath = listCaptor.getValue();
        ancestorPathCheck(ancestorPath, pids);
        assertEquals(5, ancestorPath.size());

        verify(idb).setRollup(eq(pids.get(WORK_OBJECT_DEPTH).getId()));
    }

    @Test(expected = IndexingException.class)
    public void testNoAncestors() throws Exception {
        when(pathFactory.getAncestorPids(pid)).thenReturn(Collections.emptyList());
        filter.filter(dip);
    }

    private List<PID> makePidList(int numPids) {
        List<PID> pids = new ArrayList<>();
        int i = 0;
        while(i < numPids) {
            PID pidMock = mock(PID.class);
            when(pidMock.getId()).thenReturn("uuid:" + UUID.randomUUID().toString());
            pids.add(pidMock);
            i++;
        }

        return pids;
    }

    private void ancestorPathCheck(List<String> ancestorPath, List<PID> pids) {
        for(int i = 0; i < ancestorPath.size(); i++) {
            assertTrue(ancestorPath.get(i).equals(i + 1 + "," + pids.get(i).getId()));
        }
    }
}
