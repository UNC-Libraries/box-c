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
package edu.unc.lib.dl.util;

import edu.unc.lib.dl.fedora.PID;

/**
 * ParentBond
 * @author count0
 *
 */
public class ParentBond {
    /**
     * The PID of the subject's parent
     */
    public PID parentPid = null;
    /**
     * Whether or not the subject inherits roles from this parent.
     */
    public boolean inheritsRoles = true;
}