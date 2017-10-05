/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.fcrepo4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import edu.unc.lib.dl.fedora.PID;

/**
 *
 * @author harring
 *
 */
public class RepositoryPIDMinterTest {
    private RepositoryPIDMinter pidMinter;

    @Before
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
    }

}
