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

public class FixityReplicationDAO {
	private String message;
	private String ownerPid;
	private MultipartFile goodReplicationFile;
	private MultipartFile badReplicationFile;
	private MultipartFile goodFixityFile;
	private MultipartFile badFixityFile;

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
	public MultipartFile getGoodReplicationFile() {
		return goodReplicationFile;
	}
	public void setGoodReplicationFile(MultipartFile goodReplicationFile) {
		this.goodReplicationFile = goodReplicationFile;
	}
	public MultipartFile getBadReplicationFile() {
		return badReplicationFile;
	}
	public void setBadReplicationFile(MultipartFile badReplicationFile) {
		this.badReplicationFile = badReplicationFile;
	}
	public MultipartFile getGoodFixityFile() {
		return goodFixityFile;
	}
	public void setGoodFixityFile(MultipartFile goodFixityFile) {
		this.goodFixityFile = goodFixityFile;
	}
	public MultipartFile getBadFixityFile() {
		return badFixityFile;
	}
	public void setBadFixityFile(MultipartFile badFixityFile) {
		this.badFixityFile = badFixityFile;
	}
}
