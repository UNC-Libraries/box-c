package edu.unc.lib.dl.ui.validator;

import java.util.regex.Pattern;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.validation.ValidationUtils;

import edu.unc.lib.dl.ui.model.RequestAccessForm;

public class RequestAccessFormValidator implements Validator {

	private static Pattern emailRegex = Pattern.compile("\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,4}\\b");
	
	@Override
	public boolean supports(Class<?> clazz) {
		return RequestAccessForm.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "personalName", "required.personalName");
		RequestAccessForm form = (RequestAccessForm)target;
		if (!emailRegex.matcher(form.getEmailAddress()).matches()) {
			errors.rejectValue("emailAddress", "invalid.emailAddress");
		}
	}

}
