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
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.fcrepo4.BinaryObject;
import edu.unc.lib.dl.fcrepo4.FileObject;
import edu.unc.lib.dl.fcrepo4.FolderObject;
import edu.unc.lib.dl.fcrepo4.WorkObject;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;

/**
 *
 * @author harring
 *
 */
public class SetContentTypeFilterTest {

    private static final String PID_STRING = "uuid:07d9594f-310d-4095-ab67-79a1056e7430";

    @Mock
    private DocumentIndexingPackage dip;
    @Mock
    private PID pid;
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
    @Captor
    private ArgumentCaptor<List<String>> listCaptor;

    private SetContentTypeFilter filter;

    @Before
    public void setup() throws Exception {
        initMocks(this);

        when(pid.getPid()).thenReturn(PID_STRING);

        when(dip.getDocument()).thenReturn(idb);
        when(dip.getPid()).thenReturn(pid);

        when(workObj.getPrimaryObject()).thenReturn(fileObj);
        when(fileObj.getOriginalFile()).thenReturn(binObj);

        filter = new SetContentTypeFilter();
    }

    @Test
    public void testGetContentTypeFromWorkObject() throws Exception {
        when(dip.getContentObject()).thenReturn(workObj);
        when(workObj.getPrimaryObject()).thenReturn(fileObj);

        when(binObj.getFilename()).thenReturn("primary.xml");
        when(binObj.getMimetype()).thenReturn("application/xml");

        filter.filter(dip);

        verify(idb).setContentType(listCaptor.capture());
        assertEquals("^text,Text", listCaptor.getValue().get(0));
        assertEquals("/text^xml,xml", listCaptor.getValue().get(1));
    }

    @Test
    public void testGetContentTypeFromFileObject() throws Exception {
        when(dip.getContentObject()).thenReturn(fileObj);
        when(fileObj.getOriginalFile()).thenReturn(binObj);
        when(binObj.getFilename()).thenReturn("data.csv");
        when(binObj.getMimetype()).thenReturn("ext.csv");

        filter.filter(dip);

        verify(idb).setContentType(listCaptor.capture());
        assertEquals("^dataset,Dataset", listCaptor.getValue().get(0));
        assertEquals("/dataset^csv,csv", listCaptor.getValue().get(1));
    }

    @Test
    public void testExtensionNotFoundInMapping() throws Exception {
        when(dip.getContentObject()).thenReturn(fileObj);
        when(fileObj.getOriginalFile()).thenReturn(binObj);
        // use filename with raw image extension not found in our mapping
        when(binObj.getFilename()).thenReturn("image.x3f");
        when(binObj.getMimetype()).thenReturn("some_wacky_type");

        filter.filter(dip);

        verify(idb).setContentType(listCaptor.capture());
        assertEquals("^unknown,Unknown", listCaptor.getValue().get(0));
        assertEquals("/unknown^x3f,x3f", listCaptor.getValue().get(1));
    }

    @Test
    public void testNotWorkAndNotFileObject() throws Exception {
        when(dip.getContentObject()).thenReturn(folderObj);

        filter.filter(dip);

        verify(idb, never()).setContentType(anyListOf(String.class));
    }

    @Test
    public void testWorkWithoutPrimaryObject() throws Exception {
        when(dip.getContentObject()).thenReturn(workObj);
        when(workObj.getPrimaryObject()).thenReturn(null);

        filter.filter(dip);

        verify(idb, never()).setContentType(anyListOf(String.class));
    }

    @Test
    public void testGetPlainTextContentType() throws Exception {
        when(dip.getContentObject()).thenReturn(fileObj);
        when(fileObj.getOriginalFile()).thenReturn(binObj);
        when(binObj.getFilename()).thenReturn("file.txt");
        when(binObj.getMimetype()).thenReturn("text/plain");

        filter.filter(dip);

        verify(idb).setContentType(listCaptor.capture());
        assertEquals("^text,Text", listCaptor.getValue().get(0));
        assertEquals("/text^txt,txt", listCaptor.getValue().get(1));
    }
}
