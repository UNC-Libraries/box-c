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

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import net.tanesha.recaptcha.ReCaptcha;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.support.SessionStatus;

import edu.unc.lib.dl.ui.model.ContactForm;
import edu.unc.lib.dl.ui.service.ContactEmailService;
import edu.unc.lib.dl.ui.validator.ContactFormValidator;

@Controller
@RequestMapping("/contact")
public class ContactController {
	@Autowired
	private ContactFormValidator validator;
	@Autowired
	private ReCaptcha reCaptcha;
	@Autowired
	@Qualifier("contactEmailService")
	private ContactEmailService emailService;
	
	@RequestMapping(method = RequestMethod.GET)
	public String initalizeForm(Model model, @RequestParam(value="refer", required = false) String referrer) {
		ContactForm contactForm = new ContactForm();
		contactForm.setReferrer(referrer);
		model.addAttribute("contactForm", contactForm);
		
		model.addAttribute("reCaptcha", this.reCaptcha.createRecaptchaHtml("", "clean", null));
		model.addAttribute("menuId", "contact");
		
		return "forms/contact";
	}
	
	@RequestMapping(method = RequestMethod.POST)
	public String submitForm(@ModelAttribute("contactForm") ContactForm contactForm,
			BindingResult results,
			Model model, SessionStatus status, HttpServletRequest request) {

		contactForm.setRemoteAddr(request.getServerName());
		model.addAttribute("contactForm", contactForm);
		model.addAttribute("menuId", "contact");

		// Validate form
		validator.validate(contactForm, results);

		if (results.hasErrors()) {
			model.addAttribute("reCaptcha", this.reCaptcha.createRecaptchaHtml("", "clean", null));
			return "forms/contact";
		}

		// Send email
		Map<String, Object> emailProperties = new HashMap<String, Object>();
		emailProperties.put("form", contactForm);
		emailProperties.put("serverName", request.getServerName());
		this.emailService.sendContactEmail(null, contactForm.getEmailAddress(), null, null, emailProperties);

		model.addAttribute("success", true);
		
		return "forms/contact";
	}

	public void setValidator(ContactFormValidator validator) {
		this.validator = validator;
	}

	public void setReCaptcha(ReCaptcha reCaptcha) {
		this.reCaptcha = reCaptcha;
	}

	public void setEmailService(ContactEmailService emailService) {
		this.emailService = emailService;
	}
}
