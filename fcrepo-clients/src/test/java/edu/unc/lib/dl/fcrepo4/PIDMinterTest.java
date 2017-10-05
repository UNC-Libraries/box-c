package edu.unc.lib.dl.fcrepo4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import edu.unc.lib.dl.fedora.PID;

public class PIDMinterTest extends AbstractFedoraIT {


    @Test
    public void mintDepositRecordPidTest() {
        PID pid = pidMinter.mintDepositRecordPid();

        assertEquals(RepositoryPathConstants.DEPOSIT_RECORD_BASE, pid.getQualifier());
        assertTrue(pid.getQualifiedId().startsWith(RepositoryPathConstants.DEPOSIT_RECORD_BASE));
    }

    @Test
    public void mintContentPidTest() {
        PID pid = pidMinter.mintContentPid();

        assertEquals(RepositoryPathConstants.CONTENT_BASE, pid.getQualifier());
        assertTrue(pid.getQualifiedId().startsWith(RepositoryPathConstants.CONTENT_BASE));
    }

    @Test
    public void mintPremisEventPidTest() {
        PID parentPid = pidMinter.mintContentPid();
        PID pid = pidMinter.mintPremisEventPid(parentPid);

        assertEquals(RepositoryPathConstants.CONTENT_BASE, pid.getQualifier());
        assertTrue(pid.getQualifiedId().startsWith(RepositoryPathConstants.CONTENT_BASE));
    }

}
