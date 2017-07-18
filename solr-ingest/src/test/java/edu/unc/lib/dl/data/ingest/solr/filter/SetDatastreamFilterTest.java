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

import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.LARGE_THUMBNAIL;
import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.ORIGINAL_FILE;
import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.TECHNICAL_METADATA;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.fcrepo4.BinaryObject;
import edu.unc.lib.dl.fcrepo4.FileObject;
import edu.unc.lib.dl.fcrepo4.FolderObject;
import edu.unc.lib.dl.fcrepo4.WorkObject;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Ebucore;
import edu.unc.lib.dl.rdf.Premis;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;

/**
 * 
 * @author bbpennel
 *
 */
public class SetDatastreamFilterTest {

    private static final String BASE_URI = "http://example.com/rest/";

    private static final String PID_STRING = "uuid:07d9594f-310d-4095-ab67-79a1056e7430";

    private static final String FILE_MIMETYPE = "text/plain";
    private static final String FILE_NAME = "test.txt";
    private static final String FILE_DIGEST = "urn:sha1:82022e1782b92dce5461ee636a6c5bea8509ffee";
    private static final long FILE_SIZE = 5062l;

    private static final String FILE2_MIMETYPE = "text/xml";
    private static final String FILE2_NAME = "fits.xml";
    private static final String FILE2_DIGEST = "urn:sha1:afbf62faf8a82d00969e0d4d965d62a45bb8c69b";
    private static final long FILE2_SIZE = 7231l;

    private static final String FILE3_MIMETYPE = "image/png";
    private static final String FILE3_NAME = "image.png";
    private static final String FILE3_DIGEST = "urn:sha1:280f5922b6487c39d6d01a5a8e93bfa07b8f1740";
    private static final long FILE3_SIZE = 17136l;

    @Mock
    private DocumentIndexingPackage dip;
    @Mock
    private PID pid;

    @Mock
    private FileObject fileObj;
    @Mock
    private BinaryObject binObj;

    @Mock
    private IndexDocumentBean idb;
    @Captor
    private ArgumentCaptor<List<String>> listCaptor;

    private SetDatastreamFilter filter;

    @Before
    public void setup() throws Exception {
        initMocks(this);

        when(pid.getId()).thenReturn(PID_STRING);

        when(dip.getDocument()).thenReturn(idb);
        when(dip.getPid()).thenReturn(pid);

//        when(workObj.getPrimaryObject()).thenReturn(fileObj);
        when(fileObj.getOriginalFile()).thenReturn(binObj);

        filter = new SetDatastreamFilter();

        when(fileObj.getBinaryObjects()).thenReturn(Arrays.asList(binObj));
        when(binObj.getResource()).thenReturn(
                fileResource(ORIGINAL_FILE, FILE_SIZE, FILE_MIMETYPE, FILE_NAME, FILE_DIGEST));
    }

    @Test
    public void fileObjectTest() throws Exception {
        when(dip.getContentObject()).thenReturn(fileObj);

        filter.filter(dip);

        verify(idb).setDatastream(listCaptor.capture());
        assertContainsDatastream(listCaptor.getValue(), ORIGINAL_FILE,
                FILE_SIZE, FILE_MIMETYPE, FILE_NAME, FILE_DIGEST, null);

        verify(idb).setFilesizeSort(eq(FILE_SIZE));
        verify(idb).setFilesizeTotal(eq(FILE_SIZE));
    }

