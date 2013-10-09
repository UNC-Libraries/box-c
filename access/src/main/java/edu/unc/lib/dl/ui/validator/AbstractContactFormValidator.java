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

import java.util.regex.Pattern;

import net.tanesha.recaptcha.ReCaptcha;
import net.tanesha.recaptcha.ReCaptchaResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

public abstract class AbstractContactFormValidator implements Validator {
	private final Logger LOG = LoggerFactory.getLogger(AbstractContactFormValidator.class);
	protected static Pattern emailRegex = Pattern.compile("\\b[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,4}\\b");
	@Autowired
	protected ReCaptcha reCaptcha;
	
	protected void validateEmail(String email, Errors errors) {
		if (!emailRegex.matcher(email).matches()) {
			errors.rejectValue("emailAddress", "invalid.emailAddress");
		}
	}
	
	protected void validateRecaptcha(String remoteAddr, String challengeField, String responseField, Errors errors) {
		ReCaptchaResponse response = reCaptcha.checkAnswer(remoteAddr, challengeField, responseField);
		
		if (!response.isValid()) {
			LOG.debug("Recaptcha validation failed because: " + response.getErrorMessage());
			errors.rejectValue("recaptcha_challenge_field", "incorrect.recaptcha");
		}
	}
}
