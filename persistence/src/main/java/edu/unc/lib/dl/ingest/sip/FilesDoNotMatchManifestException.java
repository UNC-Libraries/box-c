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
/**
 *
 */
package edu.unc.lib.dl.ingest.sip;

import java.util.ArrayList;
import java.util.List;

import edu.unc.lib.dl.ingest.IngestException;

/**
 * @author Gregory Jansen
 *
 */
public class FilesDoNotMatchManifestException extends IngestException {

    /**
     *
     */
    private static final long serialVersionUID = -6115709006722472202L;

    /**
     * @param msg
     * @param e
     */
    public FilesDoNotMatchManifestException(String msg, Throwable e) {
	super(msg, e);
    }

    /**
     * @param msg
     */
    public FilesDoNotMatchManifestException(String msg) {
	super(msg);
    }

    private List<String> missingFiles = new ArrayList<String>();
    public List<String> getMissingFiles() {
        return missingFiles;
    }

    public void setMissingFiles(List<String> missingFiles) {
        this.missingFiles = missingFiles;
    }

    public List<String> getExtraFiles() {
        return extraFiles;
    }

    public void setExtraFiles(List<String> extraFiles) {
        this.extraFiles = extraFiles;
    }

    public List<String> getBadChecksumFiles() {
        return badChecksumFiles;
    }

    public void setBadChecksumFiles(List<String> badChecksumFiles) {
        this.badChecksumFiles = badChecksumFiles;
    }

    private List<String> extraFiles = new ArrayList<String>();
    private List<String> badChecksumFiles = new ArrayList<String>();
}
