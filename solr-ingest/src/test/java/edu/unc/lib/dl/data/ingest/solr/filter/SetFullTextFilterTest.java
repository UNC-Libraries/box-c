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

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackageDataLoader;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackageFactory;
import edu.unc.lib.dl.fcrepo4.BinaryObject;
import edu.unc.lib.dl.fcrepo4.FileObject;
import edu.unc.lib.dl.fcrepo4.WorkObject;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;

/**
 * @author harring
 */
public class SetFullTextFilterTest extends Assert {

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
    private BinaryObject binObj;
    @Captor
    private ArgumentCaptor<String> stringCaptor;

    private InputStream stream;
    private String fullText = "some text";

    private DocumentIndexingPackageFactory factory;

    private SetFullTextFilter filter;

    @Before
    public void setup() throws Exception {
        initMocks(this);

        factory = new DocumentIndexingPackageFactory();
        factory.setDataLoader(loader);
        stream = new ByteArrayInputStream(fullText.getBytes("UTF-8"));

        when(dip.getDocument()).thenReturn(idb);
        when(dip.getPid()).thenReturn(pid);
        when(pid.getId()).thenReturn("id");
        when(fileObj.getOriginalFile()).thenReturn(binObj);
        when(binObj.getMimetype()).thenReturn("text/plain");
        when(binObj.getBinaryStream()).thenReturn(stream);

        filter = new SetFullTextFilter();
    }

    @Test
    public void testFullTextWithWorkObject() throws Exception {
        when(dip.getContentObject()).thenReturn(workObj);
        when(workObj.getPrimaryObject()).thenReturn(fileObj);

        filter.filter(dip);

        verify(idb).setFullText(stringCaptor.capture());
        assertEquals(fullText, stringCaptor.getValue());

    }

    @Test
    public void testFullTextWithFileObject() throws Exception {
        when(dip.getContentObject()).thenReturn(fileObj);

        filter.filter(dip);

        verify(idb).setFullText(stringCaptor.capture());
        assertEquals(fullText, stringCaptor.getValue());
    }

    @Test
    public void testNoFullText() throws Exception {
        when(dip.getContentObject()).thenReturn(fileObj);
        when(binObj.getMimetype()).thenReturn("application/json");

        filter.filter(dip);

        verify(idb, never()).setFullText(anyString());
    }

    @Test (expected = IndexingException.class)
    public void testBadInputStream() throws Exception {
        when(dip.getContentObject()).thenReturn(fileObj);

        doThrow(new FedoraException("Mocking error getting binary stream")).when(binObj).getBinaryStream();

        filter.filter(dip);
    }

}