    @Test
    public void fileObjectMultipleBinariesTest() throws Exception {
        BinaryObject binObj2 = mock(BinaryObject.class);
        when(binObj2.getResource()).thenReturn(
                fileResource(TECHNICAL_METADATA, FILE2_SIZE, FILE2_MIMETYPE, FILE2_NAME, FILE2_DIGEST));

        BinaryObject binObj3 = mock(BinaryObject.class);
        when(binObj3.getResource()).thenReturn(
                fileResource(LARGE_THUMBNAIL, FILE3_SIZE, FILE3_MIMETYPE, FILE3_NAME, FILE3_DIGEST));

        when(fileObj.getBinaryObjects()).thenReturn(Arrays.asList(binObj, binObj2, binObj3));
        when(dip.getContentObject()).thenReturn(fileObj);

        filter.filter(dip);

        verify(idb).setDatastream(listCaptor.capture());
        assertContainsDatastream(listCaptor.getValue(), ORIGINAL_FILE,
                FILE_SIZE, FILE_MIMETYPE, FILE_NAME, FILE_DIGEST, null);
        assertContainsDatastream(listCaptor.getValue(), TECHNICAL_METADATA,
                FILE2_SIZE, FILE2_MIMETYPE, FILE2_NAME, FILE2_DIGEST, null);
        assertContainsDatastream(listCaptor.getValue(), LARGE_THUMBNAIL,
                FILE3_SIZE, FILE3_MIMETYPE, FILE3_NAME, FILE3_DIGEST, null);

        verify(idb).setFilesizeSort(eq(FILE_SIZE));
        verify(idb).setFilesizeTotal(eq(FILE_SIZE + FILE2_SIZE + FILE3_SIZE));
    }

    @Test(expected = IndexingException.class)
    public void fileObjectNoOriginalTest() throws Exception {
        when(binObj.getResource()).thenReturn(
                fileResource(TECHNICAL_METADATA, FILE2_SIZE, FILE2_MIMETYPE, FILE2_NAME, FILE2_DIGEST));

        when(fileObj.getBinaryObjects()).thenReturn(Arrays.asList(binObj));
        when(dip.getContentObject()).thenReturn(fileObj);

        filter.filter(dip);
    }

    @Test
    public void workObjectTest() throws Exception {
        WorkObject workObj = mock(WorkObject.class);
        when(workObj.getPrimaryObject()).thenReturn(fileObj);
        when(workObj.getPid()).thenReturn(pid);

        when(dip.getContentObject()).thenReturn(workObj);

        String fileId = "055ed112-f548-479e-ab4b-bf1aad40d470";
        PID filePid = mock(PID.class);
        when(filePid.getId()).thenReturn(fileId);
        when(fileObj.getPid()).thenReturn(filePid);

        filter.filter(dip);

        verify(idb).setDatastream(listCaptor.capture());
        assertContainsDatastream(listCaptor.getValue(), ORIGINAL_FILE,
                FILE_SIZE, FILE_MIMETYPE, FILE_NAME, FILE_DIGEST, fileId);

        // Sort size is based off primary object's size
        verify(idb).setFilesizeSort(eq(FILE_SIZE));
        // Work has no datastreams of its own
        verify(idb).setFilesizeTotal(eq(0l));
    }

    @Test
    public void workObjectWithoutPrimaryObjectTest() throws Exception {
        WorkObject workObj = mock(WorkObject.class);

        when(dip.getContentObject()).thenReturn(workObj);

        filter.filter(dip);

        verify(idb, never()).setDatastream(anyListOf(String.class));
        verify(idb, never()).setFilesizeSort(anyLong());
        verify(idb, never()).setFilesizeTotal(anyLong());
    }

    @Test
    public void unsupportedObjectTest() throws Exception {
        FolderObject workObj = mock(FolderObject.class);

        when(dip.getContentObject()).thenReturn(workObj);

        filter.filter(dip);

        verify(idb, never()).setDatastream(anyListOf(String.class));
        verify(idb, never()).setFilesizeSort(anyLong());
        verify(idb, never()).setFilesizeTotal(anyLong());
    }

    private Resource fileResource(String name, long filesize, String mimetype, String filename, String digest) {
        Model model = ModelFactory.createDefaultModel();
        Resource resc = model.getResource(BASE_URI + name);
        resc.addLiteral(Premis.hasSize, filesize);
        resc.addLiteral(Ebucore.hasMimeType, mimetype);
        resc.addLiteral(Ebucore.filename, filename);
        resc.addProperty(Premis.hasMessageDigest, createResource(digest));

        return resc;
    }

    private void assertContainsDatastream(List<String> values, String name, long filesize, String mimetype, String filename, String digest, String owner) {
        String extension = filename.substring(filename.lastIndexOf('.') + 1);
        List<Object> components = Arrays.asList(
                name, mimetype, filename, extension, filesize, digest, owner);
        String joined = components.stream()
                .map(c -> c == null ? "" : c.toString())
                .collect(Collectors.joining("|"));
        assertTrue("Did not contain datastream " + name, values.contains(joined));
    }
}
