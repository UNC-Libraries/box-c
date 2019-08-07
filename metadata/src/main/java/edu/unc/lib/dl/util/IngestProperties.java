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
package edu.unc.lib.dl.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.unc.lib.dl.fedora.PID;

/**
 * @author Gregory Jansen
 *
 */
public class IngestProperties {
    private static final Log log = LogFactory.getLog(IngestProperties.class);
    private static final String PROPERTIES_FILE = "ingest.properties";
    private File propFile = null;

    private String submitter = null;
    private String[] emailRecipients = null;
    private String message = null;
    private String originalDepositId = null;
    private long submissionTime = -1;
    private String submitterGroups = null;
    /**
     * Not yet recorded
     */
    private long managedBytes = -1;
    private Map<PID, ContainerPlacement> containerPlacements = null;
    private long finishedTime = -1;
    private long startTime = -1;

    public IngestProperties(File baseDir) throws Exception {
        this.propFile = new File(baseDir, PROPERTIES_FILE);
        if (this.propFile.exists()) {
            load();
        }
    }

    /**
     * @throws IOException
     * @throws FileNotFoundException
     * @throws ClassNotFoundException
     *
     */
    private void load() throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("loading ingest properties from " + this.propFile.getAbsolutePath());
        }
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(propFile)) {
            props.load(in);
        }
        this.submitter = props.getProperty("submitter");
        String submitterGroups = props.getProperty("submitterGroups");
        if (submitterGroups != null) {
            this.submitterGroups = submitterGroups;
        }
        String er = props.getProperty("email.recipients");
        if (er != null) {
            this.emailRecipients = er.split(",");
        }
        this.message = props.getProperty("message");
        String bytes = props.getProperty("managedBytes");
        if (bytes != null) {
            try {
                this.managedBytes = Long.parseLong(bytes);
            } catch (NumberFormatException e) {
            }
        }
        String subTime = props.getProperty("submissionTime");
        if (subTime != null) {
            try {
                this.submissionTime = Long.parseLong(subTime);
            } catch (NumberFormatException e) {
                throw new Error("Unexpected submissionTime exception", e);
            }
        }
        String finTime = props.getProperty("finishedTime");
        if (finTime != null) {
            try {
                this.finishedTime = Long.parseLong(finTime);
            } catch (NumberFormatException e) {
                throw new Error("Unexpected finishedTime exception", e);
            }
        }
        String stTime = props.getProperty("startTime");
        if (stTime != null) {
            try {
                this.startTime = Long.parseLong(stTime);
            } catch (NumberFormatException e) {
                throw new Error("Unexpected startTime exception", e);
            }
        }
        this.originalDepositId = props.getProperty("originalDepositId");
        this.containerPlacements = new HashMap<PID, ContainerPlacement>();
        for (Entry<Object, Object> e : props.entrySet()) {
            String key = (String) e.getKey();
            if (key.startsWith("placement")) {
                String s = (String) e.getValue();
                String[] vals = s.split(",");
                ContainerPlacement p = new ContainerPlacement();
                p.pid = new PID(vals[0]);
                p.parentPID = new PID(vals[1]);
                if (2 < vals.length && !"null".equals(vals[2])) {
                    p.designatedOrder = Integer.parseInt(vals[2]);
                }
                if (3 < vals.length && !"null".equals(vals[3])) {
                    p.sipOrder = Integer.parseInt(vals[3]);
                }
                if (4 < vals.length && !"null".equals(vals[4])) {
                    p.label = vals[4];
                }
                this.containerPlacements.put(p.pid, p);
            }
        }
    }

    public void save() {
        if (log.isDebugEnabled()) {
            log.debug("saving ingest properties to "
                    + this.propFile.getAbsolutePath());
        }
        Properties props = new Properties();
        if (this.submitter != null) {
            props.put("submitter", this.submitter);
        }
        if (this.submitterGroups != null) {
            props.put("submitterGroups", this.submitterGroups);
        }
        if (this.emailRecipients != null && this.emailRecipients.length > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append(this.emailRecipients[0]);
            for (int i = 1; i < this.emailRecipients.length; i++) {
                sb.append(",").append(this.emailRecipients[i]);
            }
            props.put("email.recipients", sb.toString());
        }
        if (this.message != null) {
            props.put("message", this.message);
        }
        if (this.originalDepositId != null) {
            props.put("originalDepositId", this.originalDepositId);
        }
        if (this.managedBytes != -1) {
            props.put("managedBytes", String.valueOf(this.managedBytes));
        }
        if (this.submissionTime != -1) {
            props.put("submissionTime", String.valueOf(this.submissionTime));
        }
        if (this.finishedTime != -1) {
            props.put("finishedTime", String.valueOf(this.finishedTime));
        }
        if (this.startTime != -1) {
            props.put("startTime", String.valueOf(this.startTime));
        }
        if (this.containerPlacements != null
                && this.containerPlacements.size() > 0) {
            int count = 0;
            for (ContainerPlacement p : this.containerPlacements.values()) {
                StringBuilder sb = new StringBuilder();
                sb.append(p.pid).append(',').append(p.parentPID).append(',')
                        .append(p.designatedOrder).append(',')
                        .append(p.sipOrder).append(",").append(p.label);
                props.put("placement." + count, sb.toString());
                count++;
            }
        }
        FileOutputStream f = null;
        try {
            f = new FileOutputStream(this.propFile);
            props.store(f,
                    "This file provides properties to the batch ingest service.");
        } catch (IOException e) {
            throw new Error("Unexpected", e);
        } finally {
            if (f != null) {
                try {
                    f.flush();
                    f.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    public void save(File newFile) {
        this.propFile = newFile;
        this.save();
    }

    public String getSubmitter() {
        return submitter;
    }

    public void setSubmitter(String submitter) {
        this.submitter = submitter;
    }

    public String[] getEmailRecipients() {
        return emailRecipients;
    }

    public void setEmailRecipients(String[] emailRecipients) {
        this.emailRecipients = emailRecipients;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<PID, ContainerPlacement> getContainerPlacements() {
        return containerPlacements;
    }

    public void setContainerPlacements(Map<PID, ContainerPlacement> placements) {
        this.containerPlacements = placements;
    }

    public long getManagedBytes() {
        return managedBytes;
    }

    public void setManagedBytes(int managedBytes) {
        this.managedBytes = managedBytes;
    }

    public String getOriginalDepositId() {
        return originalDepositId;
    }

    public void setOriginalDepositId(String originalDepositId) {
        this.originalDepositId = originalDepositId;
    }

    public long getSubmissionTime() {
        return submissionTime;
    }

    public void setSubmissionTime(long submissionTime) {
        this.submissionTime = submissionTime;
    }

    public String getSubmitterGroups() {
        return submitterGroups;
    }

    public void setSubmitterGroups(String submitterGroups) {
        this.submitterGroups = submitterGroups;
    }

    /**
     * @param finishedTime
     */
    public void setFinishedTime(long finishedTime) {
        this.finishedTime = finishedTime;
    }

    public long getFinishedTime() {
        return finishedTime;
    }

    /**
     * @param startTime
     */
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getStartTime() {
        return startTime;
    }
}
