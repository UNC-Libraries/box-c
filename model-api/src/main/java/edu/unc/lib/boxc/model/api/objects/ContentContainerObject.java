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
package edu.unc.lib.boxc.model.api.objects;

import java.util.List;

import edu.unc.lib.boxc.model.api.exceptions.ObjectTypeMismatchException;


/**
 * Represents a repository object within the main content tree which can
 * contain other ContentObjects.
 * @author bbpennel
 */
public interface ContentContainerObject extends ContentObject {

    /**
     * Add a ContentObject as a member to this container
     *
     * @param member
     * @return this ContentContainerObject
     * @throws ObjectTypeMismatchException
     *             Thrown if the new member is not of a type supported by this
     *             container
     */
    ContentContainerObject addMember(ContentObject member) throws ObjectTypeMismatchException;

    /**
     * Retrieve a list of member content objects for this object.
     *
     * @return
     */
    List<ContentObject> getMembers();

}