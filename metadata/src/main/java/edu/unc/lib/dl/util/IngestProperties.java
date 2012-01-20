/**
 * Copyright 2011 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
	private int managedBytes = -1;
	private Map<PID, ContainerPlacement> containerPlacements = null;

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
		props.load(new FileInputStream(propFile));
		this.submitter = props.getProperty("submitter");
		String er = props.getProperty("email.recipients");
		if (er != null) {
			this.emailRecipients = er.split(",");
		}
		this.message = props.getProperty("message");
		String bytes = props.getProperty("managedBytes");
		if(bytes != null) {
			try {
				this.managedBytes = Integer.parseInt(bytes);
			} catch(NumberFormatException e) {}
		}
		this.containerPlacements = new HashMap<PID, ContainerPlacement>();
		for (Entry<Object, Object> e : props.entrySet()) {
			String key = (String) e.getKey();
			if (key.startsWith("placement")) {
				String s = (String) e.getValue();
				String[] vals = s.split(",");
				ContainerPlacement p = new ContainerPlacement();
				p.pid = new PID(vals[0]);
				p.parentPID = new PID(vals[1]);
				if(vals[2] != null && !"null".equals(vals[2])) {
					p.designatedOrder = Integer.parseInt(vals[2]);
				}
				if(vals[3] != null && !"null".equals(vals[3])) {
					p.sipOrder = Integer.parseInt(vals[3]);
				}
				this.containerPlacements.put(p.pid, p);
			}
		}
	}

	public void save() {
		if (log.isDebugEnabled()) {
			log.debug("saving ingest properties to " + this.propFile.getAbsolutePath());
		}
		Properties props = new Properties();
		if (this.submitter != null)
			props.put("submitter", this.submitter);
		if (this.emailRecipients != null && this.emailRecipients.length > 0) {
			StringBuilder sb = new StringBuilder();
			sb.append(this.emailRecipients[0]);
			for (int i = 1; i < this.emailRecipients.length; i++) {
				sb.append(",").append(this.emailRecipients[i]);
			}
			props.put("email.recipients", sb.toString());
		}
		if (this.message != null)
			props.put("message", this.message);
		if(this.managedBytes != -1) {
			props.put("managedBytes", String.valueOf(this.managedBytes));
		}
		if (this.containerPlacements != null && this.containerPlacements.size() > 0) {
			int count = 0;
			for (ContainerPlacement p : this.containerPlacements.values()) {
				StringBuilder sb = new StringBuilder();
				sb.append(p.pid).append(',').append(p.parentPID).append(',').append(p.designatedOrder).append(',')
						.append(p.sipOrder);
				props.put("placement." + count, sb.toString());
				count++;
			}
		}
		try {
			FileOutputStream f = new FileOutputStream(this.propFile);
			props.store(f, "This file provides properties to the batch ingest service.");
			f.flush();
			f.close();
		} catch (IOException e) {
			throw new Error("Unexpected", e);
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

	public int getManagedBytes() {
		return managedBytes;
	}

	public void setManagedBytes(int managedBytes) {
		this.managedBytes = managedBytes;
	}
}
