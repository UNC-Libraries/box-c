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
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.text.SimpleDateFormat;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;

import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackageDataLoader;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Fcrepo4Repository;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;

/*
 * @author harring
 */
public class SetRecordDatesFilterTest {

    private static final String PID_STRING = "uuid:07d9594f-310d-4095-ab67-79a1056e7430";
    private static final String DATE_ADDED = "2017-01-01";
    private static final String DATE_MODIFIED = "2017-05-31";
    private static final String BAD_DATE = "abcd";

    @Mock
    private DocumentIndexingPackageDataLoader loader;
    @Mock
    private DocumentIndexingPackage dip;
    @Mock
    private PID pid;
    @Mock
    private ContentObject contentObj;
    @Mock
    private Resource resource;
    @Mock
    private Statement stmt1;
    @Mock
    private Statement stmt2;
    @Mock
    private Literal literal1;
    @Mock
    private Literal literal2;
    @Mock
    private Object object1;
    @Mock
    private Object object2;

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    private IndexDocumentBean idb;
    private SetRecordDatesFilter filter;

    @Before
    public void setup() throws Exception {
        idb = new IndexDocumentBean();
        initMocks(this);

        when(pid.getPid()).thenReturn(PID_STRING);

        when(dip.getDocument()).thenReturn(idb);
        when(dip.getPid()).thenReturn(pid);
        when(dip.getContentObject()).thenReturn(contentObj);

        when(contentObj.getResource()).thenReturn(resource);

        when(resource.getProperty(Fcrepo4Repository.created))
            .thenReturn(stmt1);
        when(stmt1.getLiteral()).thenReturn(literal1);
        when(literal1.getValue()).thenReturn(object1);
        when(object1.toString()).thenReturn(DATE_ADDED);
        when(resource.getProperty(Fcrepo4Repository.lastModified))
            .thenReturn(stmt2);
        when(stmt2.getLiteral()).thenReturn(literal2);
        when(literal2.getValue()).thenReturn(object2);
        when(object2.toString()).thenReturn(DATE_MODIFIED);

        filter = new SetRecordDatesFilter();
    }

    @Test
    public void testCreateDate() throws Exception {
        filter.filter(dip);
        assertEquals(DATE_ADDED, new SimpleDateFormat("yyyy-MM-dd").format(idb.getDateAdded()));
    }

    @Test
    public void testUpdateDate() throws Exception {
        filter.filter(dip);
        assertEquals(DATE_MODIFIED, new SimpleDateFormat("yyyy-MM-dd").format(idb.getDateUpdated()));
    }

    @Test
    public void testUnparseableDate() throws Exception {
        expectedEx.expect((IndexingException.class));
        // checks that the exception message contains the substring param
        expectedEx.expectMessage("Failed to parse record dates from ");

        when(object2.toString()).thenReturn(BAD_DATE);

        filter.filter(dip);
    }

}
