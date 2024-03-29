package edu.unc.lib.boxc.indexing.solr.filter;

import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackageDataLoader;
import edu.unc.lib.boxc.indexing.solr.utils.MemberOrderService;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.search.solr.models.IndexDocumentBean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

/**
 * @author bbpennel
 */
public class SetMemberOrderFilterTest {
    private static final String SUBJECT_UUID = "f277bb38-272c-471c-a28a-9887a1328a1f";

    private AutoCloseable closeable;

    @Mock
    private MemberOrderService memberOrderService;
    @Mock
    private DocumentIndexingPackageDataLoader documentIndexingPackageDataLoader;
    private SetMemberOrderFilter filter;

    private DocumentIndexingPackage dip;
    private IndexDocumentBean idb;
    private PID subjectPid;

    @BeforeEach
    public void setup() {
        closeable = openMocks(this);
        subjectPid = PIDs.get(SUBJECT_UUID);
        dip = new DocumentIndexingPackage(subjectPid, null, documentIndexingPackageDataLoader);
        dip.setPid(subjectPid);
        idb = dip.getDocument();
        filter = new SetMemberOrderFilter();
        filter.setMemberOrderService(memberOrderService);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void withFileInUnorderedWorkTest() {
        var subject = mock(FileObject.class);
        when(documentIndexingPackageDataLoader.getContentObject(dip)).thenReturn(subject);
        when(memberOrderService.getOrderValue(subject)).thenReturn(null);

        filter.filter(dip);

        assertNull(idb.getMemberOrderId());
    }

    @Test
    public void withFileInOrderedWorkTest() {
        Integer expected = Integer.valueOf(4);
        var subject = mock(FileObject.class);
        when(documentIndexingPackageDataLoader.getContentObject(dip)).thenReturn(subject);
        when(memberOrderService.getOrderValue(subject)).thenReturn(expected);

        filter.filter(dip);

        assertEquals(expected, idb.getMemberOrderId());
    }
}
