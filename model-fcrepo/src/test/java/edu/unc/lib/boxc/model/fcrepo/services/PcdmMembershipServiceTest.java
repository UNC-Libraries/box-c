package edu.unc.lib.boxc.model.fcrepo.services;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.rdf.PcdmModels;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoResponse;
import org.fcrepo.client.GetBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

/**
 * @author bbpennel
 */
public class PcdmMembershipServiceTest {
    private static final String PARENT_UUID = "f277bb38-272c-471c-a28a-9887a1328a1f";
    private static final String MEMBER1_UUID = "83c2d7f8-2e6b-4f0b-ab7e-7397969c0682";
    private static final String MEMBER2_UUID = "0e33ad0b-7a16-4bfa-b833-6126c262d889";
    private static final String MEMBER3_UUID = "9cb6cc61-d88e-403e-b959-2396cd331a12";
    private PID parentPid;
    private PID member1Pid;
    private PID member2Pid;
    private PID member3Pid;
    private Model model;
    private AutoCloseable closeable;
    @Mock
    private FcrepoClient fcrepoClient;
    private PcdmMembershipService membershipService;

    @BeforeEach
    public void setup() {
        closeable = openMocks(this);
        model = ModelFactory.createDefaultModel();
        membershipService = new PcdmMembershipService();
        membershipService.setClient(fcrepoClient);

        parentPid = PIDs.get(PARENT_UUID);
        member1Pid = PIDs.get(MEMBER1_UUID);
        member2Pid = PIDs.get(MEMBER2_UUID);
        member3Pid = PIDs.get(MEMBER3_UUID);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void listMembersTest() throws Exception {
        var parentResc = model.getResource(parentPid.getRepositoryPath());
        var member1Resc = model.getResource(member1Pid.getRepositoryPath());
        var member2Resc = model.getResource(member2Pid.getRepositoryPath());
        var member3Resc = model.getResource(member3Pid.getRepositoryPath());
        member1Resc.addProperty(PcdmModels.memberOf, parentResc);
        member2Resc.addProperty(PcdmModels.memberOf, parentResc);
        member3Resc.addProperty(PcdmModels.memberOf, member1Resc);

        mockNTriplesResponse(parentPid, model);

        // Only returns immediate children, so member3Resc is not listed
        var members = membershipService.listMembers(parentPid);
        assertEquals(2, members.size());
        assertTrue(members.contains(member1Pid));
        assertTrue(members.contains(member2Pid));
    }

    @Test
    public void listMembersEmptyTest() throws Exception {
        var parentResc = model.getResource(parentPid.getRepositoryPath());
        var member1Resc = model.getResource(member1Pid.getRepositoryPath());
        member1Resc.addProperty(PcdmModels.memberOf, parentResc);

        mockNTriplesResponse(member1Pid, model);

        var members = membershipService.listMembers(member1Pid);
        assertTrue(members.isEmpty());
    }

    private void mockNTriplesResponse(PID requestPid, Model model) throws Exception {
        var response = mock(FcrepoResponse.class);
        var get = mock(GetBuilder.class);

        var out = new ByteArrayOutputStream();
        RDFDataMgr.write(out, model, Lang.NTRIPLES);
        var inputStream = new ByteArrayInputStream(out.toByteArray());

        when(fcrepoClient.get(eq(requestPid.getRepositoryUri()))).thenReturn(get);
        when(get.accept(any())).thenReturn(get);
        when(get.preferRepresentation(any(), any())).thenReturn(get);
        when(get.perform()).thenReturn(response);
        when(response.getBody()).thenReturn(inputStream);
    }
}
