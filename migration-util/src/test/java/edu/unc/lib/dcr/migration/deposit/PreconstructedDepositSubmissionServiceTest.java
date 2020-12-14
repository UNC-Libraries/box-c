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
package edu.unc.lib.dcr.migration.deposit;

import static edu.unc.lib.dcr.migration.deposit.PreconstructedDepositSubmissionService.EMAIL_SUFFIX;
import static edu.unc.lib.dl.acl.util.AccessPrincipalConstants.USER_NAMESPACE;
import static edu.unc.lib.dl.test.TestHelpers.setField;
import static edu.unc.lib.dl.util.DepositMethod.BXC3_TO_5_MIGRATION_UTIL;
import static edu.unc.lib.dl.util.PackagingType.BAG_WITH_N3;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.IOException;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.fcrepo4.RepositoryPIDMinter;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.DepositStatusFactory;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositAction;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositState;
import edu.unc.lib.dl.util.RedisWorkerConstants.Priority;

/**
 * @author bbpennel
 */
public class PreconstructedDepositSubmissionServiceTest {

    private static final String DEPOSITOR = "testUser";
    private static final String DEPOSITOR_GROUPS = "somegroup";
    private static final String DISPLAY_LABEL = "My deposit";

    @Captor
    private ArgumentCaptor<Map<String, String>> statusCaptor;

    @Mock
    private DepositStatusFactory statusFactory;

    private RepositoryPIDMinter pidMinter;

    private PreconstructedDepositSubmissionService service;

    @Before
    public void setup() throws Exception {
        initMocks(this);

        service = new PreconstructedDepositSubmissionService("localhost", 1);
        setField(service, "depositStatusFactory", statusFactory);

        pidMinter = new RepositoryPIDMinter();
    }

    @After
    public void tearDown() throws IOException {
        service.close();
    }

    @Test
    public void submitDeposit() {
        PID depositPid = pidMinter.mintDepositRecordPid();
        PID destinationPid = pidMinter.mintContentPid();

        int result = service.submitDeposit(DEPOSITOR, DEPOSITOR_GROUPS, depositPid, destinationPid, DISPLAY_LABEL);

        assertEquals(0, result);

        verify(statusFactory).save(eq(depositPid.getId()), statusCaptor.capture());

        Map<String, String> status = statusCaptor.getValue();
        verifyDepositFields(depositPid, destinationPid, status);
    }

    private void verifyDepositFields(PID depositPid, PID destinationPid,
            Map<String, String> status) {
        assertEquals(depositPid.getId(), status.get(DepositField.uuid.name()));
        assertNotNull("Deposit submission time must be set", status.get(DepositField.submitTime.name()));
        assertEquals(BAG_WITH_N3.getUri(), status.get(DepositField.packagingType.name()));
        assertEquals(BXC3_TO_5_MIGRATION_UTIL.getLabel(), status.get(DepositField.depositMethod.name()));
        assertEquals(DEPOSITOR, status.get(DepositField.depositorName.name()));
        assertEquals(DEPOSITOR + EMAIL_SUFFIX, status.get(DepositField.depositorEmail.name()));
        assertEquals(destinationPid.getId(), status.get(DepositField.containerId.name()));
        assertEquals(Priority.low.name(), status.get(DepositField.priority.name()));
        assertEquals(true, Boolean.parseBoolean(status.get(DepositField.overrideTimestamps.name())));
        assertEquals(DISPLAY_LABEL, status.get(DepositField.depositSlug.name()));

        assertEquals(DepositState.unregistered.name(), status.get(DepositField.state.name()));
        assertEquals(DepositAction.register.name(), status.get(DepositField.actionRequest.name()));
        AccessGroupSet depositPrincipals = new AccessGroupSet(status.get(DepositField.permissionGroups.name()));
        assertTrue("user principal must be set in deposit", depositPrincipals.contains(USER_NAMESPACE + DEPOSITOR));
        assertTrue("group principal must be set in deposit", depositPrincipals.contains(DEPOSITOR_GROUPS));
    }
}
