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

import java.io.Serializable;

import edu.unc.lib.dl.fedora.PID;

/**
 * @author Gregory Jansen
 *
 */
public class ContainerPlacement implements Serializable {
    private static final long serialVersionUID = -6481661187173513169L;
    public PID parentPID;
    public PID pid;
    public String label;

    /**
     * An explicit position number for this object within the parent's other
     * children, designated by the submitter. May be NULL.
     */
    public Integer designatedOrder = null;

    /**
     * The position of this object within its other incoming siblings.
     */
    public Integer sipOrder = null;
}
