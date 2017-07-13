/**
 * Copyright 2017 The University of North Carolina at Chapel Hill
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
import edu.unc.lib.dl.fcrepo4.BinaryObject;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fcrepo4.FileObject;
import edu.unc.lib.dl.fcrepo4.FolderObject;
import edu.unc.lib.dl.fcrepo4.WorkObject;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;
import edu.unc.lib.dl.search.solr.util.FacetConstants;

public class SetContentStatusFilterTest {
    @Mock
    private DocumentIndexingPackage dip;
    @Mock
    private PID pid;
    @Mock
    private ContentObject contentObj;
    @Mock
    private FileObject fileObj;
    @Mock
    private WorkObject workObj;
    @Mock
    private BinaryObject binObj;
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
        when(contentObj.getResource()).thenReturn(resc);

        when(resc.hasProperty(any(Property.class))).thenReturn(true);

        filter = new SetContentStatusFilter();
    }

    @Test
    public void testDescribedWork() throws Exception {
        when(dip.getContentObject()).thenReturn(workObj);
        when(workObj.getResource()).thenReturn(resc);

        filter.filter(dip);

        verify(idb).setContentStatus(listCaptor.capture());
        assertTrue(listCaptor.getValue().contains(FacetConstants.CONTENT_DESCRIBED));

    }

    @Test
    public void testNotDescribedWork() throws Exception {
        when(dip.getContentObject()).thenReturn(workObj);
        when(workObj.getResource()).thenReturn(resc);
        when(resc.hasProperty(any(Property.class))).thenReturn(false);

        filter.filter(dip);

        verify(idb).setContentStatus(listCaptor.capture());
        assertTrue(listCaptor.getValue().contains(FacetConstants.CONTENT_NOT_DESCRIBED));

    }


}
