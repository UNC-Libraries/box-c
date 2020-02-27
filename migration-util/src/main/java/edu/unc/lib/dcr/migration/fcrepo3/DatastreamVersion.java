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
package edu.unc.lib.dcr.migration.fcrepo3;

import org.jdom2.Element;

/**
 * Version of a datastream from a fedora 3 object.
 *
 * @author bbpennel
 */
public class DatastreamVersion {

    private String md5;
    private String dsName;
    private String versionName;
    private String created;
    private String size;
    private String mimeType;
    private String altIds;
    private Element bodyEl;

    /**
     * Construct a datastream with the provided fields
     *
     * @param md5
     * @param dsName
     * @param versionName
     * @param created
     * @param size
     * @param mimeType
     */
    public DatastreamVersion(String md5, String dsName, String versionName, String created,
            String size, String mimeType, String altIds) {
        this.md5 = md5;
        this.dsName = dsName;
        this.versionName = versionName;
        this.created = created;
        this.size = size;
        this.mimeType = mimeType;
        this.altIds = altIds;
    }

    /**
     * @return the md5
     */
    public String getMd5() {
        return md5;
    }

    /**
     * @return the dsName
     */
    public String getDsName() {
        return dsName;
    }

    /**
     * @return the versionName
     */
    public String getVersionName() {
        return versionName;
    }

    /**
     * @return the created
     */
    public String getCreated() {
        return created;
    }

    /**
     * @return the size
     */
    public String getSize() {
        return size;
    }

    /**
     * @return the mimeType
     */
    public String getMimeType() {
        return mimeType;
    }

    public String getAltIds() {
        return altIds;
    }

    public void setAltIds(String altIds) {
        this.altIds = altIds;
    }

    /**
     * @return the bodyEl
     */
    public Element getBodyEl() {
        return bodyEl;
    }

    /**
     * @param bodyEl the bodyEl to set
     */
    public void setBodyEl(Element bodyEl) {
        this.bodyEl = bodyEl;
    }
}
