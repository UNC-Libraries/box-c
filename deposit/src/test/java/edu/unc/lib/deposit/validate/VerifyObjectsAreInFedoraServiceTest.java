package edu.unc.lib.deposit.validate;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;

public class VerifyObjectsAreInFedoraServiceTest {
    private VerifyObjectsAreInFedoraService verificationService = new VerifyObjectsAreInFedoraService();

    @Test
    public void listObjectPIDsTest() {
        PID depositPid = makePid();
        PID obj1 = makePid();
        PID obj2 = makePid();
        List<PID> objPids = new ArrayList<>();
        objPids.add(obj1);
        objPids.add(obj2);

        assertEquals(
                "The following objects from deposit " + depositPid.getQualifiedId() + " did not make it to Fedora:\n"
                        + obj1.toString() + "\n" + obj2.toString() + "\n",
                verificationService.listObjectPIDs(depositPid.getQualifiedId(), objPids));
    }

    private PID makePid() {
        return PIDs.get(UUID.randomUUID().toString());
    }

}
