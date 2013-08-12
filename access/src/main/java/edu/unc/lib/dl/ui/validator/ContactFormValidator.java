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
		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "personalName", "required.personalName");
		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "comments", "required.comments");
		ContactForm form = (ContactForm)target;
		validateEmail(form.getEmailAddress(), errors);
		validateRecaptcha(form.getRemoteAddr(), form.getRecaptcha_challenge_field(), form.getRecaptcha_response_field(), errors);
	}
}
