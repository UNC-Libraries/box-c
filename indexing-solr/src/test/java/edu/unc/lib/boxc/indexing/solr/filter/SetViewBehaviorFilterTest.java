package edu.unc.lib.boxc.indexing.solr.filter;

import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackageDataLoader;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.rdf.CdrView;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.viewSettings.ViewSettingRequest;
import edu.unc.lib.boxc.search.solr.models.IndexDocumentBean;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class SetViewBehaviorFilterTest {
    private static final String WORK_UUID = "f277bb38-272c-471c-a28a-9887a1328a1f";
    private AutoCloseable closeable;
    @Mock
    private DocumentIndexingPackageDataLoader documentIndexingPackageDataLoader;
    @Mock
    private Resource resource;
    @Mock
    private Statement stmt;
    private SetViewBehaviorFilter filter;
    private DocumentIndexingPackage dip;
    private IndexDocumentBean idb;
    private PID workPid;

    @BeforeEach
    public void setup() {
        closeable = openMocks(this);
        workPid = PIDs.get(WORK_UUID);
        dip = new DocumentIndexingPackage(workPid, null, documentIndexingPackageDataLoader);
        dip.setPid(workPid);
        idb = dip.getDocument();
        filter = new SetViewBehaviorFilter();
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testWithWorkObject() {
        var work = mock(WorkObject.class);
        var behavior = ViewSettingRequest.ViewBehavior.PAGED.getString();
        when(documentIndexingPackageDataLoader.getContentObject(dip)).thenReturn(work);
        when(work.getResource()).thenReturn(resource);
        when(resource.hasProperty(eq(CdrView.viewBehavior))).thenReturn(true);
        when(resource.getProperty(eq(CdrView.viewBehavior))).thenReturn(stmt);
        when(stmt.getString()).thenReturn(behavior);

        filter.filter(dip);

        assertEquals(behavior, idb.getViewBehavior());
    }

    @Test
    public void testWithWorkObjectWithoutViewBehavior() {
        var work = mock(WorkObject.class);
        when(documentIndexingPackageDataLoader.getContentObject(dip)).thenReturn(work);
        when(work.getResource()).thenReturn(resource);
        when(resource.hasProperty(eq(CdrView.viewBehavior))).thenReturn(false);

        filter.filter(dip);

        assertNull(idb.getViewBehavior());
    }
}
