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
package edu.unc.lib.dl.cdr.sword.server;

import org.swordapp.server.SwordConfiguration;

/**
 *
 * @author bbpennel
 *
 */
public class SwordConfigurationImpl implements SwordConfiguration {
    public static final String COLLECTION_PATH = "/collection";
    public static final String SERVICE_DOCUMENT_PATH = "/serviceDocument";
    public static final String EDIT_MEDIA_PATH = "/em";
    public static final String EDIT_PATH = "/object";
    public static final String STATE_PATH = "/state";

    private String authType = null;
    private int maxUploadSize = -1;
    private String tempDirectory = null;
    private String basePath;
    private String swordPath;
    private String swordVersion = null;
    private String generator = null;
    private String generatorVersion = null;
    private String depositorNamespace = null;
    private String adminDepositor = null;

    public SwordConfigurationImpl() {
    }

    @Override
    public boolean returnDepositReceipt() {
        return true;
    }

    @Override
    public boolean returnStackTraceInError() {
        return true;
    }

    @Override
    public boolean returnErrorBody() {
        return true;
    }

    @Override
    public String generator() {
        return this.generator;
    }

    @Override
    public String generatorVersion() {
        return this.generatorVersion;
    }

    public void setGenerator(String generator) {
        this.generator = generator;
    }

    public void setGeneratorVersion(String generatorVersion) {
        this.generatorVersion = generatorVersion;
    }

    @Override
    public String administratorEmail() {
        return null;
    }

    @Override
    public String getAuthType() {
        return this.authType;
    }

    public void setAuthType(String authType) {
        this.authType = authType;
    }

    @Override
    public boolean storeAndCheckBinary() {
        return true;
    }

    @Override
    public String getTempDirectory() {
        return this.tempDirectory;
    }

    public void setTempDirectory(String tempDirectory) {
        this.tempDirectory = tempDirectory;
    }

    @Override
    public int getMaxUploadSize() {
        return this.maxUploadSize;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public String getSwordPath() {
        return swordPath;
    }

    public void setSwordPath(String swordPath) {
        this.swordPath = swordPath;
    }

    public String getSwordVersion() {
        return swordVersion;
    }

    public void setSwordVersion(String swordVersion) {
        this.swordVersion = swordVersion;
    }

    public String getDepositorNamespace() {
        return depositorNamespace;
    }

    public void setDepositorNamespace(String depositorNamespace) {
        this.depositorNamespace = depositorNamespace;
    }

    @Override
    public String getAlternateUrl() {
        return null;
    }

    @Override
    public String getAlternateUrlContentType() {
        return null;
    }

    public String getAdminDepositor() {
        return adminDepositor;
    }

    public void setAdminDepositor(String adminDepositor) {
        this.adminDepositor = adminDepositor;
    }

    @Override
    public boolean allowUnauthenticatedMediaAccess() {
        return false;
    }
}
