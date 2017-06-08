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
package edu.unc.lib.dl.admin.collect;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

/**
 * Stores the configuration information for a single deposit bin, from one or more directories
 *
 * @author bbpennel
 * @date Jun 13, 2014
 */
public class DepositBinConfiguration {

    private String name;
    private List<String> paths;
    private Pattern filePattern;
    private Long maxBytesPerFile;
    private String packageType;
    private String destination;
    private boolean publishObjects = true;

    private final ReentrantLock keyLock;

    public DepositBinConfiguration() {
        keyLock = new ReentrantLock();
    }

    /**
     *
     * @return title for this bin
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     *
     * @return file system paths where files for this bin are located. There must be one or more of these
     */
    public List<String> getPaths() {
        return paths;
    }

    public void setPaths(List<String> paths) {
        this.paths = paths;
    }

    /**
     *
     * @return regular expression which determines names of files allowed in this collection.
     * If not set, then file names are not restricted
     */
    public Pattern getFilePattern() {
        return filePattern;
    }

    /**
     *
     * @param filePattern
     * regular expression which determines names of files allowed in this collection
     */
    public void setFilePattern(String filePattern) {
        this.filePattern = Pattern.compile(filePattern);
    }

    /**
     *
     * @return the maximum size allowable for files from this bin, in bytes
     */
    public Long getMaxBytesPerFile() {
        return maxBytesPerFile;
    }

    public void setMaxBytesPerFile(Long maxSize) {
        this.maxBytesPerFile = maxSize;
    }

    /**
     *
     * @return the packaging type for batches from this bin
     */
    public String getPackageType() {
        return packageType;
    }

    public void setPackageType(String packageType) {
        this.packageType = packageType;
    }

    /**
     *
     * @return PID of the destination container where objects from this bin will be ingested to.
     *     In valid pid format, ie:  uuid:... or info:fedora/uuid:...
     */
    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    /**
     * True if there are any restrictions on the files to pull from this bin
     *
     * @return
     */
    public boolean hasFileFilters() {
        return maxBytesPerFile != null || filePattern != null;
    }

    /**
     * If true, then packages ingested through the collector should be marked as published immediately
     *
     * @return
     */
    public boolean isPublishObjects() {
        return publishObjects;
    }

    public void setPublishObjects(boolean publishObjects) {
        this.publishObjects = publishObjects;
    }

    public ReentrantLock getKeyLock() {
        return keyLock;
    }

}