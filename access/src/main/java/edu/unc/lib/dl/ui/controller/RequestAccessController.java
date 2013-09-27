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
package edu.unc.lib.dl.ui.controller;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import net.tanesha.recaptcha.ReCaptcha;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadataBean;
import edu.unc.lib.dl.search.solr.model.SimpleIdRequest;
import edu.unc.lib.dl.ui.model.RequestAccessForm;
import edu.unc.lib.dl.ui.service.ContactEmailService;
import edu.unc.lib.dl.ui.validator.RequestAccessFormValidator;

import org.springframework.validation.BindingResult;
import org.springframework.web.bind.support.SessionStatus;
import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/requestAccess")
public class RequestAccessController extends AbstractSolrSearchController {
	private final Logger LOG = LoggerFactory.getLogger(RequestAccessController.class);

	@Autowired
	private RequestAccessFormValidator validator;
	@Autowired
	private ReCaptcha reCaptcha;
	@Autowired
	@Qualifier("requestAccessEmailService")
	private ContactEmailService emailService;
	@Autowired
	@Qualifier("requestAccessUserResponseEmailService")
	private ContactEmailService userResponseEmailService;

	private void setFormAttributes(String id, Model model) {
		SimpleIdRequest idRequest = new SimpleIdRequest(id, GroupsThreadStore.getGroups());
		BriefObjectMetadataBean metadata = this.queryLayer.getObjectById(idRequest);

		model.addAttribute("metadata", metadata);
	}

	@RequestMapping(method = RequestMethod.GET)
	public String initalizeForm(Model model) {
		return this.initalizeForm(null, model);
	}

	@RequestMapping(value = "/{id}", method = RequestMethod.GET)
	public String initalizeForm(@PathVariable("id") String id, Model model) {
		RequestAccessForm requestAccessForm = new RequestAccessForm();
		model.addAttribute("requestAccessForm", requestAccessForm);

		LOG.debug("Initializing request access form, retrieving metadata for " + id);
		if (id != null)
			this.setFormAttributes(id, model);
		model.addAttribute("reCaptcha", this.reCaptcha.createRecaptchaHtml("", "clean", null));

		model.addAttribute("menuId", "contact");

		return "forms/requestAccessForm";
	}

	@RequestMapping(method = RequestMethod.POST)
	public String submitForm(@ModelAttribute("requestAccessForm") RequestAccessForm requestAccessForm,
			BindingResult results, Model model, SessionStatus status, HttpServletRequest request) {
		return this.submitForm(requestAccessForm, results, null, model, status, request);
	}

	@RequestMapping(value = "/{id}", method = RequestMethod.POST)
	public String submitForm(@ModelAttribute("requestAccessForm") RequestAccessForm requestAccessForm,
			BindingResult results, @PathVariable("id") String id,
			Model model, SessionStatus status, HttpServletRequest request) {

		requestAccessForm.setRemoteAddr(request.getServerName());
		model.addAttribute("requestAccessForm", requestAccessForm);
		model.addAttribute("menuId", "contact");

		// Validate form
		validator.validate(requestAccessForm, results);

		if (id != null)
			this.setFormAttributes(id, model);

		if (results.hasErrors()) {
			model.addAttribute("reCaptcha", this.reCaptcha.createRecaptchaHtml("", "clean", null));
			return "forms/requestAccessForm";
		}

		// Send email
		Map<String, Object> emailProperties = new HashMap<String, Object>();
		emailProperties.put("form", requestAccessForm);
		emailProperties.put("serverName", request.getServerName());
		this.emailService.sendContactEmail(null, requestAccessForm.getEmailAddress(), null, null, emailProperties);

		// Send user confirmation email
		this.userResponseEmailService.sendContactEmail(null, null, null,
				Arrays.asList(requestAccessForm.getEmailAddress()), emailProperties);

		model.addAttribute("success", true);

		return "forms/requestAccessForm";
	}

	public void setValidator(RequestAccessFormValidator validator) {
		this.validator = validator;
	}

	public void setEmailService(ContactEmailService emailService) {
		this.emailService = emailService;
	}
}
