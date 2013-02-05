package edu.unc.lib.dl.ui.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadataBean;
import edu.unc.lib.dl.search.solr.model.SimpleIdRequest;
import edu.unc.lib.dl.ui.model.RequestAccessForm;
import edu.unc.lib.dl.ui.validator.RequestAccessFormValidator;

import org.springframework.validation.BindingResult;
import org.springframework.web.bind.support.SessionStatus;

@Controller
@RequestMapping("/requestAccess")
public class RequestAccessController extends AbstractSolrSearchController {
	private final Logger LOG = LoggerFactory.getLogger(RequestAccessController.class);
	
	@Autowired
	private RequestAccessFormValidator validator;

	@RequestMapping(value="/{idPrefix}/{id}", method = RequestMethod.GET)
	public String initalizeForm(@PathVariable("idPrefix") String idPrefix, @PathVariable("id") String idSuffix,
			Model model) {
		String id = idPrefix + ":" + idSuffix;
		LOG.debug("Initializing request access form, retrieving metadata for " + id);
		SimpleIdRequest idRequest = new SimpleIdRequest(id, GroupsThreadStore.getGroups());
		BriefObjectMetadataBean metadata = this.queryLayer.getObjectById(idRequest);

		RequestAccessForm requestAccessForm = new RequestAccessForm();

		model.addAttribute("requestAccessForm", requestAccessForm);
		model.addAttribute("metadata", metadata);

		LOG.debug("Forwarding to requestAccessForm");
		return "requestAccessForm";
	}

	@RequestMapping(method = RequestMethod.POST)
	public String submitForm(@ModelAttribute("requestAccessForm") RequestAccessForm requestAccessForm, BindingResult results,
			@PathVariable("idPrefix") String idPrefix, @PathVariable("id") String idSuffix, Model model,
			SessionStatus status) {

		// Validate form
		validator.validate(requestAccessForm, results);

		if (results.hasErrors()) {
			return "requestAccessForm";
		}
		
		// Send email

		return "requestAccessForm";
	}

	public void setValidator(RequestAccessFormValidator validator) {
		this.validator = validator;
	}
}
