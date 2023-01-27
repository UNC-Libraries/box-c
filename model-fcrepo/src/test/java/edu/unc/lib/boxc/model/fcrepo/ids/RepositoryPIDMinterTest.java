package edu.unc.lib.boxc.model.fcrepo.ids;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.ids.PIDMinter;
import edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants;
import edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPIDMinter;

/**
 *
 * @author harring
 *
 */
public class RepositoryPIDMinterTest {
    private PIDMinter pidMinter;

    @BeforeEach
    public void init() {
        pidMinter = new RepositoryPIDMinter();
    }

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
        assertTrue(pid.getRepositoryPath().matches(".*/event\\d+$"));
    }

}
