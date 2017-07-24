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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.List;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.fcrepo4.FileObject;
import edu.unc.lib.dl.fcrepo4.FolderObject;
import edu.unc.lib.dl.fcrepo4.WorkObject;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;
import edu.unc.lib.dl.search.solr.util.FacetConstants;

/**
 * @author harring
 */
public class SetContentStatusFilterTest {
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
    private Resource resc;
    @Captor
    private ArgumentCaptor<List<String>> listCaptor;

    private SetContentStatusFilter filter;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        when(dip.getDocument()).thenReturn(idb);
        when(dip.getPid()).thenReturn(pid);
        // by default an object in this test suite has only FacetConstants.CONTENT_NOT_DESCRIBED set
        when(resc.hasProperty(any(Property.class))).thenReturn(false);

        filter = new SetContentStatusFilter();
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
        assertFalse(listCaptor.getValue().contains(FacetConstants.NO_PRIMARY_OBJECT));
    }

    @Test
    public void testWorkWithInvalidTerm() throws Exception {
        when(dip.getContentObject()).thenReturn(workObj);
        when(workObj.getResource()).thenReturn(resc);
        when(resc.hasProperty(Cdr.invalidTerm)).thenReturn(true);

        filter.filter(dip);

        verify(idb).setContentStatus(listCaptor.capture());
        assertTrue(listCaptor.getValue().contains(FacetConstants.INVALID_VOCAB_TERM));
    }

    @Test
    public void testUnpublishedFileObject() throws Exception {
        when(dip.getContentObject()).thenReturn(fileObj);
        when(fileObj.getResource()).thenReturn(resc);
        when(resc.hasProperty(Cdr.unpublished)).thenReturn(true);

        filter.filter(dip);

        verify(idb).setContentStatus(listCaptor.capture());
        assertTrue(listCaptor.getValue().contains(FacetConstants.UNPUBLISHED));
    }

}
