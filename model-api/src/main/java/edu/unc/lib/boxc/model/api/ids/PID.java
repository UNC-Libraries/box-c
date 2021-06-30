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
package edu.unc.lib.boxc.model.api.ids;

import java.net.URI;

/**
 * A persistent identifier for a repository resource
 * @author bbpennel
 *
 */
public interface PID {
    /**
     * @return String representation of the PID
     */
    @Deprecated
    public String getPid();

    /**
     * @return URI representation of the persistent identifer
     */
    @Deprecated
    public String getURI();

    /**
     * @return
     */
    @Deprecated
    public String getPath();

    /**
     * @return The UUID portion of this PID
     */
    public String getUUID();

    /**
     * Get the unique identifier for this object.
     *
     * @return the unique identifier for this object
     */
    public String getId();

    /**
     * Get the object type path qualifier for this object.
     *
     * @return the object type path qualifier for this object.
     */
    public String getQualifier();

    /**
     * Get the qualified unique identifier for this object, containing the
     * formatted qualifier and id.
     *
     * @return the qualified id
     */
    public String getQualifiedId();

    /**
     * Get the component path, which is the portion of the repository path identifying
     * a specific component of the digital object
     *
     * @return the component path
     */
    public String getComponentPath();

    /**
     * Get the unique identifier for this object, including the component path if present
     *
     * @return
     */
    public String getComponentId();

    /**
     * Returns true if the provided pid is a component of the this pid
     *
     * @param pid
     * @return
     */
    public boolean containsComponent(PID pid);

    /**
     * Returns the full repository uri for this object or component.
     *
     * @return
     */
    public URI getRepositoryUri();

    /**
     * Returns the repository URI for this pid as a string
     *
     * @return
     */
    public String getRepositoryPath();
}

