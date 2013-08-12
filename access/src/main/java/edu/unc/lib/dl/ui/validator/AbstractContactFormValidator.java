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
