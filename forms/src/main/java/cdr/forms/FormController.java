package cdr.forms;

import edu.unc.lib.dl.fedora.PID;
import gov.loc.mods.mods.MODSFactory;
import gov.loc.mods.mods.ModsDefinition;

import java.io.IOException;
import java.io.StringWriter;
import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.validation.Valid;

import org.eclipse.emf.ecore.xmi.XMLResource;
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
	
	public FormController() {
		LOG.debug("FormController created");
	}

	private static final Logger LOG = LoggerFactory.getLogger(FormController.class);
	
	@Autowired
	private DepositHandler depositHandler;
	
	public DepositHandler getDepositHandler() {
		return depositHandler;
	}

	public void setDepositHandler(DepositHandler depositHandler) {
		this.depositHandler = depositHandler;
	}

	@Autowired
	private PID defaultContainer = null;

	public PID getDefaultContainer() {
		return defaultContainer;
	}

	public void setDefaultContainer(PID defaultContainer) {
		this.defaultContainer = defaultContainer;
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
		String mods = makeMods(form);
		LOG.debug(mods);
		// perform a deposit with the default handler.
		try {
			this.getDepositHandler().deposit(form.getDepositContainerId(), mods, file.getInputStream());
		} catch (IOException e) {
			throw new Error("temporary file upload storage failed", e);
		}
		
		// TODO email notices
		
		// clear session
		sessionStatus.setComplete();
		return "success";
	}

	private String makeMods(Form form) {
		String result;
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
		StringWriter sw = new StringWriter();
		Map<Object, Object> options = new HashMap<Object, Object>();
		options.put(XMLResource.OPTION_ENCODING, "utf-8");
		options.put(XMLResource.OPTION_LINE_WIDTH, new Integer(80));
		options.put(XMLResource.OPTION_ROOT_OBJECTS, Collections.singletonList(mods));
		try {
			((XMLResource) mods.eResource()).save(sw, options);
		} catch (IOException e) {
			sw.append("failed to serialize XML for model object");
		}
		result = sw.toString();
		return result;
	}

}