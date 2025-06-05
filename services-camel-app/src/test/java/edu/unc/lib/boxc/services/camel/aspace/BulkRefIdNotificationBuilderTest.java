package edu.unc.lib.boxc.services.camel.aspace;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.aspace.BulkRefIdRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BulkRefIdNotificationBuilderTest {
    private static final String WORK1_UUID = "f277bb38-272c-471c-a28a-9887a1328a1f";
    private static final String WORK2_UUID = "75e29173-2e4d-418d-a9a6-caa68413edaf";
    private static final String REF1_ID = "refid1";
    private static final String REF2_ID = "refid2";
    private static final String EMAIL = "user1@example.com";
    private static final String USERNAME = "user1";
    private AgentPrincipals agent = new AgentPrincipalsImpl(USERNAME, new AccessGroupSetImpl("agroup"));
    private BulkRefIdRequest request = new BulkRefIdRequest();
    private BulkRefIdNotificationBuilder builder;

    @BeforeEach
    public void setup(){
        var refIdMap = Map.of(WORK1_UUID, REF1_ID, WORK2_UUID, REF2_ID);
        request.setEmail(EMAIL);
        request.setAgent(agent);
        request.setRefIdMap(refIdMap);
        builder = new BulkRefIdNotificationBuilder();
    }

    @Test
    public void constructEmailWithNoErrors() {
        var successes = Arrays.asList(PIDs.get(WORK1_UUID), PIDs.get(WORK2_UUID));
        var errors = new ArrayList<String>();
        var expected = "Here are the results of your BulkRefId update request.\n" +
                "Number of updates requested: 2\n" +
                "Number successfully updated: 2\n" +
                "There were no errors.";

        assertEquals(expected, builder.construct(request, successes, errors));
    }

    @Test
    public void constructEmailWithErrorsAndNoSuccesses() {
        var successes = new ArrayList<PID>();
        var errors = Arrays.asList("First error", "Another error oh no");
        var expected = "Here are the results of your BulkRefId update request.\n" +
                "Number of updates requested: 2\n" +
                "Number successfully updated: 0\n" +
                "There were the following errors:\n" +
                "-- First error\n" +
                "-- Another error oh no\n";

        assertEquals(expected, builder.construct(request, successes, errors));
    }

    @Test
    public void constructEmailWithOneErrorAndOneSuccess() {
        var successes = List.of(PIDs.get(WORK1_UUID));
        var errors = List.of("Second Work error");
        var expected = "Here are the results of your BulkRefId update request.\n" +
                "Number of updates requested: 2\n" +
                "Number successfully updated: 1\n" +
                "There were the following errors:\n" +
                "-- Second Work error\n";

        assertEquals(expected, builder.construct(request, successes, errors));
    }
}
