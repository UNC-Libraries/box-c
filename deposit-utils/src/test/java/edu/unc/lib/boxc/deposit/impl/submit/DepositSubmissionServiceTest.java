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
package edu.unc.lib.boxc.deposit.impl.submit;

import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.deposit.api.submit.DepositData;
import edu.unc.lib.boxc.deposit.api.submit.DepositHandler;
import edu.unc.lib.boxc.deposit.impl.model.DepositStatusFactory;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.persist.api.PackagingType;
import edu.unc.lib.boxc.persist.api.exceptions.UnsupportedPackagingTypeException;

/**
 *
 * @author bbpennel
 *
 */
public class DepositSubmissionServiceTest {
    private static final String FILENAME = "test.txt";
    private static final String MIMETYPE = "text/plain";
    private static final String DEPOSIT_METHOD = "unitTest";
    private static final Path FILEPATH = Paths.get(FILENAME);

    @Mock
    private AccessControlService aclService;

    @Mock
    private DepositHandler depositHandler1;
    @Mock
    private DepositHandler depositHandler2;
    @Mock
    private AgentPrincipals depositingAgent;
    @Mock
    private PID depositPid;
    @Mock
    private DepositStatusFactory depositStatusFactory;

    private Map<PackagingType, DepositHandler> packageHandlers;

    private DepositSubmissionService depositService;

    @Before
    public void init() throws Exception {
        initMocks(this);

        packageHandlers = new HashMap<>();
        packageHandlers.put(PackagingType.SIMPLE_OBJECT, depositHandler1);
        packageHandlers.put(PackagingType.BAGIT, depositHandler2);

        depositService = new DepositSubmissionService();
        depositService.setPackageHandlers(packageHandlers);
        depositService.setAclService(aclService);
    }

    @Test
    public void testSubmitDepositSimplePackage() throws Exception {
        DepositData deposit = new DepositData(FILEPATH.toUri(),
                MIMETYPE,
                PackagingType.SIMPLE_OBJECT,
                DEPOSIT_METHOD,
                depositingAgent);

        depositService.submitDeposit(depositPid, deposit);

        verify(depositHandler1).doDeposit(depositPid, deposit);
    }

    @Test
    public void testSubmitDepositBagitPackage() throws Exception {
        DepositData deposit = new DepositData(FILEPATH.toUri(),
                MIMETYPE,
                PackagingType.BAGIT,
                DEPOSIT_METHOD,
                depositingAgent);

        depositService.submitDeposit(depositPid, deposit);

        verify(depositHandler2).doDeposit(depositPid, deposit);
    }

    @Test(expected = UnsupportedPackagingTypeException.class)
    public void testSubmitDepositUnregisteredPackage() throws Exception {
        DepositData deposit = new DepositData(FILEPATH.toUri(),
                MIMETYPE,
                PackagingType.ATOM,
                DEPOSIT_METHOD,
                depositingAgent);

        depositService.submitDeposit(depositPid, deposit);
    }

    @Test(expected = UnsupportedPackagingTypeException.class)
    public void testSubmitDepositNullPackage() throws Exception {
        DepositData deposit = new DepositData(FILEPATH.toUri(),
                MIMETYPE,
                null,
                DEPOSIT_METHOD,
                depositingAgent);

        depositService.submitDeposit(depositPid, deposit);
    }
}
