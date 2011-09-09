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
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;
import org.springframework.web.multipart.MultipartFile;

import edu.unc.lib.dl.util.MediatedSubmitDAO;

/**
 * @author steve
 * 
 */
public class CreateCollectionValidator implements Validator {

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
		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "filePath",
				"submit.repository.path.missing");

		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "ownerPid",
				"submit.owner.missing");

		MediatedSubmitDAO dao = (MediatedSubmitDAO) target;

		MultipartFile metadata = dao.getMetadata();

		if ((metadata == null) || (metadata.getSize() < 1)) {
			errors.rejectValue("metadata", "submit.metadata.missing");
		}

		String path = dao.getFilePath();
		if(path.contains("/")) {
			errors.rejectValue("filePath", "submit.collection.folder.only");
		}
//		else {
//			if (path.matches(".*\\s.*")) {
//				errors.rejectValue("filePath", "submit.filepath.whitespace");
//			}
//		}
	}

}
