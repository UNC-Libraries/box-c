package cdr.forms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import crosswalk.Form;
import crosswalk.FormElement;
import crosswalk.InputField;
import crosswalk.MetadataBlock;

public class FormValidator implements Validator {
	private static final Logger LOG = LoggerFactory.getLogger(FormValidator.class);

	@Override
	public boolean supports(Class clazz) {
		 return Form.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		Form form = (Form)target;
		for (FormElement el : form.getElements()) {
			if (MetadataBlock.class.isInstance(el)) {
				MetadataBlock mb = (MetadataBlock) el;
				int mbIdx = form.getElements().indexOf(mb);
				for (InputField in : mb.getPorts()) {
					int inIdx = mb.getPorts().indexOf(in);
					if(in.isRequired()) {
						StringBuilder path = new StringBuilder().append("elements[").append(mbIdx)
								.append("].ports[").append(inIdx).append("].enteredValue");
						LOG.debug(in.getLabel()+ " | " + path+ " is a required field");
						ValidationUtils.rejectIfEmptyOrWhitespace(errors, path.toString(), "field.required", "This field is required.");
					}
				}
			}
		}
	}

}
