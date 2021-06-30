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

import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.api.exceptions.FedoraException;
import edu.unc.lib.boxc.model.api.exceptions.ObjectTypeMismatchException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.CollectionObject;
import edu.unc.lib.boxc.model.api.objects.ContentContainerObject;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.PcdmModels;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.services.RepositoryObjectDriver;

/**
 * Represents a collection within the repository. This is a second-level container to which
 * folders and works can be added.
 *
 * @author harring
 *
 */
public class CollectionObjectImpl extends AbstractContentContainerObject implements CollectionObject {

    protected CollectionObjectImpl(PID pid, RepositoryObjectDriver driver,
            RepositoryObjectFactory repoObjFactory) {
        super(pid, driver, repoObjFactory);
    }

    @Override
    public CollectionObjectImpl validateType() throws FedoraException {
        if (!isType(Cdr.Collection.getURI())) {
            throw new ObjectTypeMismatchException("Object " + pid + " is not a Collection.");
        }
        if (!isType(PcdmModels.Object.getURI())) {
            throw new ObjectTypeMismatchException("Object " + pid + " is not a PCDM Object.");
        }
        return this;
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.Collection;
    }

    @Override
    public ContentContainerObject addMember(ContentObject member) throws ObjectTypeMismatchException {
        if (!(member instanceof FolderObjectImpl || member instanceof WorkObjectImpl)) {
            throw new ObjectTypeMismatchException("Cannot add object of type " + member.getClass().getName()
                    + " as a member of CollectionObject " + pid.getQualifiedId());
        }

        repoObjFactory.addMember(this, member);
        member.shouldRefresh();
        return this;
    }

}
