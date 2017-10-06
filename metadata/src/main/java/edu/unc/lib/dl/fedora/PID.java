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
package edu.unc.lib.dl.fedora;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Work with PIDS
 * @author bbpennel
 *
 */
public class PID implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 6597515249960543107L;
    protected static final String uriPrefix = "info:fedora/";
    protected String pid;

    public PID() {
    }

    /**
     * Construct an immutable digital object ID from a PID or a PID URI string.
     *
     * @param pid
     *                pid or pid URI string
     * @throws IllegalArgumentException
     */
    public PID(String pid) {
        if (pid == null) {
            throw new IllegalArgumentException("A non-null PID is required to create a Digital Object ID");
        } else if (pid.startsWith(uriPrefix)) {
            this.pid = pid.substring(uriPrefix.length());
        } else {
            this.pid = pid;
        }

    }

    @Override
        public boolean equals(Object obj) {
        if (obj instanceof PID) {
            PID input = (PID) obj;
            String pid = getPid();
            if (pid != null) {
            return pid.equals(input.getPid());
            }
        }
        return false;
    }

    @Deprecated
    public String getPid() {
        return pid;
    }

    public String getURI() {
    return uriPrefix + this.pid;
    }

    public String getPath() {
        return pid.replace(":", "/");
    }

    public String getUUID() throws UnsupportedOperationException {
        if (pid.startsWith("uuid:")) {
            return pid.substring(5);
        } else {
            throw new UnsupportedOperationException("PID is not a UUID PID");
        }
    }

    @Override
    public int hashCode() {
        return pid.hashCode();
    }

    @Override
    public String toString() {
        return this.pid;
    }

    public static List<PID> toPIDList(List<String> pidStrings) {
        List<PID> pids = new ArrayList<>(pidStrings.size());
        for (String pidString : pidStrings) {
            pids.add(new PID(pidString));
        }
        return pids;
    }

    /**
     * Get the unique identifier for this object.
     *
     * @return the unique identifier for this object
     */
    public String getId() {
        return pid;
    }

    /**
     * Get the object type path qualifier for this object.
     *
     * @return the object type path qualifier for this object.
     */
    public String getQualifier() {
        return pid;
    }

    /**
     * Get the qualified unique identifier for this object, containing the
     * formatted qualifier and id.
     *
     * @return the qualified id
     */
    public String getQualifiedId() {
        return pid;
    }

    /**
     * Get the component path, which is the portion of the repository path identifying
     * a specific component of the digital object
     *
     * @return the component path
     */
    public String getComponentPath() {
        return pid;
    }

    /**
     * Returns true if the provided pid is a component of the this pid
     *
     * @param pid
     * @return
     */
    public boolean containsComponent(PID pid) {
        return false;
    }

    /**
     * Returns the full repository uri for this object or component.
     *
     * @return
     */
    public URI getRepositoryUri() {
        return URI.create(this.getURI());
    }

    /**
     * Returns the repository URI for this pid as a string
     *
     * @return
     */
    public String getRepositoryPath() {
        return this.getURI();
    }
}

