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
 * Represents the highest-level container in the repository.
 *
 * @author harring
 *
 */
public class AdminUnit extends ContentContainerObject {

    protected AdminUnit(PID pid, RepositoryObjectDriver driver,
            RepositoryObjectFactory repoObjFactory) {
        super(pid, driver, repoObjFactory);
    }

    @Override
    public ContentContainerObject addMember(ContentObject member) throws ObjectTypeMismatchException {
        if (!(member instanceof CollectionObject)) {
            throw new ObjectTypeMismatchException("Cannot add object of type " + member.getClass().getName()
                    + " as a member of AdminUnit " + pid.getQualifiedId());
        }
        repoObjFactory.addMember(this, member);
        return this;
    }

    @Override
    public AdminUnit validateType() throws FedoraException {
        if (!isType(Cdr.AdminUnit.getURI())) {
            throw new ObjectTypeMismatchException("Object " + pid + " is not an AdminUnit.");
        }
        if (!isType(PcdmModels.Collection.getURI())) {
            throw new ObjectTypeMismatchException("Object " + pid + " is not a PCDM Collection.");
        }
        return this;
    }

}
