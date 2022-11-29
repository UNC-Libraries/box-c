package edu.unc.lib.boxc.model.fcrepo.services;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.rdf.PcdmModels;
import edu.unc.lib.boxc.model.api.sparql.SparqlQueryService;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.sparql.JenaSparqlQueryServiceImpl;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
    private SparqlQueryService sparqlQueryService;
    private PcdmMembershipService membershipService;

    @Before
    public void setup() {
        model = ModelFactory.createDefaultModel();
        sparqlQueryService = new JenaSparqlQueryServiceImpl(model);
        membershipService = new PcdmMembershipService();
        membershipService.setSparqlQueryService(sparqlQueryService);

        parentPid = PIDs.get(PARENT_UUID);
        member1Pid = PIDs.get(MEMBER1_UUID);
        member2Pid = PIDs.get(MEMBER2_UUID);
        member3Pid = PIDs.get(MEMBER3_UUID);
    }

    @Test
    public void listMembersTest() {
        var parentResc = model.getResource(parentPid.getRepositoryPath());
        var member1Resc = model.getResource(member1Pid.getRepositoryPath());
        var member2Resc = model.getResource(member2Pid.getRepositoryPath());
        var member3Resc = model.getResource(member3Pid.getRepositoryPath());
        member1Resc.addProperty(PcdmModels.memberOf, parentResc);
        member2Resc.addProperty(PcdmModels.memberOf, parentResc);
        member3Resc.addProperty(PcdmModels.memberOf, member1Resc);

        // Only returns immediate children, so member3Resc is not listed
        var members = membershipService.listMembers(parentPid);
        assertEquals(2, members.size());
        assertTrue(members.contains(member1Pid));
        assertTrue(members.contains(member2Pid));
    }

    @Test
    public void listMembersEmptyTest() {
        var parentResc = model.getResource(parentPid.getRepositoryPath());
        var member1Resc = model.getResource(member1Pid.getRepositoryPath());
        member1Resc.addProperty(PcdmModels.memberOf, parentResc);

        var members = membershipService.listMembers(member1Pid);
        assertTrue(members.isEmpty());
    }
}
