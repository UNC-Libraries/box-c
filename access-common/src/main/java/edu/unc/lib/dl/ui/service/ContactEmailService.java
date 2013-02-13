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
package edu.unc.lib.dl.ui.service;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * Email sending service dedicated to generating and sending a single email template.
 * 
 * @author bbpennel
 *
 */
public class ContactEmailService {

	private JavaMailSender mailSender;
	private Configuration freemarkerConfiguration;

	private String defaultSubjectLine;
	private String fromAddress;
	private String fromName;
	private List<String> emailRecipients;
	private Template htmlTemplate;
	private Template textTemplate;

	/**
	 * Sends an email to any number of recipients using the configured email templates
	 * 
	 * @param subjectLine The subject line of the email.  If not provided, the configured default will be used.
	 * @param fromAddress The reply-to address for the email.  If not provided, the configured default will be used.
	 * @param fromName The display name for the sender of the email.  If not provided, the configured default will be used.
	 * @param emailRecipients List of email addresses to send the email to
	 * @param model Map containing the input parameters for the email template to use
	 */
	public void sendContactEmail(String subjectLine, String fromAddress, String fromName, List<String> emailRecipients,
			Map<String, Object> model) {
		try {
			StringWriter sw;
			String html, text;
			
			// Generate the email templates
			if (htmlTemplate != null) {
				sw = new StringWriter();
				htmlTemplate.process(model, sw);
				html = sw.toString();
			} else {
				html = null;
			}

			if (textTemplate != null) {
				sw = new StringWriter();
				textTemplate.process(model, sw);
				text = sw.toString();
			} else {
				text = null;
			}

			MimeMessage mimeMessage = mailSender.createMimeMessage();
			MimeMessageHelper message = new MimeMessageHelper(mimeMessage, MimeMessageHelper.MULTIPART_MODE_MIXED);

			if (emailRecipients == null)
				emailRecipients = this.emailRecipients;
			for (String address : emailRecipients) {
				message.addTo(address);
			}

			if (subjectLine == null)
				message.setSubject(this.defaultSubjectLine);
			else
				message.setSubject(subjectLine);

			// Use the default email address if one isn't supplied in the request
			if (fromAddress != null) {
				if (fromName == null)
					message.setFrom(fromAddress);
				else
					message.setFrom(fromAddress, fromName);
			} else {
				if (this.fromName == null)
					message.setFrom(this.fromAddress);
				else
					message.setFrom(this.fromAddress, this.fromName);
			}

			// Allow for html, text, or html/text bodies
			if (html != null && text != null) {
				message.setText(text, html);
			} else if (html != null) {
				message.setText(html, true);
			} else {
				message.setText(text);
			}

			this.mailSender.send(mimeMessage);
		} catch (IOException e) {
			throw new Error("Unable to load email template for Ingest Success", e);
		} catch (TemplateException e) {
			throw new Error("There was a problem loading FreeMarker templates for email notification", e);
		} catch (MessagingException e) {
			throw new Error("Unable to send contact email", e);
		}
	}

	public void setMailSender(JavaMailSender mailSender) {
		this.mailSender = mailSender;
	}

	public void setDefaultSubjectLine(String defaultSubjectLine) {
		this.defaultSubjectLine = defaultSubjectLine;
	}

	public void setFromAddress(String fromAddress) {
		this.fromAddress = fromAddress;
	}

	public void setFromName(String fromName) {
		this.fromName = fromName;
	}

	public void setEmailRecipients(List<String> emailRecipients) {
		this.emailRecipients = emailRecipients;
	}

	public void setHtmlTemplatePath(String path) throws IOException {
		htmlTemplate = this.freemarkerConfiguration.getTemplate(path, Locale.getDefault(), "utf-8");
	}

	public void setTextTemplatePath(String path) throws IOException {
		textTemplate = this.freemarkerConfiguration.getTemplate(path, Locale.getDefault(), "utf-8");
	}

	public void setHtmlTemplate(Template htmlTemplate) {
		this.htmlTemplate = htmlTemplate;
	}

	public void setTextTemplate(Template textTemplate) {
		this.textTemplate = textTemplate;
	}

	public void setFreemarkerConfiguration(Configuration freemarkerConfiguration) {
		this.freemarkerConfiguration = freemarkerConfiguration;
	}
}
