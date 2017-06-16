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

import edu.unc.lib.dl.fedora.PID;

/**
 * Simple, local implementation of PIDGenerator for testing purposes.
 *
 * The first PID will be test:1, the next will be test:2, and so on.
 * 
 * @author count0
 * 
 */
public class TestPIDGenerator implements PIDGenerator {

    private int m_number;

    public TestPIDGenerator() {
        m_number = 0;
    }

    public PID getNextPID() {
        m_number++;
        return new PID("test:" + m_number);
    }

    public List<PID> getNextPIDs(int howMany) {
        List<PID> pids = new ArrayList<PID>();
        for (int i = 0; i < howMany; i++) {
            pids.add(getNextPID());
        }
        return pids;
    }

}