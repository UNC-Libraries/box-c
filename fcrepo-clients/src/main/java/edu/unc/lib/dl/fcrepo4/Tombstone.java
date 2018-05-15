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

import java.util.HashMap;
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

    private Map<String,String> record = new HashMap<>();
    private RepositoryObjectLoader repoObjLoader;

    public Tombstone(PID pid, RepositoryObjectDriver driver, RepositoryObjectFactory repoObjFactory) {
        super(pid, driver, repoObjFactory);
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

    private void populateRecord() {
        record.put("PREMIS log", this.getPremisLog().getEvents().toString());
        //TODO: link to deposit record
        //TODO: display name of obj
        RepositoryObject obj = repoObjLoader.getRepositoryObject(pid);
        String objectType = obj.getContentObjectType();
        record.put("object type", objectType);
        if (objectType.equals("file")) {
            // get binobj(s) of this file obj and call insertBinaryDetails...
        }
    }

    private void insertBinaryDetailsIntoRecord(BinaryObject binObj) {
        record.put("filename", binObj.getFilename());

        String sha1Checksum = binObj.getSha1Checksum();
        String md5Checksum = binObj.getMd5Checksum();
        if (sha1Checksum != null) {
            record.put("sha1 checksum", sha1Checksum);
        }
        if ( md5Checksum  != null) {
            record.put("md5 checksum", md5Checksum);
        }

        record.put("filesize", binObj.getFilesize().toString());

        record.put("mimetype", binObj.getMimetype());
    }

}
