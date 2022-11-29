package edu.unc.lib.boxc.persist.api.sources;

import edu.unc.lib.boxc.persist.api.PackagingType;

/**
 * A single candidate for potential deposit from an ingest source.
 *
 * @author bbpennel
 *
 */
public class IngestSourceCandidate {
    private String sourceId;
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

    public void setPackagingTypeUri(String uri) {
        this.packagingType = PackagingType.getPackagingType(uri);
    }

    public String getPackagingTypeUri() {
        return packagingType.getUri();
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
