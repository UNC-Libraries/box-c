package edu.unc.lib.boxc.indexing.solr.filter;

import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackageDataLoader;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.search.solr.models.IndexDocumentBean;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static edu.unc.lib.boxc.indexing.solr.test.MockRepositoryObjectHelpers.makeFileResource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class SetStreamingUrlFilterTest {
    private static final String FILE_UUID = "f277bb38-272c-471c-a28a-9887a1328a1f";
    private AutoCloseable closeable;
    @Mock
    private DocumentIndexingPackageDataLoader documentIndexingPackageDataLoader;
    private Resource resource;
    private SetStreamingUrlFilter filter;
    private DocumentIndexingPackage dip;
    private IndexDocumentBean idb;

    @BeforeEach
    public void setup() {
        closeable = openMocks(this);
        PID filePid = PIDs.get(FILE_UUID);
        dip = new DocumentIndexingPackage(filePid, null, documentIndexingPackageDataLoader);
        dip.setPid(filePid);
        idb = dip.getDocument();
        filter = new SetStreamingUrlFilter();
        resource = makeFileResource(FILE_UUID);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testWithFileObject() {
        var file = mock(FileObject.class);
        var url = "https://streaming.url";
        when(documentIndexingPackageDataLoader.getContentObject(dip)).thenReturn(file);
        when(file.getResource()).thenReturn(resource);
        resource.addProperty(Cdr.streamingUrl, url);

        filter.filter(dip);

        assertEquals(url, idb.getStreamingUrl());
    }

    @Test
    public void testWithFileObjectWithoutStreamingUrl() {
        var file = mock(FileObject.class);
        when(documentIndexingPackageDataLoader.getContentObject(dip)).thenReturn(file);
        when(file.getResource()).thenReturn(resource);

        filter.filter(dip);

        assertNull(idb.getStreamingUrl());
    }
}
