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

import org.springframework.web.multipart.MultipartFile;

public class UpdateFileDAO {
	private MultipartFile sourceFile;
	private String id;
	private String label;
	private String originalLabel;
	private String checksum;

	public MultipartFile getSourceFile() {
		return sourceFile;
	}
	public void setSourceFile(MultipartFile sourceFile) {
		this.sourceFile = sourceFile;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getLabel() {
		return label;
	}
	public String getOriginalLabel() {
		return originalLabel;
	}
	public void setOriginalLabel(String originalLabel) {
		this.originalLabel = originalLabel;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public String getChecksum() {
		return checksum;
	}
	public void setChecksum(String checksum) {
		this.checksum = checksum;
	}
}
