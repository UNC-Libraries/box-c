/**
 * Copyright © 2008 The University of North Carolina at Chapel Hill (cdr@unc.edu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.util;

import edu.unc.lib.dl.fedora.PID;

/**
 * Deposit Constants
 * @author count0
 *
 */
public class DepositConstants {
    public static final String DESCRIPTION_DIR = "description";
    public static final String JENA_TDB_DIR = "jena-tdb-model";
    public static final String EVENTS_FILE = "events.xml";
    public static final String EVENTS_DIR = "events";
    public static final String DUBLINCORE_DIR = "dc";
    public static final String FOXML_DIR = "foxml";
    public static final String AIPS_DIR = "aips";
    public static final String DATA_DIR = "data";
    public static final String TECHMD_DIR = "techmd";
    public static final String RESUBMIT_DIR_PREFIX = "resubmit-";
    public static final String RESUBMIT_BACKUP_DIR = "resubmit-backup";
    public static final String FILE_LOCATOR_URI = "http://cdr.lib.unc.edu/schema/bag#locator";

    private DepositConstants() {

    }

    /**
     * Answers the object's PID for a given metadata file within a tag
     * directory.
     * 
     * @param path
     *            the file path, relative or absolute, e.g.
     *            description/65a631a3-8ec0-4233-a4d7-99c6190c875f.xml
     * @return the PID
     */
    public static PID getPIDForTagFile(String path) {
        String uuid = path.substring(path.lastIndexOf('/') + 1,
                path.lastIndexOf('.'));
        return new PID("uuid:" + uuid);
    }
}
