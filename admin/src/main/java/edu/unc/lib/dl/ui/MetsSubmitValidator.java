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
/**
 * 
 */
package edu.unc.lib.dl.ui;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.multipart.MultipartFile;

import edu.unc.lib.dl.util.MediatedSubmitDAO;

/**
 * @author steve
 * 
 */
public class MetsSubmitValidator implements Validator {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.validation.Validator#supports(java.lang.Class)
	 */
	public boolean supports(Class arg0) {
		return MediatedSubmitDAO.class.isAssignableFrom(arg0);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.validation.Validator#validate(java.lang.Object,
	 *      org.springframework.validation.Errors)
	 */
	public void validate(Object target, Errors errors) {

		MediatedSubmitDAO dao = (MediatedSubmitDAO) target;

//		if (dao.getFilePath().matches(".*\\s.*")) {
//			errors.rejectValue("filePath", "submit.filepath.whitespace");
//		}

		MultipartFile file = dao.getFile();

		if ((file == null) || (file.getSize() < 1)) {
			errors.rejectValue("file", "submit.file.missing");
		} else {
			String name = file.getOriginalFilename().toLowerCase();

			if (!name.endsWith(".zip")) {
				if (!name.endsWith(".xml")) {
					errors.rejectValue("file", "submit.file.ziporxml");
				}
			}
		}
		
		String parentPid = dao.getParentPid();
		
		if((parentPid == null) || (parentPid.equals(""))) {
			errors.rejectValue("parentPid", "submit.parentpid.missing");
		}
	}

}
