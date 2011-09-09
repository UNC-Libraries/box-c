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

public class UpdateObjectDAO {
	private String pid;
	private UpdateFileDAO metadata;
	private String message;
	private String ownerPid;
	private List users = new ArrayList();
	private List groups = new ArrayList();
    private List breadcrumbs = new ArrayList();
    private List paths = new ArrayList();
	private List<UpdateFileDAO> files = LazyList.decorate( new ArrayList<UpdateFileDAO>(), FactoryUtils.instantiateFactory(UpdateFileDAO.class));

	public String getPid() {
		return pid;
	}
	public void setPid(String pid) {
		this.pid = pid;
	}
	
	public UpdateFileDAO getMetadata() {
		return metadata;
	}
	public void setMetadata(UpdateFileDAO metadata) {
		this.metadata = metadata;
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
	public List getUsers() {
		return users;
	}
	public void setUsers(List users) {
		this.users = users;
	}
	public List getGroups() {
		return groups;
	}
	public void setGroups(List groups) {
		this.groups = groups;
	}
	public List getBreadcrumbs() {
		return breadcrumbs;
	}
	public void setBreadcrumbs(List breadcrumbs) {
		this.breadcrumbs = breadcrumbs;
	}
	public List<UpdateFileDAO> getFiles() {
		return files;
	}
	public void setFiles(List<UpdateFileDAO> files) {
		this.files = files;
	}
	public List getPaths() {
		return paths;
	}
	public void setPaths(List paths) {
		this.paths = paths;
	}
}

