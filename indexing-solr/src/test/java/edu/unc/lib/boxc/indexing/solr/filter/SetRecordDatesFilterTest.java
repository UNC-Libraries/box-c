package edu.unc.lib.boxc.indexing.solr.filter;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.text.SimpleDateFormat;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import edu.unc.lib.boxc.indexing.solr.exception.IndexingException;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackageDataLoader;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.rdf.Fcrepo4Repository;
import edu.unc.lib.boxc.search.solr.models.IndexDocumentBean;
import edu.unc.lib.boxc.model.api.objects.ContentObject;

/*
 * @author harring
 */
public class SetRecordDatesFilterTest {

    private static final String DATE_ADDED = "2017-01-01";
    private static final String DATE_MODIFIED = "2017-05-31";
    private static final String BAD_DATE = "abcd";

    private AutoCloseable closeable;

    @Mock
    private DocumentIndexingPackageDataLoader loader;
    @Mock
    private DocumentIndexingPackage dip;
    @Mock
    private PID pid;
    @Mock
    private ContentObject contentObj;
    @Mock
    private Resource resource;
    @Mock
    private Statement stmt1;
    @Mock
    private Statement stmt2;
    @Mock
    private Literal literal1;
    @Mock
    private Literal literal2;
    @Mock
    private Object object1;
    @Mock
    private Object object2;

    private IndexDocumentBean idb;
    private SetRecordDatesFilter filter;

    @BeforeEach
    public void setup() throws Exception {
        idb = new IndexDocumentBean();
        closeable = openMocks(this);

        when(dip.getDocument()).thenReturn(idb);
        when(dip.getPid()).thenReturn(pid);
        when(dip.getContentObject()).thenReturn(contentObj);

        when(contentObj.getResource()).thenReturn(resource);

        when(resource.getProperty(Fcrepo4Repository.created))
            .thenReturn(stmt1);
        when(stmt1.getLiteral()).thenReturn(literal1);
        when(literal1.getValue()).thenReturn(object1);
        when(object1.toString()).thenReturn(DATE_ADDED);
        when(resource.getProperty(Fcrepo4Repository.lastModified))
            .thenReturn(stmt2);
        when(stmt2.getLiteral()).thenReturn(literal2);
        when(literal2.getValue()).thenReturn(object2);
        when(object2.toString()).thenReturn(DATE_MODIFIED);

        filter = new SetRecordDatesFilter();
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testCreateDate() throws Exception {
        filter.filter(dip);
        assertEquals(DATE_ADDED, new SimpleDateFormat("yyyy-MM-dd").format(idb.getDateAdded()));
    }

    @Test
    public void testUpdateDate() throws Exception {
        filter.filter(dip);
        assertEquals(DATE_MODIFIED, new SimpleDateFormat("yyyy-MM-dd").format(idb.getDateUpdated()));
    }

    @Test
    public void testUnparseableDate() throws Exception {
        Exception expected = Assertions.assertThrows(IndexingException.class, () -> {
            when(object2.toString()).thenReturn(BAD_DATE);

            filter.filter(dip);
        });
        // checks that the exception message contains the substring param
        assertTrue(expected.getMessage().contains("Failed to parse record dates from "));
    }

}
