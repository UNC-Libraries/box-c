package edu.unc.lib.boxc.indexing.solr.filter;

import edu.unc.lib.boxc.common.util.DateTimeUtil;
import edu.unc.lib.boxc.indexing.solr.exception.IndexingException;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.rdf.Fcrepo4Repository;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.search.solr.models.IndexDocumentBean;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

/*
 * @author harring
 */
public class SetRecordDatesFilterTest {

    private static final String DATE_ADDED = "2017-01-01T00:00:00.000Z";
    private static final String DATE_MODIFIED = "2017-05-31T00:00:00.000Z";
    private static final String BAD_DATE = "abcd";

    private AutoCloseable closeable;

    @Mock
    private DocumentIndexingPackage dip;
    private PID pid;
    @Mock
    private ContentObject contentObj;
    private Resource resource;

    private IndexDocumentBean idb;
    private SetRecordDatesFilter filter;

    @BeforeEach
    public void setup() throws Exception {
        idb = new IndexDocumentBean();
        closeable = openMocks(this);

        pid = TestHelper.makePid();
        when(dip.getDocument()).thenReturn(idb);
        when(dip.getPid()).thenReturn(pid);
        when(dip.getContentObject()).thenReturn(contentObj);
        Model model = ModelFactory.createDefaultModel();
        resource = model.getResource(pid.getRepositoryPath());
        resource.addProperty(Fcrepo4Repository.created, DATE_ADDED);
        resource.addProperty(Fcrepo4Repository.lastModified, DATE_MODIFIED);
        when(contentObj.getResource()).thenReturn(resource);

        filter = new SetRecordDatesFilter();
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testCreateDate() throws Exception {
        filter.filter(dip);
        assertEquals(DATE_ADDED, DateTimeUtil.formatDateToUTC(idb.getDateAdded()));
    }

    @Test
    public void testUpdateDate() throws Exception {
        filter.filter(dip);
        assertEquals(DATE_MODIFIED, DateTimeUtil.formatDateToUTC(idb.getDateUpdated()));
    }

    @Test
    public void testUnparseableDate() throws Exception {
        Exception expected = Assertions.assertThrows(IndexingException.class, () -> {
            resource.removeAll(Fcrepo4Repository.lastModified);
            resource.addProperty(Fcrepo4Repository.lastModified, BAD_DATE);

            filter.filter(dip);
        });
        // checks that the exception message contains the substring param
        assertTrue(expected.getMessage().contains("Failed to parse record dates from "));
    }

}
