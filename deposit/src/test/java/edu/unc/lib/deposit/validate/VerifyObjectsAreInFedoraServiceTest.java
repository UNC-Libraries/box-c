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
package edu.unc.lib.deposit.validate;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;

/**
 *
 * @author harring
 *
 */
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
