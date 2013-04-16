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

public class PID implements Serializable {
    /**
	 *
	 */
	private static final long serialVersionUID = 6597515249960543107L;
	protected static final String uriPrefix = "info:fedora/";
    protected String pid;

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
	    if (pid != null) {
		return pid.equals(input.getPid());
	    }
	}
	return false;
    }

    public String getPid() {
	return pid;
    }

    public String getURI() {
	return uriPrefix + this.pid;
    }
    
    public String getPath() {
   	 return pid.replace(":", "/");
    }

    @Override
    public int hashCode() {
	return pid.hashCode();
    }

    @Override
    public String toString() {
	return this.pid;
    }

}
