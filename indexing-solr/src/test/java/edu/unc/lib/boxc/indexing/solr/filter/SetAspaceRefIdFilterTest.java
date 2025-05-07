package edu.unc.lib.boxc.indexing.solr.filter;

import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackageDataLoader;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.CdrAspace;
import edu.unc.lib.boxc.search.solr.models.IndexDocumentBean;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static edu.unc.lib.boxc.indexing.solr.test.MockRepositoryObjectHelpers.makeFileObject;
import static edu.unc.lib.boxc.indexing.solr.test.MockRepositoryObjectHelpers.makePid;
import static edu.unc.lib.boxc.indexing.solr.test.MockRepositoryObjectHelpers.makeResource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class SetAspaceRefIdFilterTest {
    private AutoCloseable closeable;
    private DocumentIndexingPackage dip;
    private IndexDocumentBean idb;
    private Resource resource;
    private SetAspaceRefIdFilter filter;
    private PID workPid;
    @Mock
    private DocumentIndexingPackageDataLoader documentIndexingPackageDataLoader;
    @Mock
    private WorkObject workObj;

    @BeforeEach
    public void setup() {
        closeable = openMocks(this);
        workPid = makePid();
        dip = new DocumentIndexingPackage(workPid, null, documentIndexingPackageDataLoader);
        dip.setPid(workPid);
        idb = dip.getDocument();
        filter = new SetAspaceRefIdFilter();
        resource = makeResource(workPid, Cdr.Work);
        when(workObj.getResource()).thenReturn(resource);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void addRefIdToWorkObject() {
        var id = "2817ec3c77e5ea9846d5c070d58d402b";
        dip.setContentObject(workObj);
        when(documentIndexingPackageDataLoader.getContentObject(dip)).thenReturn(workObj);
        resource.addProperty(CdrAspace.refId, id);

        filter.filter(dip);

        assertEquals(id, idb.getAspaceRefId());
    }

    @Test
    public void noRefIdOnWorkObject() {
        dip.setContentObject(workObj);
        when(documentIndexingPackageDataLoader.getContentObject(dip)).thenReturn(workObj);
        filter.filter(dip);

        assertNull(idb.getAspaceRefId());
    }

    @Test
    public void addRefIdToNonWorkObject() {
        var filePid = makePid();
        var file = makeFileObject(filePid, null);
        when(documentIndexingPackageDataLoader.getContentObject(dip)).thenReturn(file);
        filter.filter(dip);

        assertNull(idb.getAspaceRefId());
    }
}
