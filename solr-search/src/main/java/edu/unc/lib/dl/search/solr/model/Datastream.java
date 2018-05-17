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
package edu.unc.lib.dl.search.solr.model;

import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.ContentModelHelper.DatastreamCategory;

/**
 *
 * @author bbpennel
 *
 */
public class Datastream {
    private String owner;
    private String name;
    private Long filesize;
    private String mimetype;
    private String filename;
    private String extension;
    private String checksum;
    private ContentModelHelper.Datastream datastreamClass;

    public Datastream(String owner, String name, Long filesize, String mimetype, String filename, String extension,
            String checksum) {
        this.owner = owner;
        this.name = name;
        this.filesize = filesize;
        this.mimetype = mimetype;
        this.filename = filename;
        this.extension = extension;
        this.checksum = checksum;
    }

    public Datastream(String datastream) {
        if (datastream == null) {
            throw new IllegalArgumentException("Datastream value must not be null");
        }

        String[] dsParts = datastream.split("\\|", -1);
        if (dsParts.length < 6) {
            throw new IllegalArgumentException("Invalid datastream string, requires 6 parameters, only included "
                    + dsParts.length);
        }

        this.name = dsParts[0];
        this.mimetype = dsParts[1];
        this.filename = dsParts[2];
        this.extension = dsParts[3];

        try {
            this.filesize = new Long(dsParts[4]);
        } catch (NumberFormatException e) {
            this.filesize = null;
        }
        this.checksum = dsParts[5];
        this.owner = dsParts[6];
    }

    @Override
    public String toString() {
        //DS name|mimetype|extension|filesize|checksum|owner
        StringBuilder sb = new StringBuilder();
        if (name != null) {
            sb.append(name);
        }
        sb.append('|');
        if (mimetype != null) {
            sb.append(mimetype);
        }
        sb.append('|');
        if (filename != null) {
            sb.append(filename);
        }
        sb.append('|');
        if (extension != null) {
            sb.append(extension);
        }
        sb.append('|');
        if (filesize != null) {
            sb.append(filesize);
        }
        sb.append('|');
        if (checksum != null) {
            sb.append(checksum);
        }
        sb.append('|');
        if (owner != null) {
            sb.append(owner);
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object object) {
        if (object == null) {
            return false;
        }
        if (object instanceof Datastream) {
            Datastream rightHand = (Datastream) object;
            // Equal if names match and either pids are null or both match
            return name.equals(rightHand.name)
                    && (rightHand.owner == null || owner == null || owner.equals(rightHand.owner));
        }
        if (object instanceof String) {
            String rightHandString = (String) object;
            if (rightHandString.equals(this.name)) {
                return true;
            }
            // Attempt comparison of string as a datastream
            try {
                Datastream rightHand = new Datastream(rightHandString);
                return this.equals(rightHand);
            } catch (IllegalArgumentException e) {
                // String wasn't a datastream, so not equal
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    public String getDatastreamIdentifier() {
        if (owner == null) {
            return name;
        }
        return owner + "/" + name;
    }

    public String getName() {
        return name;
    }

    public String getOwner() {
        return owner;
    }

    public Long getFilesize() {
        return filesize;
    }

    public String getMimetype() {
        return mimetype;
    }

    public String getExtension() {
        return extension;
    }

    public String getChecksum() {
        return checksum;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public void setMimetype(String mimetype) {
        this.mimetype = mimetype;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public void setFilesize(Long filesize) {
        this.filesize = filesize;
    }

    public DatastreamCategory getDatastreamCategory() {
        if (datastreamClass == null) {
            this.datastreamClass = ContentModelHelper.Datastream.getDatastream(this.name);
        }
        if (datastreamClass == null) {
            return null;
        }
        return datastreamClass.getCategory();
    }
}
