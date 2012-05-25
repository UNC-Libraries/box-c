package cdr.forms;

import edu.unc.lib.dl.fedora.PID;
import gov.loc.mods.mods.MODSFactory;
import gov.loc.mods.mods.ModsDefinition;

import java.security.Principal;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.multipart.MultipartFile;

import crosswalk.Form;
import crosswalk.FormElement;
import crosswalk.MetadataBlock;
import crosswalk.OutputElement;

@Controller
@RequestMapping(value = { "/*", "/**" })
@SessionAttributes("form")
public class FormController {

	private static final Logger LOG = LoggerFactory.getLogger(FormController.class);
	
	@Autowired
	private static PID defaultContainer = null;

	public static PID getDefaultContainer() {
		return defaultContainer;
	}

	public static void setDefaultContainer(PID defaultContainer) {
		FormController.defaultContainer = defaultContainer;
	}

	@Autowired
	AbstractFormFactory factory = null;

	public AbstractFormFactory getFactory() {
		return factory;
	}

	public void setFactory(AbstractFormFactory factory) {
		this.factory = factory;
	}

	@InitBinder
   protected void initBinder(WebDataBinder binder) {
       binder.setValidator(new FormValidator());
   }
	
	@ModelAttribute("form")
	protected Form getForm(@PathVariable String formId) {
		return factory.getForm(formId);
	}

	@RequestMapping(value = "/{formId}.form", method = RequestMethod.GET)
	public String showForm(@PathVariable String formId, @ModelAttribute("form") Form form) {
		LOG.debug("in GET for form " + formId);
		return "form";
	}

	@RequestMapping(value = "/{formId}.form", method = RequestMethod.POST)
	public String processForm(@PathVariable String formId, @Valid @ModelAttribute("form") Form form, BindingResult errors,
			Principal user, @RequestParam("file") MultipartFile file, SessionStatus sessionStatus) {
		LOG.debug("in POST for form " + formId);
		if (user != null) form.setCurrentUser(user.getName());
		if (errors.hasErrors()) {
			LOG.debug(errors.getErrorCount() + " errors");
			return "form";
		}
		if(file.isEmpty()) {
			errors.addError( new ObjectError("file", "You must select a file for upload."));
			return "form";
		}
		// run the mapping and get a MODS record. (report any errors)
		ModsDefinition mods = MODSFactory.eINSTANCE.createModsDefinition();
		for (FormElement fe : form.getElements()) {
			if(MetadataBlock.class.isInstance(fe)) {
				MetadataBlock mb = (MetadataBlock)fe;
				for(OutputElement oe : mb.getElements()) {
					oe.updateRecord(mods);
				}
			}
		}
		LOG.debug(mods.toString());
		// TODO create a METS SIP
		// TODO perform a sword submission.
		
		// TODO email notices
		
		// clear session
		sessionStatus.setComplete();
		return "success";
	}

}