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
package edu.unc.lib.dl.cdr.services.model;

import java.util.Date;

import org.joda.time.format.ISODateTimeFormat;

import edu.unc.lib.dl.fedora.PID;

/**
 * Stores data related to a specific application of an enhancement
 * @author bbpennel
 *
 */
public class EnhancementApplication {
	//Subject of this enhancement
	private PID pid;
	//Last date on which this enhancement was applied
	private Date lastApplied;
	//Version of services when this enhancement was last run
	private String version;
	//Class of the enhancement applied
	private Class<?> enhancementClass;
	
	public PID getPid() {
		return pid;
	}
	public void setPid(PID pid) {
		this.pid = pid;
	}
	public Date getLastApplied() {
		return lastApplied;
	}
	public void setLastApplied(Date lastApplied) {
		this.lastApplied = lastApplied;
	}
	public void setLastAppliedFromISO8601(String lastApplied){
		this.lastApplied = ISODateTimeFormat.dateTimeParser().withOffsetParsed().parseDateTime(lastApplied).toDate();
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public Class<?> getEnhancementClass() {
		return enhancementClass;
	}
	public void setEnhancementClass(Class<?> enhancementClass) {
		this.enhancementClass = enhancementClass;
	}
}
