package edu.unc.lib.dl.cdr.services.processing;

import edu.unc.lib.dl.acl.exception.AccessRestrictionException;
import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.services.OperationsMessageSender;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static edu.unc.lib.dl.acl.util.Permission.runEnhancements;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class RunEnhancementsServiceTest {
    @Mock
    private AccessControlService aclService;
    @Mock
    private AgentPrincipals agent;
    @Mock
    private AccessGroupSet groups;
    @Mock
    private OperationsMessageSender messageSender;

    private RunEnhancementsService service;
    private PID pid;
    private ArrayList pids;

    @Before
    public void init() {
        initMocks(this);
        pid = PIDs.get(UUID.randomUUID().toString());
        pids = new ArrayList<>();
        HashMap<String, Object> values = new HashMap<>();
        values.put("pid", pid);
        values.put("objectType", "Folder");
        pids.add(values);

        when(agent.getPrincipals()).thenReturn(groups);
        service = new RunEnhancementsService();
        service.setAclService(aclService);
    }

    @Test(expected = AccessRestrictionException.class)
    public void insufficientAccessTest() {
        doThrow(new AccessRestrictionException()).when(aclService)
                .assertHasAccess(anyString(), eq(pid), any(AccessGroupSet.class), eq(runEnhancements));
        service.run(agent, pids, true);
    }

    @Test
    public void messageSent() {
        service.run(agent, pids, true);
    }
}
