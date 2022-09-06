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
package edu.unc.lib.boxc.indexing.solr.filter;

import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackageDataLoader;
import edu.unc.lib.boxc.indexing.solr.utils.MemberOrderService;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.search.solr.models.IndexDocumentBean;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author bbpennel
 */
public class SetMemberOrderFilterTest {
    private static final String SUBJECT_UUID = "f277bb38-272c-471c-a28a-9887a1328a1f";

    @Mock
    private MemberOrderService memberOrderService;
    @Mock
    private DocumentIndexingPackageDataLoader documentIndexingPackageDataLoader;
    private SetMemberOrderFilter filter;

    private DocumentIndexingPackage dip;
    private IndexDocumentBean idb;
    private PID subjectPid;

    @Before
    public void setup() {
        initMocks(this);
        subjectPid = PIDs.get(SUBJECT_UUID);
        dip = new DocumentIndexingPackage(subjectPid, null, documentIndexingPackageDataLoader);
        dip.setPid(subjectPid);
        idb = dip.getDocument();
        filter = new SetMemberOrderFilter();
        filter.setMemberOrderService(memberOrderService);
    }

    @Test
    public void withFileInUnorderedWorkTest() {
        var subject = mock(FileObject.class);
        when(documentIndexingPackageDataLoader.getContentObject(dip)).thenReturn(subject);
        when(memberOrderService.getOrderValue(subject)).thenReturn(null);

        filter.filter(dip);

        assertNull(idb.getMemberOrderId());
    }

    @Test
    public void withFileInOrderedWorkTest() {
        Integer expected = Integer.valueOf(4);
        var subject = mock(FileObject.class);
        when(documentIndexingPackageDataLoader.getContentObject(dip)).thenReturn(subject);
        when(memberOrderService.getOrderValue(subject)).thenReturn(expected);

        filter.filter(dip);

        assertEquals(expected, idb.getMemberOrderId());
    }
}
