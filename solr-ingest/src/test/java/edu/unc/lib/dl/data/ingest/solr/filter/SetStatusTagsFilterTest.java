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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;
import edu.unc.lib.dl.search.solr.util.FacetConstants;

/**
 * @author harring
 */
public class SetStatusTagsFilterTest {
    @Mock
    private DocumentIndexingPackage dip;
    @Mock
    private IndexDocumentBean idb;
    @Captor
    private ArgumentCaptor<List<String>> setCaptor;

    private List<String> contentStatus;
    private List<String> accessStatus;

    private SetStatusTagsFilter filter;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        contentStatus = new ArrayList<>();
        accessStatus = new ArrayList<>();

        when(dip.getDocument()).thenReturn(idb);
        when(idb.getContentStatus()).thenReturn(contentStatus);
        when(idb.getStatus()).thenReturn(accessStatus);

        filter = new SetStatusTagsFilter();
    }

    @Test
    public void testDescribedWork() throws Exception {
        contentStatus.add(FacetConstants.CONTENT_DESCRIBED);

        filter.filter(dip);

        verify(idb).setStatusTags(setCaptor.capture());
        assertTrue(setCaptor.getValue().contains(FacetConstants.CONTENT_DESCRIBED));
        assertFalse(setCaptor.getValue().contains(FacetConstants.CONTENT_NOT_DESCRIBED));
    }

    @Test
    public void testNotDescribedWork() throws Exception {
        filter.filter(dip);

        verify(idb).setStatusTags(setCaptor.capture());
        assertTrue(setCaptor.getValue().contains(FacetConstants.CONTENT_NOT_DESCRIBED));
        assertFalse(setCaptor.getValue().contains(FacetConstants.CONTENT_DESCRIBED));
    }

    @Test
    public void testPrimaryObject() throws Exception {
        contentStatus.add(FacetConstants.IS_PRIMARY_OBJECT);

        filter.filter(dip);

        verify(idb).setStatusTags(setCaptor.capture());
        assertTrue(setCaptor.getValue().contains(FacetConstants.IS_PRIMARY_OBJECT));
    }

    @Test
    public void testUnpublished() throws Exception {
        contentStatus.add(FacetConstants.UNPUBLISHED);

        filter.filter(dip);

        verify(idb).setStatusTags(setCaptor.capture());
        assertTrue(setCaptor.getValue().contains(FacetConstants.UNPUBLISHED));
    }

    @Test
    public void testEmbargoed() throws Exception {
        accessStatus.add(FacetConstants.EMBARGOED);

        filter.filter(dip);

        verify(idb).setStatusTags(setCaptor.capture());
        assertTrue(setCaptor.getValue().contains(FacetConstants.EMBARGOED));
    }

    @Test
    public void testNoStatusTags() throws Exception {
        // NB: only one status tag is set in the default case, CONTENT_NOT_DESCRIBED
        filter.filter(dip);

        verify(idb).setStatusTags(setCaptor.capture());
        assertFalse(setCaptor.getValue().contains(FacetConstants.EMBARGOED));
        assertFalse(setCaptor.getValue().contains(FacetConstants.UNPUBLISHED));
        assertFalse(setCaptor.getValue().contains(FacetConstants.IS_PRIMARY_OBJECT));
        assertFalse(setCaptor.getValue().contains(FacetConstants.CONTENT_DESCRIBED));
        assertTrue(setCaptor.getValue().contains(FacetConstants.CONTENT_NOT_DESCRIBED));
    }

}
