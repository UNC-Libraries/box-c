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
package edu.unc.lib.dl.update;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.PremisEventLogger;

/**
 * 
 * @author bbpennel
 *
 */
public class UIPImpl implements UpdateInformationPackage {

    protected PID pid;
    protected String user;
    protected UpdateOperation operation;

    protected HashMap<String, ?> incomingData;
    protected HashMap<String, ?> originalData;
    protected HashMap<String, ?> modifiedData;

    protected String message;

    protected PremisEventLogger eventLogger = new PremisEventLogger(ContentModelHelper.Administrative_PID
            .ADMINISTRATOR_GROUP.getPID().getURI());

    public UIPImpl(PID pid, String user, UpdateOperation operation) {
        this.pid = pid;
        this.user = user;
        this.operation = operation;
        message = null;
    }

    @Override
    public PID getPID() {
        return pid;
    }

    @Override
    public String getUser() {
        return user;
    }

    @Override
    public UpdateOperation getOperation() {
        return operation;
    }

    @Override
    public Map<String, ?> getIncomingData() {
        return incomingData;
    }

    @Override
    public Map<String, ?> getOriginalData() {
        return originalData;
    }

    @Override
    public Map<String, ?> getModifiedData() {
        return modifiedData;
    }

    @Override
    public Map<String, File> getModifiedFiles() {
        return null;
    }

    @Override
    public String getMimetype(String key) {
        return null;
    }

    @Override
    public PremisEventLogger getEventLogger() {
        return this.eventLogger;
    }

    public void setIncomingData(HashMap<String, ?> incomingData) {
        this.incomingData = incomingData;
    }

    public void setOriginalData(HashMap<String, ?> originalData) {
        this.originalData = originalData;
    }

    public void setModifiedData(HashMap<String, ?> modifiedData) {
        this.modifiedData = modifiedData;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
