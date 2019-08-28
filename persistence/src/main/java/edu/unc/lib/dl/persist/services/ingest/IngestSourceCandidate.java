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
package edu.unc.lib.dl.persist.services.ingest;

import edu.unc.lib.dl.util.PackagingType;

/**
 * A single candidate for potential deposit from an ingest source.
 *
 * @author bbpennel
 *
 */
public class IngestSourceCandidate {
    private String sourceId;
    private String base;
    private String patternMatched;
    private String version;
    private PackagingType packagingType;
    private Long fileSize;
    private Integer fileCount;

    /**
     * @return the sourceId
     */
    public String getSourceId() {
        return sourceId;
    }

    /**
     * @param sourceId the sourceId to set
     */
    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    /**
     * @return the base
     */
    public String getBase() {
        return base;
    }

    /**
     * @param base the base to set
     */
    public void setBase(String base) {
        this.base = base;
    }

    /**
     * @return the patternMatched
     */
    public String getPatternMatched() {
        return patternMatched;
    }

    /**
     * @param patternMatched the patternMatched to set
     */
    public void setPatternMatched(String patternMatched) {
        this.patternMatched = patternMatched;
    }

    /**
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * @param version the version to set
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * @return the packagingType
     */
    public PackagingType getPackagingType() {
        return packagingType;
    }

    /**
     * @param packagingType the packagingType to set
     */
    public void setPackagingType(PackagingType packagingType) {
        this.packagingType = packagingType;
    }

    /**
     * @return the fileCount
     */
    public Integer getFileCount() {
        return fileCount;
    }

    /**
     * @param fileCount the fileCount to set
     */
    public void setFileCount(Integer fileCount) {
        this.fileCount = fileCount;
    }

    /**
     * @return the fileSize
     */
    public Long getFileSize() {
        return fileSize;
    }

    /**
     * @param fileSize the fileSize to set
     */
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }
}
