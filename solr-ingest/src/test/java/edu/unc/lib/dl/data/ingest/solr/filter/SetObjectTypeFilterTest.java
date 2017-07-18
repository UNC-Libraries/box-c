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

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.Fcrepo4Repository;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;
import edu.unc.lib.dl.util.ResourceType;

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

    @Before
    public void setup() throws Exception {
        initMocks(this);

        when(pid.getPid()).thenReturn("uuid:" + UUID.randomUUID().toString());

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

    @Test(expected = IndexingException.class)
    public void testNoResourceType() throws Exception {
        when(contentObj.getTypes()).thenReturn(Collections.emptyList());

        filter.filter(dip);
    }

    @Test(expected = IndexingException.class)
    public void testBadResourceType() throws Exception {
        when(contentObj.getTypes()).thenReturn(Arrays.asList("http://example.com/bad"));

        filter.filter(dip);
    }
}
