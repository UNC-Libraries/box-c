package edu.unc.lib.boxc.indexing.solr.filter;

import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackageDataLoader;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.search.solr.models.IndexDocumentBean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static edu.unc.lib.boxc.indexing.solr.test.MockRepositoryObjectHelpers.makeFileObject;
import static edu.unc.lib.boxc.indexing.solr.test.MockRepositoryObjectHelpers.makePid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class SetStreamingPropertiesFilterTest {
    private AutoCloseable closeable;
    @Mock
    private DocumentIndexingPackageDataLoader documentIndexingPackageDataLoader;
    private SetStreamingPropertiesFilter filter;
    private DocumentIndexingPackage dip;
    private IndexDocumentBean idb;
    private PID filePid;

    @BeforeEach
    public void setup() {
        closeable = openMocks(this);
        filePid = makePid();
        dip = new DocumentIndexingPackage(filePid, null, documentIndexingPackageDataLoader);
        dip.setPid(filePid);
        idb = dip.getDocument();
        filter = new SetStreamingPropertiesFilter();
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testWithFileObject() {
        var file =  makeFileObject(filePid, null);
        var url = "https://streaming.url";
        var type = "video";
        when(documentIndexingPackageDataLoader.getContentObject(dip)).thenReturn(file);
        var resource = file.getResource();
        resource.addProperty(Cdr.streamingUrl, url);
        resource.addProperty(Cdr.streamingType, type);

        filter.filter(dip);

        assertEquals(url, idb.getStreamingUrl());
        assertEquals(type, idb.getStreamingType());
    }

    @Test
    public void testWithFileObjectWithoutStreamingProperties() {
        var file = makeFileObject(filePid, null);
        when(documentIndexingPackageDataLoader.getContentObject(dip)).thenReturn(file);

        filter.filter(dip);

        assertNull(idb.getStreamingUrl());
        assertNull(idb.getStreamingType());
    }
}
