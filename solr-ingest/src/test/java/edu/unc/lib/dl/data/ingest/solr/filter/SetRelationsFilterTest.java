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

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.List;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackageDataLoader;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackageFactory;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fcrepo4.FileObject;
import edu.unc.lib.dl.fcrepo4.WorkObject;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;

/**
 * @author harring
 */
public class SetRelationsFilterTest {

    private static final String INVALID_TERM = "some_invalid_term";

    @Mock
    private DocumentIndexingPackageDataLoader loader;
    @Mock
    private DocumentIndexingPackage dip;
    @Mock
    private IndexDocumentBean idb;
    @Mock
    private PID pid;
    @Mock
    private WorkObject workObj;
    @Mock
    private FileObject fileObj;
    @Mock
    private ContentObject contentObj;
    @Mock
    private Resource resource;
    @Mock
    private StmtIterator it;
    @Mock
    private Statement stmt;
    @Mock
    private Literal literal;
    @Captor
    private ArgumentCaptor<List<String>> listCaptor;

    private DocumentIndexingPackageFactory factory;

    private SetRelationsFilter filter;

    @Before
    public void setup() throws Exception {
        initMocks(this);

        factory = new DocumentIndexingPackageFactory();
        factory.setDataLoader(loader);

        when(dip.getDocument()).thenReturn(idb);
        when(dip.getPid()).thenReturn(pid);
        when(pid.getId()).thenReturn("id");

        when(workObj.getPrimaryObject()).thenReturn(fileObj);
        when(workObj.getPid()).thenReturn(pid);
        when(fileObj.getPid()).thenReturn(pid);
        when(workObj.getResource()).thenReturn(resource);
        when(fileObj.getResource()).thenReturn(resource);
        when(resource.listProperties(Cdr.invalidTerm)).thenReturn(it);
        when(it.hasNext()).thenReturn(true, false);
        when(it.nextStatement()).thenReturn(stmt);
        when(stmt.getLiteral()).thenReturn(literal);
        when(literal.getString()).thenReturn(INVALID_TERM);

        filter = new SetRelationsFilter();
    }

    @Test
    public void setPrimaryObjectFromWorkTest() throws Exception {
        when(dip.getContentObject()).thenReturn(workObj);
        String primObjPid = "pofromwork";
        when(pid.getId()).thenReturn(primObjPid);

        filter.filter(dip);

        verify(idb).setRelations(listCaptor.capture());
        assertTrue(listCaptor.getValue().contains("http://cdr.unc.edu/definitions/model#primaryObject|" + primObjPid));
    }

    @Test
    public void setInvalidTermsTest() throws Exception {
        when(dip.getContentObject()).thenReturn(workObj);

        filter.filter(dip);

        verify(idb).setRelations(listCaptor.capture());
        assertTrue(listCaptor.getValue().contains("http://cdr.unc.edu/definitions/model#invalidTerm|" + INVALID_TERM));
    }

}
