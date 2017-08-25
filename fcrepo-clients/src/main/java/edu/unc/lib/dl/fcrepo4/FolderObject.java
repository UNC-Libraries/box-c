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

import org.apache.jena.rdf.model.Model;

import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.ObjectTypeMismatchException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;

/**
 * A repository object which represents a Folder. Folders are containers which
 * may hold work objects or folder objects directly inside of them.
 *
 * @author bbpennel, harring
 *
 */
public class FolderObject extends ContentContainerObject {

    private RepositoryPIDMinter pidMinter;

    protected FolderObject(PID pid, RepositoryObjectLoader repoObjLoader, RepositoryObjectDataLoader dataLoader,
            RepositoryObjectFactory repoObjFactory) {
        super(pid, repoObjLoader, dataLoader, repoObjFactory);
    }

    @Override
    public FolderObject validateType() throws FedoraException {
        if (!isType(Cdr.Folder.toString())) {
            throw new ObjectTypeMismatchException("Object " + pid + " is not a Folder object.");
        }
        return this;
    }

    @Override
    public ContentContainerObject addMember(ContentObject member) throws ObjectTypeMismatchException {
        if (!(member instanceof FolderObject || member instanceof WorkObject)) {
            throw new ObjectTypeMismatchException("Cannot add object of type " + member.getClass().getName()
                    + " as a member of WorkObject " + pid.getQualifiedId());
        }

        repoObjFactory.addMember(this, member);
        return this;
    }

    /**
     * Creates and adds a new folder to this folder.
     *
     * @return the newly created folder object
     */
    public FolderObject addFolder() {
        return addFolder(pidMinter.mintContentPid(), null);
    }

    /**
     * Creates and adds a new folder with the provided pid and properties to this
     * folder.
     *
     * @param pid
     *            pid for the new folder
     * @param model
     *            properties for the new folder
     * @return the newly created folder object
     */
    public FolderObject addFolder(PID childPid, Model model) {
        FolderObject work = repoObjFactory.createFolderObject(childPid, model);
        repoObjFactory.addMember(this, work);

        return work;
    }

    /**
     * Creates and adds a new work to this folder.
     *
     * @return the newly created work object
     */
    public WorkObject addWork() {
        return addWork(pidMinter.mintContentPid(), null);
    }

    /**
     * Creates and adds a new work with the provided pid and properties to this
     * folder.
     *
     * @param pid
     *            pid for the new work
     * @param model
     *            optional additional properties for the work
     * @return the newly created work object
     */
    public WorkObject addWork(PID childPid, Model model) {
        WorkObject work = repoObjFactory.createWorkObject(childPid, model);
        repoObjFactory.addMember(this, work);

        return work;
    }
}
