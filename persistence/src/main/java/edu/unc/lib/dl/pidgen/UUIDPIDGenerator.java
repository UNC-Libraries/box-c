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
package edu.unc.lib.dl.pidgen;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.TripleStoreQueryService;

/**
 * PIDGenerator that uses the Java UUID class to generate type 4 UUID-based PIDs.
 *
 * @see http://www.ietf.org/rfc/rfc4122.txt This identifier scheme is compatible with both the Fedora PID and a URN.
 *      PIDs are of the form "uuid:21a579fb-7bca-4025-88c0-345ee293725b". Note that datastream PIDs and datastream
 *      version PIDs will be uniquely persistent, but will not be compatible with URN.
 *
 * @author count0
 * 
 */
public class UUIDPIDGenerator implements PIDGenerator {
    private TripleStoreQueryService tripleStoreQueryService;
    private boolean verifyUnique = false;

    public boolean isVerifyUnique() {
        return verifyUnique;
    }

    public void init() {
        if (this.verifyUnique) {
            if (this.tripleStoreQueryService == null) {
                throw new Error("The UUIDPIDGenerator cannot verify unique PIDs without a tripleStoreQueryService.");
            }
        }
    }

    public void setVerifyUnique(boolean verifyUnique) {
        this.verifyUnique = verifyUnique;
    }

    public TripleStoreQueryService getTripleStoreQueryService() {
        return tripleStoreQueryService;
    }

    public void setTripleStoreQueryService(TripleStoreQueryService tripleStoreQueryService) {
        this.tripleStoreQueryService = tripleStoreQueryService;
    }

    public UUIDPIDGenerator() {
    }

    public PID getNextPID() {
        return getNextPIDs(1).get(0);
    }

    public List<PID> getNextPIDs(int howMany) {
        List<PID> result = new ArrayList<PID>();
        for (int i = 0; i < howMany; i++) {
            while (true) {
                UUID u = UUID.randomUUID();
                String s = String.format("uuid:%1$s", u);
                PID p = new PID(s);
                if (this.verifyUnique) {
                    if (this.tripleStoreQueryService.verify(p) != null) {
                        continue;
                    }
                }
                result.add(p);
                break;
            }
        }
        return result;
    }

}
