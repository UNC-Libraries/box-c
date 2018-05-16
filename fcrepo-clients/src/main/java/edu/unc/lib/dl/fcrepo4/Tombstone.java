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

import java.util.Map;

import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.ObjectTypeMismatchException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Fcrepo4Repository;

/**
 * Represents a tombstone object within the repository
 *
 * @author harring
 *
 */
public class Tombstone extends RepositoryObject {

    private Map<String,String> record;

    public Tombstone(PID pid, RepositoryObjectDriver driver, RepositoryObjectFactory repoObjFactory,
            Map<String, String> record) {
        super(pid, driver, repoObjFactory);
        this.record = record;
    }

    @Override
    public RepositoryObject validateType() throws FedoraException {
        if (!isType(Fcrepo4Repository.Tombstone.toString())) {
            throw new ObjectTypeMismatchException("Object " + pid + " is not a tombstone");
        }
        return this;
    }

    @Override
    public RepositoryObject getParent() {
        return driver.getParentObject(this);
    }

    public Map<String,String> getRecord() {
        return record;
    }
}
