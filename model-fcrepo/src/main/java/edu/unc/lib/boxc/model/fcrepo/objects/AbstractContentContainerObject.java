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
package edu.unc.lib.boxc.model.fcrepo.objects;

import java.util.List;
import java.util.stream.Collectors;

import edu.unc.lib.boxc.model.api.exceptions.ObjectTypeMismatchException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.ContentContainerObject;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.services.RepositoryObjectDriver;

/**
 * Represents a generic repository object within the main content tree which can
 * contain other ContentObjects.
 *
 * @author bbpennel
 *
 */
public abstract class AbstractContentContainerObject extends AbstractContentObject implements ContentContainerObject {

    protected AbstractContentContainerObject(PID pid, RepositoryObjectDriver driver,
            RepositoryObjectFactory repoObjFactory) {
        super(pid, driver, repoObjFactory);
    }

    /**
     * Add a ContentObject as a member to this container
     *
     * @param member
     * @return this ContentContainerObject
     * @throws ObjectTypeMismatchException
     *             Thrown if the new member is not of a type supported by this
     *             container
     */
    @Override
    public abstract ContentContainerObject addMember(ContentObject member) throws ObjectTypeMismatchException;

    /**
     * Retrieve a list of member content objects for this object.
     *
     * @return
     */
    @Override
    public List<ContentObject> getMembers() {
        return driver.listMembers(this).stream()
                .map(m -> (ContentObject) driver.getRepositoryObject(m))
                .collect(Collectors.toList());
    }
}
