package edu.unc.lib.boxc.operations.jms.indexing;
import static edu.unc.lib.boxc.auth.api.Permission.reindex;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingService;

/**
 *
 * @author harring
 *
 */
public class IndexingServiceTest {
    private static final String USERNAME = "username";

    @Mock
    private AccessControlService aclService;
    @Mock
    private IndexingMessageSender messageSender;
    @Mock
    private AgentPrincipals agent;
    @Mock
    private AccessGroupSet groups;
    @Mock
    private ContentObject obj;

    @Captor
    private ArgumentCaptor<PID> pidCaptor;
    @Captor
    private ArgumentCaptor<String> stringCaptor;
    @Captor
    private ArgumentCaptor<IndexingActionType> actionCaptor;

    private IndexingService service;
    private PID objPid;

    @Before
    public void init() throws Exception{
        initMocks(this);

        when(agent.getPrincipals()).thenReturn(groups);
        when(agent.getUsername()).thenReturn(USERNAME);

        objPid = PIDs.get(UUID.randomUUID().toString());

        service = new IndexingService();
        service.setAclService(aclService);
        service.setIndexingMessageSender(messageSender);
    }

    @After
    public void tearDown() {
        GroupsThreadStore.clearStore();
    }

    @Test
    public void reindexObjectTest() {
        service.reindexObject(agent, objPid);

        verify(messageSender).sendIndexingOperation(stringCaptor.capture(), pidCaptor.capture(),
                actionCaptor.capture());

        verifyParameters(IndexingActionType.ADD);
    }

    @Test
    public void inplaceReindexObjectAndChildrenTest() {
        service.reindexObjectAndChildren(agent, objPid, true);

        verify(messageSender).sendIndexingOperation(stringCaptor.capture(), pidCaptor.capture(),
                actionCaptor.capture());

        verifyParameters(IndexingActionType.RECURSIVE_REINDEX);
    }

    @Test
    public void cleanReindexObjectAndChildrenTest() {
        service.reindexObjectAndChildren(agent, objPid, false);

        verify(messageSender).sendIndexingOperation(stringCaptor.capture(), pidCaptor.capture(),
                actionCaptor.capture());

        verifyParameters(IndexingActionType.CLEAN_REINDEX);
    }

    @Test(expected = AccessRestrictionException.class)
    public void insufficientAccessTest() {
        doThrow(new AccessRestrictionException()).when(aclService)
                .assertHasAccess(anyString(), any(PID.class), any(), eq(reindex));

        service.reindexObject(agent, objPid);
    }

    private void verifyParameters(IndexingActionType expectedActionType) {
        assertEquals(objPid, pidCaptor.getValue());
        assertEquals(agent.getUsername(), stringCaptor.getValue());
        assertEquals(expectedActionType, actionCaptor.getValue());
    }

}
