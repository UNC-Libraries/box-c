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

import static edu.unc.lib.boxc.indexing.solr.test.MockRepositoryObjectHelpers.makeCollectionObject;
import static edu.unc.lib.boxc.indexing.solr.test.MockRepositoryObjectHelpers.makePid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class SetCollectionDisplayPropertiesFilterTest {
    private AutoCloseable closeable;
    @Mock
    private DocumentIndexingPackageDataLoader documentIndexingPackageDataLoader;
    private SetCollectionDisplayPropertiesFilter filter;
    private DocumentIndexingPackage dip;
    private IndexDocumentBean idb;
    private PID collectionPid;

    @BeforeEach
    public void setup() {
        closeable = openMocks(this);
        collectionPid = makePid();
        dip = new DocumentIndexingPackage(collectionPid, null, documentIndexingPackageDataLoader);
        dip.setPid(collectionPid);
        idb = dip.getDocument();
        filter = new SetCollectionDisplayPropertiesFilter();
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testWithFileObject() {
        var collection =  makeCollectionObject(collectionPid, null);
        var settings = "{\"sortType\": \"default,normal\", \"displayType\": \"gallery-display\", \"worksOnly\": \"true\"}";
        when(documentIndexingPackageDataLoader.getContentObject(dip)).thenReturn(collection);
        var resource = collection.getResource();
        resource.addProperty(Cdr.collectionDefaultDisplaySettings, settings);

        filter.filter(dip);

        assertEquals(settings, idb.getCollectionDisplaySettings());
    }

    @Test
    public void testWithFileObjectWithoutStreamingProperties() {
        var collection = makeCollectionObject(collectionPid, null);
        when(documentIndexingPackageDataLoader.getContentObject(dip)).thenReturn(collection);

        filter.filter(dip);

        assertNull(idb.getCollectionDisplaySettings());
    }
}
