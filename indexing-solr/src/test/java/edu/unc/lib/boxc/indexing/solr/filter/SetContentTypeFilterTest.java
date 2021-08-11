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

import edu.unc.lib.boxc.indexing.solr.filter.SetContentTypeFilter;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.FolderObject;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.search.solr.models.IndexDocumentBean;

/**
 *
 * @author harring
 *
 */
public class SetContentTypeFilterTest {

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
        mockFile("data.csv", "ext.csv");

        filter.filter(dip);

        verify(idb).setContentType(listCaptor.capture());
        assertEquals("^dataset,Dataset", listCaptor.getValue().get(0));
        assertEquals("/dataset^csv,csv", listCaptor.getValue().get(1));
    }

    @Test
    public void testExtensionNotFoundInMapping() throws Exception {
        // use filename with raw image extension not found in our mapping
        mockFile("image.x3f", "some_wacky_type");

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
        mockFile("file.txt", "text/plain");

        filter.filter(dip);

        verify(idb).setContentType(listCaptor.capture());
        assertEquals("^text,Text", listCaptor.getValue().get(0));
        assertEquals("/text^txt,txt", listCaptor.getValue().get(1));
    }

    @Test
    public void testAppleDoublePdf() throws Exception {
        mockFile("._doc.pdf", "multipart/appledouble");

        filter.filter(dip);

        verify(idb).setContentType(listCaptor.capture());
        assertEquals("^unknown,Unknown", listCaptor.getValue().get(0));
        assertEquals("/unknown^pdf,pdf", listCaptor.getValue().get(1));
    }

    @Test
    public void testImageJpg() throws Exception {
        mockFile("picture.jpg", "image/jpg");

        filter.filter(dip);

        verify(idb).setContentType(listCaptor.capture());
        assertEquals("^image,Image", listCaptor.getValue().get(0));
        assertEquals("/image^jpg,jpg", listCaptor.getValue().get(1));
    }

    @Test
    public void testVideoMp4() throws Exception {
        mockFile("my_video", "video/mp4");

        filter.filter(dip);

        verify(idb).setContentType(listCaptor.capture());
        assertEquals("^video,Video", listCaptor.getValue().get(0));
        assertEquals("/video^mp4,mp4", listCaptor.getValue().get(1));
    }

    @Test
    public void testAudioWav() throws Exception {
        mockFile("sound_file.wav", "audio/wav");

        filter.filter(dip);

        verify(idb).setContentType(listCaptor.capture());
        assertEquals("^audio,Audio", listCaptor.getValue().get(0));
        assertEquals("/audio^wav,wav", listCaptor.getValue().get(1));
    }

    @Test
    public void testNoMimetype() throws Exception {
        mockFile("unidentified.stuff", null);

        filter.filter(dip);

        verify(idb).setContentType(listCaptor.capture());
        assertEquals("^unknown,Unknown", listCaptor.getValue().get(0));
        assertEquals("/unknown^stuff,stuff", listCaptor.getValue().get(1));
    }

    @Test
    public void testExtensionTooLongFallbackToMimetype() throws Exception {
        mockFile("unidentified.superlongextension", "text/plain");

        filter.filter(dip);

        verify(idb).setContentType(listCaptor.capture());
        assertEquals("^text,Text", listCaptor.getValue().get(0));
        assertEquals("/text^txt,txt", listCaptor.getValue().get(1));
    }

    @Test
    public void testExtensionTooLongNoFallback() throws Exception {
        mockFile("unidentified.superlongextension", "application/boxc-stuff");

        filter.filter(dip);

        verify(idb).setContentType(listCaptor.capture());
        assertEquals("^unknown,Unknown", listCaptor.getValue().get(0));
        assertEquals("/unknown^unknown,unknown", listCaptor.getValue().get(1));
    }

    @Test
    public void testNoExtensionNoMimetype() throws Exception {
        mockFile("unidentified", null);

        filter.filter(dip);

        verify(idb).setContentType(listCaptor.capture());
        assertEquals("^unknown,Unknown", listCaptor.getValue().get(0));
        assertEquals("/unknown^unknown,unknown", listCaptor.getValue().get(1));
    }

    @Test
    public void testInvalidExtensionFallbackToMimetype() throws Exception {
        mockFile("unidentified.20210401", "text/plain");

        filter.filter(dip);

        verify(idb).setContentType(listCaptor.capture());
        assertEquals("^text,Text", listCaptor.getValue().get(0));
        assertEquals("/text^txt,txt", listCaptor.getValue().get(1));
    }


    private void mockFile(String filename, String mimetype) {
        when(dip.getContentObject()).thenReturn(fileObj);
        when(fileObj.getOriginalFile()).thenReturn(binObj);
        when(binObj.getFilename()).thenReturn(filename);
        when(binObj.getMimetype()).thenReturn(mimetype);
    }
}
