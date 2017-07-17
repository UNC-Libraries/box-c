package edu.unc.lib.dl.data.ingest.solr.filter;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import edu.unc.lib.dl.acl.fcrepo4.InheritedAclFactory;
import edu.unc.lib.dl.acl.fcrepo4.ObjectAclFactory;
import edu.unc.lib.dl.acl.service.PatronAccess;
import edu.unc.lib.dl.acl.util.AccessPrincipalConstants;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;
import edu.unc.lib.dl.search.solr.util.FacetConstants;

public class SetAccessStatusFilterTest {

	private static final String PID_STRING = "uuid:07d9594f-310d-4095-ab67-79a1056e7430";

    private static final String PRINC1 = "group1";
    private static final String PRINC2 = "group2";

    @Mock
    private DocumentIndexingPackage dip;
    @Mock
    private IndexDocumentBean idb;
    @Mock
    private InheritedAclFactory inheritedAclFactory;
    @Mock
    private ObjectAclFactory objAclFactory;
    @Mock
    private ContentObject contentObj;
    @Mock
    private Date date;

    @Mock
    private PID pid;

    @Captor
    private ArgumentCaptor<List<String>> listCaptor;

    private Map<String, Set<String>> principalRoles;

    private SetAccessStatusFilter filter;

    @Before
    public void setup() throws Exception {
        initMocks(this);

        when(pid.toString()).thenReturn(PID_STRING);

        when(dip.getDocument()).thenReturn(idb);
        when(dip.getPid()).thenReturn(pid);
        when(dip.getContentObject()).thenReturn(contentObj);
        when(contentObj.getPid()).thenReturn(pid);

        when(inheritedAclFactory.isMarkedForDeletion(any(PID.class))).thenReturn(true);
        when(inheritedAclFactory.getEmbargoUntil(any(PID.class))).thenReturn(date);

        principalRoles = new HashMap<>();
        principalRoles.put(AccessPrincipalConstants.PUBLIC_PRINC, new HashSet<String>());
        when(inheritedAclFactory.getPrincipalRoles(any(PID.class))).thenReturn(principalRoles);

        filter = new SetAccessStatusFilter();
        filter.setObjectAclFactory(objAclFactory);
        filter.setInheritedAclFactory(inheritedAclFactory);
    }


	@Test
	public void testIsMarkedForDeletion() throws Exception {
		when(inheritedAclFactory.getPatronAccess(any(PID.class))).thenReturn(PatronAccess.none);
		when(objAclFactory.getPatronAccess(any(PID.class))).thenReturn(PatronAccess.none);
		filter.filter(dip);

		verify(idb).setStatus(listCaptor.capture());
		assertTrue(listCaptor.getValue().contains(FacetConstants.MARKED_FOR_DELETION));
	}

}
