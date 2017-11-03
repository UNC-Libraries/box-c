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
package edu.unc.lib.dl.fcrepo4;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;

import edu.unc.lib.dl.fedora.ObjectTypeMismatchException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.PcdmModels;

/**
 * Represents a generic repository object within the main content tree which can
 * contain other ContentObjects.
 *
 * @author bbpennel
 *
 */
public abstract class ContentContainerObject extends ContentObject {

    protected ContentContainerObject(PID pid, RepositoryObjectDriver driver,
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
    public abstract ContentContainerObject addMember(ContentObject member) throws ObjectTypeMismatchException;

    /**
     * Retrieve a list of member content objects for this object.
     *
     * @return
     */
    public List<ContentObject> getMembers() {
        List<ContentObject> members = new ArrayList<>();
        Resource resc = getResource();

        for (StmtIterator it = resc.listProperties(PcdmModels.hasMember); it.hasNext(); ) {
            String memberUri = it.nextStatement().getResource().toString();

            members.add(driver.getRepositoryObject(PIDs.get(memberUri), ContentObject.class));
        }

        return members;
    }
}
