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

import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.ObjectTypeMismatchException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.PcdmModels;

/**
 * Represents a collection within the repository. This is a second-level container to which
 * folders and works can be added.
 *
 * @author harring
 *
 */
public class CollectionObject extends ContentContainerObject {

    protected CollectionObject(PID pid, RepositoryObjectDriver driver,
            RepositoryObjectFactory repoObjFactory) {
        super(pid, driver, repoObjFactory);
    }

    @Override
    public CollectionObject validateType() throws FedoraException {
        if (!isType(Cdr.Collection.getURI())) {
            throw new ObjectTypeMismatchException("Object " + pid + " is not a Collection.");
        }
        if (!isType(PcdmModels.Object.getURI())) {
            throw new ObjectTypeMismatchException("Object " + pid + " is not a PCDM Object.");
        }
        return this;
    }

    @Override
    public ContentContainerObject addMember(ContentObject member) throws ObjectTypeMismatchException {
        if (!(member instanceof FolderObject || member instanceof WorkObject)) {
            throw new ObjectTypeMismatchException("Cannot add object of type " + member.getClass().getName()
                    + " as a member of CollectionObject " + pid.getQualifiedId());
        }

        repoObjFactory.addMember(this, member);
        return this;
    }

}
