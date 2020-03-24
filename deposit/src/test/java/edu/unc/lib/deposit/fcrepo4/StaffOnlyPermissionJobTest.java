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
package edu.unc.lib.deposit.fcrepo4;

import static edu.unc.lib.dl.acl.util.AccessPrincipalConstants.AUTHENTICATED_PRINC;
import static edu.unc.lib.dl.acl.util.AccessPrincipalConstants.PUBLIC_PRINC;
import static edu.unc.lib.dl.acl.util.UserRole.none;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryPathConstants;
import org.apache.jena.rdf.model.Model;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.unc.lib.dl.acl.fcrepo4.AclFactory;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;
import edu.unc.lib.dl.acl.util.UserRole;
import edu.unc.lib.dl.fcrepo4.DepositRecord;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectFactory;
import edu.unc.lib.dl.fedora.PID;

/**
 * @author lfarrell
 */
public class StaffOnlyPermissionJobTest extends AbstractDepositJobTest {
    @Mock
    private DepositRecord depositRecord;
    @Mock
    private RepositoryObjectFactory repoObjFactory;
    @Mock
    private AclFactory aclFactory;

    private StaffOnlyPermissionJob job;

    @Before
    public void setup() throws Exception {
        when(repoObjFactory.createDepositRecord(any(PID.class), any(Model.class)))
                .thenReturn(depositRecord);
        PID eventPid = makePid("content");
        when(pidMinter.mintPremisEventPid(any(PID.class))).thenReturn(eventPid);

        depositPid = PIDs.get(RepositoryPathConstants.DEPOSIT_RECORD_BASE + "/" + depositUUID);
        when(depositRecord.getPid()).thenReturn(depositPid);
    }

    @Test
    public void testStaffOnly() throws Exception {
        Map<String, String> depositStatus = new HashMap<>();
        depositStatus.put(DepositField.staffOnly.name(), "true");
        when(depositStatusFactory.get(eq(depositUUID))).thenReturn(depositStatus);

        job = new StaffOnlyPermissionJob();
        job.setDepositUUID(depositUUID);

        Map<String, Set<String>> princRoles = aclFactory.getPrincipalRoles(depositPid);

        assertPrincipalHasRoles("Role not found for public user",
                princRoles, PUBLIC_PRINC, none);
        assertPrincipalHasRoles("Role not found for authenticated user",
                princRoles, AUTHENTICATED_PRINC, none);
    }

    @Test
    public void testNoStaffOnly() throws Exception {
        Map<String, String> depositStatus = new HashMap<>();
        depositStatus.put(DepositField.staffOnly.name(), "false");
        when(depositStatusFactory.get(eq(depositUUID))).thenReturn(depositStatus);

        job = new StaffOnlyPermissionJob();
        job.setDepositUUID(depositUUID);

        Map<String, Set<String>> princRoles = aclFactory.getPrincipalRoles(depositPid);
        assertPrincipalDoesNotHasRoles("Role found for public user",
                princRoles, PUBLIC_PRINC, none);
        assertPrincipalDoesNotHasRoles("Role found for authenticated user",
                princRoles, AUTHENTICATED_PRINC, none);
    }

    private static void assertPrincipalHasRoles(String message, Map<String, Set<String>> princRoles,
                                                String principal, UserRole... expectedRoles) {
        try {
            Set<String> roles = princRoles.get(principal);
            assertNotNull(roles);
            assertEquals(expectedRoles.length, roles.size());
            for (UserRole expectedRole : expectedRoles) {
                assertTrue(roles.contains(expectedRole.getPropertyString()));
            }
        } catch (Error e) {
            throw new AssertionError(message, e);

        }
    }

    private static void assertPrincipalDoesNotHasRoles(String message, Map<String, Set<String>> princRoles,
                                                String principal, UserRole... expectedRoles) {
        try {
            Set<String> roles = princRoles.get(principal);
            assertNotNull(roles);
            assertEquals(expectedRoles.length, roles.size());
            for (UserRole expectedRole : expectedRoles) {
                assertFalse(roles.contains(expectedRole.getPropertyString()));
            }
        } catch (Error e) {
            throw new AssertionError(message, e);

        }
    }
}
