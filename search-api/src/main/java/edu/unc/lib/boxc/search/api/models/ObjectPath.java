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
package edu.unc.lib.boxc.search.api.models;

import java.util.List;

/**
 * A hierarchy path for a content object
 * @author bbpennel
 */
public interface ObjectPath {

    /**
     * @return All entries in this object path
     */
    List<ObjectPathEntry> getEntries();

    /**
     * @param pid
     * @return The title/name of the object in this path with the provided pid
     */
    String getName(String pid);

    /**
     * Return a string representation of the path constructed from the names of
     * entries in this path
     *
     * @return name path
     */
    String toNamePath();

    /**
     * Return a string representation of the path constructed from the ids of
     * entries in this path.
     *
     * @return id path
     */
    String toIdPath();

}