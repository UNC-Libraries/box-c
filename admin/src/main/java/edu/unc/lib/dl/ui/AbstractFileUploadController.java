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
package edu.unc.lib.dl.ui;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.SimpleFormController;

import edu.unc.lib.dl.services.FolderManager;
import edu.unc.lib.dl.ui.util.UiUtilityMethods;
import edu.unc.lib.dl.ui.ws.UiWebService;

public abstract class AbstractFileUploadController extends SimpleFormController {

	protected UiWebService uiWebService;
	protected FolderManager folderManager;
	protected UiUtilityMethods uiUtilityMethods;

	protected String writeFile(MultipartFile file, String extension) {
		Calendar now = Calendar.getInstance();
		String fileName = new String(Long.toString(now.getTimeInMillis()));
			
		try {
			File temp = File.createTempFile(fileName, extension);

			fileName = temp.getCanonicalPath();
				
			FileOutputStream fos = new FileOutputStream(temp);
			
			fos.write(file.getBytes());
			fos.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
			return null;
		}
		
		return fileName;
	}

	public UiWebService getUiWebService() {
		return uiWebService;
	}

	public void setUiWebService(UiWebService uiWebService) {
		this.uiWebService = uiWebService;
	}

	public FolderManager getFolderManager() {
		return folderManager;
	}

	public void setFolderManager(FolderManager folderManager) {
		this.folderManager = folderManager;
	}

	public UiUtilityMethods getUiUtilityMethods() {
		return uiUtilityMethods;
	}

	public void setUiUtilityMethods(UiUtilityMethods uiUtilityMethods) {
		this.uiUtilityMethods = uiUtilityMethods;
	}
}
