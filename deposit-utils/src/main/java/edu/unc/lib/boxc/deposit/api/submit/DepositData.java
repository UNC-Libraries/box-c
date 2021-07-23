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
package edu.unc.lib.boxc.deposit.api.submit;

import java.io.InputStream;
import java.net.URI;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.deposit.api.PackagingType;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.Priority;

/**
 * Data composing a deposit submission
 *
 * @author bbpennel
 *
 */
public class DepositData {

    private InputStream inputStream;
    private String filename;
    private String mimeType;
    private String slug;
    private String md5;
    private String accessionNumber;
    private String mediaId;
    private URI sourceUri;
    private Priority priority;
    private AgentPrincipals depositingAgent;
    private String depositorEmail;
    private PackagingType packagingType;
    private String depositMethod;
    private boolean staffOnly;
    private boolean overrideTimestamps;
    private boolean createParentFolder;

    public DepositData(InputStream inputStream, String filename, String mimeType, PackagingType packagingType,
            String depositMethod, AgentPrincipals depositingAgent) {
        this(mimeType, packagingType, depositMethod, depositingAgent);
        this.inputStream = inputStream;
        this.filename = filename;
    }

    public DepositData(URI sourceUri, String mimeType, PackagingType packagingType, String depositMethod,
            AgentPrincipals depositingAgent) {
        this(mimeType, packagingType, depositMethod, depositingAgent);
        this.sourceUri = sourceUri;
    }

    private DepositData(String mimeType, PackagingType packagingType, String depositMethod,
            AgentPrincipals depositingAgent) {
        this.mimeType = mimeType;
        this.depositingAgent = depositingAgent;
        this.packagingType = packagingType;
        this.depositMethod = depositMethod;
        priority = Priority.normal;
    }

    /**
     * @return the inputStream containing the deposit payload
     */
    public InputStream getInputStream() {
        return inputStream;
    }

    /**
     * @param inputStream the inputStream to set
     */
    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    /**
     * @return the filename
     */
    public String getFilename() {
        return filename;
    }

    /**
     * @param filename the filename to set
     */
    public void setFilename(String filename) {
        this.filename = filename;
    }

    /**
     * @return the mimeType
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * @param mimeType the mimeType to set
     */
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * @return the slug
     */
    public String getSlug() {
        return slug;
    }

    /**
     * @param slug the slug to set
     */
    public void setSlug(String slug) {
        this.slug = slug;
    }

    /**
     * @return the md5
     */
    public String getMd5() {
        return md5;
    }

    /**
     * @param md5 the md5 to set
     */
    public void setMd5(String md5) {
        this.md5 = md5;
    }

    /**
     * @return the sourceUri
     */
    public URI getSourceUri() {
        return sourceUri;
    }

    /**
     * @param sourceUri the sourceUri to set
     */
    public void setSourceUri(URI sourceUri) {
        this.sourceUri = sourceUri;
    }

    /**
     * @return the priority
     */
    public Priority getPriority() {
        return priority;
    }

    /**
     * @param priority the priority to set
     */
    public void setPriority(Priority priority) {
        this.priority = priority;
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
     * @return the depositMethod
     */
    public String getDepositMethod() {
        return depositMethod;
    }

    /**
     * @param depositMethod the depositMethod to set
     */
    public void setDepositMethod(String depositMethod) {
        this.depositMethod = depositMethod;
    }

    /**
     * @return the depositingAgent
     */
    public AgentPrincipals getDepositingAgent() {
        return depositingAgent;
    }

    /**
     * @param depositingAgent the depositingAgent to set
     */
    public void setDepositingAgent(AgentPrincipals depositingAgent) {
        this.depositingAgent = depositingAgent;
    }

    /**
     * @return the depositorEmail
     */
    public String getDepositorEmail() {
        return depositorEmail;
    }

    /**
     * @param depositorEmail the depositorEmail to set
     */
    public void setDepositorEmail(String depositorEmail) {
        this.depositorEmail = depositorEmail;
    }

    /**
     * @return the accessionNumber
     */
    public String getAccessionNumber() {
        return accessionNumber;
    }

    /**
     * @param accessionNumber the accessionNumber to set
     */
    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }

    /**
     * @return the mediaId
     */
    public String getMediaId() {
        return mediaId;
    }

    /**
     * @param mediaId the mediaId to set
     */
    public void setMediaId(String mediaId) {
        this.mediaId = mediaId;
    }

    /**
     * @return staffOnly status
     */
    public boolean getStaffOnly() {
        return staffOnly;
    }

    /**
     * Set staffOnly status
     * @param staffOnly staffOnly permission true/false
     */
    public void setStaffOnly(boolean staffOnly) {
        this.staffOnly = staffOnly;
    }

    /**
     * @return true if the deposit should attempt to override timestamps when specified
     */
    public boolean getOverrideTimestamps() {
        return overrideTimestamps;
    }

    public void setOverrideTimestamps(boolean overrideTimestamps) {
        this.overrideTimestamps = overrideTimestamps;
    }

    /**
     * @return if true, then this deposit will create a parent container to wrap the contents of
     *      the deposit if appropriate for the deposit type.
     */
    public boolean getCreateParentFolder() {
        return createParentFolder;
    }

    public void setCreateParentFolder(boolean createParentFolder) {
        this.createParentFolder = createParentFolder;
    }
}
