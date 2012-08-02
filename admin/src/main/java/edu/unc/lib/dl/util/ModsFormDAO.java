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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.FactoryUtils;
import org.apache.commons.collections.list.LazyList;
import org.springframework.web.multipart.MultipartFile;

public class ModsFormDAO {
	private String pid;
	private String message;
	private String ingestMessage;
	private String ownerPid;
	private String filePath;
	private List breadcrumbs = new ArrayList();
   private List paths = new ArrayList();
   private String mods;
   
	public String getPid() {
		return pid;
	}
	public void setPid(String pid) {
		this.pid = pid;
	}

	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public String getOwnerPid() {
		return ownerPid;
	}
	public void setOwnerPid(String ownerPid) {
		this.ownerPid = ownerPid;
	}
	public List getBreadcrumbs() {
		return breadcrumbs;
	}
	public void setBreadcrumbs(List breadcrumbs) {
		this.breadcrumbs = breadcrumbs;
	}
	public List getPaths() {
		return paths;
	}
	public void setPaths(List paths) {
		this.paths = paths;
	}
	public String getFilePath() {
		return filePath;
	}
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public String getIngestMessage() {
		return ingestMessage;
	}

	public void setIngestMessage(String ingestMessage) {
		this.ingestMessage = ingestMessage;
	}
	public String getMods() {
		return mods;
	}
	public void setMods(String mods) {
		this.mods = mods;
	}
}
