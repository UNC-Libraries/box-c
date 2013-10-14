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
package edu.unc.lib.dl.ui.validator;

import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;

import edu.unc.lib.dl.ui.model.ContactForm;

public class ContactFormValidator extends AbstractContactFormValidator {
	@Override
	public boolean supports(Class<?> clazz) {
		return ContactForm.class.isAssignableFrom(clazz);
	}
	
	@Override
	public void validate(Object target, Errors errors) {
		if (target == null)
			return;
		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "personalName", "required.personalName");
		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "comments", "required.comments");
		ContactForm form = (ContactForm)target;
		validateEmail(form.getEmailAddress(), errors);
		validateRecaptcha(form.getRemoteAddr(), form.getRecaptcha_challenge_field(), form.getRecaptcha_response_field(), errors);
	}
}
