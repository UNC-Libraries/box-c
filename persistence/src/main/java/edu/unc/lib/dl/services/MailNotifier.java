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
/**
 *
 */
package edu.unc.lib.dl.services;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.output.DOMOutputter;
import org.jdom.output.XMLOutputter;
import org.springframework.core.io.InputStreamSource;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

import edu.unc.lib.dl.ingest.IngestException;
import edu.unc.lib.dl.ingest.sip.FilesDoNotMatchManifestException;
import edu.unc.lib.dl.ingest.sip.InvalidMETSException;
import edu.unc.lib.dl.ingest.sip.METSParseException;
import edu.unc.lib.dl.util.ContainerPlacement;
import edu.unc.lib.dl.util.IngestProperties;
import freemarker.ext.dom.NodeModel;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * Prepares a email that reports a successful CDR ingest. Events log is
 * attached. Email is dual-mode plain text and html. Each email is templated
 * with the Freemarker template system.
 *
 * @see http://www.freemarker.org/docs/index.html
 *
 * @author Gregory Jansen
 *
 */
/**
 * @author Gregory Jansen
 *
 */
public class MailNotifier {
	private static final Log log = LogFactory.getLog(MailNotifier.class);
	private String irBaseUrl = null;
	private JavaMailSender mailSender = null;
	private Configuration freemarkerConfiguration = null;
	private String repositoryFromAddress = null;
	private String administratorAddress = null;

	public String getRepositoryFromAddress() {
		return repositoryFromAddress;
	}

	public void setRepositoryFromAddress(String repositoryFromAddress) {
		this.repositoryFromAddress = repositoryFromAddress;
	}

	public String getAdministratorAddress() {
		return administratorAddress;
	}

	public void setAdministratorAddress(String administratorAddress) {
		this.administratorAddress = administratorAddress;
	}

	public Configuration getFreemarkerConfiguration() {
		return freemarkerConfiguration;
	}

	public void setFreemarkerConfiguration(Configuration freemarkerConfiguration) {
		this.freemarkerConfiguration = freemarkerConfiguration;
	}

	public String getIrBaseUrl() {
		return irBaseUrl;
	}

	public void setIrBaseUrl(String irBaseUrl) {
		this.irBaseUrl = irBaseUrl;
	}

	public JavaMailSender getMailSender() {
		return mailSender;
	}

	public void setMailSender(JavaMailSender mailSender) {
		this.mailSender = mailSender;
	}

	public MailNotifier() {
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.springframework.mail.javamail.MimeMessagePreparator#prepare(javax .mail.internet.MimeMessage)
	 */
	public void sendIngestSuccessNotice(IngestProperties props, int ingestedCount) {
		String html = null, text = null;
		boolean logEmail = true;
		MimeMessage mimeMessage = null;
		try {
			Template htmlTemplate = this.freemarkerConfiguration.getTemplate("IngestSuccessHtml.ftl", Locale.getDefault(),
					"utf-8");
			Template textTemplate = this.freemarkerConfiguration.getTemplate("IngestSuccessText.ftl", Locale.getDefault(),
					"utf-8");

			// put data into the model
			HashMap<String, Object> model = new HashMap<String, Object>();
			model.put("numberOfObjects", new Integer(ingestedCount));
			model.put("irBaseUrl", this.irBaseUrl);
			List tops = new ArrayList();
			for(ContainerPlacement p : props.getContainerPlacements().values()) {
				HashMap om = new HashMap();
				om.put("pid", p.pid.getPid());
				om.put("label", p.label);
				tops.add(om);
			}
			model.put("tops", tops);

			StringWriter sw = new StringWriter();
			htmlTemplate.process(model, sw);
			html = sw.toString();
			sw = new StringWriter();
			textTemplate.process(model, sw);
			text = sw.toString();
		} catch (IOException e1) {
			throw new Error("Unable to load email template for Ingest Success", e1);
		} catch (TemplateException e) {
			throw new Error("There was a problem loading FreeMarker templates for email notification", e);
		}

		try {
			if (log.isDebugEnabled() && this.mailSender instanceof JavaMailSenderImpl) {
				((JavaMailSenderImpl) this.mailSender).getSession().setDebug(true);
			}
			mimeMessage = mailSender.createMimeMessage();
			MimeMessageHelper message = new MimeMessageHelper(mimeMessage, MimeMessageHelper.MULTIPART_MODE_MIXED);

			for(String addy : props.getEmailRecipients()) {
				message.addTo(addy);
			}
			message.setSubject("CDR ingest complete");

			message.setFrom("cdr@unc.edu");
			message.setText(text, html);

			// attach Events XML
			// Document events = new Document(aip.getEventLogger().getAllEvents());
			// message.addAttachment("events.xml", new JDOMStreamSource(events));
			this.mailSender.send(mimeMessage);
			logEmail = false;
		} catch (MessagingException e) {
			log.error("Unable to send ingest success email.", e);
		} catch (RuntimeException e) {
			log.error(e);
		} finally {
			if (mimeMessage != null && logEmail) {
				try {
					mimeMessage.writeTo(System.out);
				} catch (Exception e) {
					log.error("Could not log email message after error.", e);
				}
			}
		}

	}

	/**
	 * @param e
	 * @param user
	 */
	public void sendIngestFailureNotice(Throwable ex, IngestProperties props) {
		String html = null, text = null;
		MimeMessage mimeMessage = null;
		boolean logEmail = true;
		try {
			// create templates
			Template htmlTemplate = this.freemarkerConfiguration.getTemplate("IngestFailHtml.ftl", Locale.getDefault(),
					"utf-8");
			Template textTemplate = this.freemarkerConfiguration.getTemplate("IngestFailText.ftl", Locale.getDefault(),
					"utf-8");

			if (log.isDebugEnabled() && this.mailSender instanceof JavaMailSenderImpl) {
				((JavaMailSenderImpl) this.mailSender).getSession().setDebug(true);
			}

			// create mail message
			mimeMessage = mailSender.createMimeMessage();
			MimeMessageHelper message = new MimeMessageHelper(mimeMessage, MimeMessageHelper.MULTIPART_MODE_MIXED);

			// put data into the model
			HashMap<String, Object> model = new HashMap<String, Object>();
			model.put("irBaseUrl", this.irBaseUrl);
/*			List<ContainerPlacement> tops = new ArrayList<ContainerPlacement>();
			tops.addAll(props.getContainerPlacements().values());
			model.put("tops", tops);*/

			if (ex != null && ex.getMessage() != null) {
				model.put("message", ex.getMessage());
			} else if (ex != null) {
				model.put("message", ex.toString());
			} else {
				model.put("message", "No exception or error message available.");
			}

			// specific exception processing
			if (ex instanceof FilesDoNotMatchManifestException) {
				model.put("FilesDoNotMatchManifestException", ex);
			} else if (ex instanceof InvalidMETSException) {
				model.put("InvalidMETSException", ex);
				InvalidMETSException ime = (InvalidMETSException) ex;
				if (ime.getSvrl() != null) {
					Document jdomsvrl = ((InvalidMETSException) ex).getSvrl();
					DOMOutputter domout = new DOMOutputter();
					try {
						org.w3c.dom.Document svrl = domout.output(jdomsvrl);
						model.put("svrl", NodeModel.wrap(svrl));

						// also dump SVRL to attachment
						message.addAttachment("schematron-output.xml", new JDOMStreamSource(jdomsvrl));
					} catch (JDOMException e) {
						throw new Error(e);
					}
				}
			} else if (ex instanceof METSParseException) {
				log.debug("putting MPE in the model");
				model.put("METSParseException", ex);
			} else {
				log.debug("IngestException without special email treatment", ex);
			}

			// attach error xml if available
			if (ex instanceof IngestException) {
				IngestException ie = (IngestException) ex;
				if (ie.getErrorXML() != null) {
					message.addAttachment("error.xml", new JDOMStreamSource(ie.getErrorXML()));
				}
			}

			model.put("user", props.getSubmitter());
			model.put("irBaseUrl", this.irBaseUrl);

			StringWriter sw = new StringWriter();
			htmlTemplate.process(model, sw);
			html = sw.toString();
			sw = new StringWriter();
			textTemplate.process(model, sw);
			text = sw.toString();

			// Addressing: to initiator if a person, otherwise to all members of
			// admin group
			if(props.getEmailRecipients() != null) {
				for(String r : props.getEmailRecipients()) {
					message.addTo(r);
				}
				message.setSubject("CDR ingest failed");
			} else {
				message.addTo("cdr@unc.edu", "CDR Administrator");
				message.setSubject("CDR non-user initiated ingest failed");
			}

			message.setFrom("cdr@unc.edu");
			message.setText(text, html);

			// attach Events XML
			// if (aip != null) {
			// /message.addAttachment("events.xml", new JDOMStreamSource(aip.getEventLogger().getAllEvents()));
			// }
			this.mailSender.send(mimeMessage);
			logEmail = false;
		} catch (MessagingException e) {
			log.error("Unable to send ingest fail email.", e);
		} catch (MailSendException e) {
			log.error("Unable to send ingest fail email.", e);
		} catch (UnsupportedEncodingException e) {
			log.error("Unable to send ingest fail email.", e);
		} catch (IOException e1) {
			throw new Error("Unable to load email template for Ingest Failure", e1);
		} catch (TemplateException e) {
			throw new Error("There was a problem loading FreeMarker templates for email notification", e);
		} finally {
			if (mimeMessage != null && logEmail) {
				try {
					mimeMessage.writeTo(System.out);
				} catch (Exception e) {
					log.error("Could not log email message after error.", e);
				}
			}
		}
	}

	class JDOMStreamSource implements InputStreamSource {
		public Document xml;

		@Override
		public InputStream getInputStream() throws IOException {
			try {
				XMLOutputter out = new XMLOutputter();
				ByteArrayOutputStream os = new ByteArrayOutputStream();
				out.output(xml, os);
				os.flush();
				os.close();
				InputStream is = new ByteArrayInputStream(os.toByteArray());
				return is;
			} catch (IOException e) {
				log.error(e);
				throw e;
			} catch (RuntimeException e) {
				e.printStackTrace();
				throw e;
			}
		}

		JDOMStreamSource(Element xml) {
			xml.detach();
			this.xml = new Document(xml);
		}

		JDOMStreamSource(Document xml) {
			this.xml = xml;
		}
	}

	/**
	 * Sends a plain text email to the repository administrator.
	 *
	 * @param subject
	 * @param text
	 */
	public void sendAdministratorMessage(String subject, String text) {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(this.repositoryFromAddress);
		message.setTo(this.administratorAddress);
		message.setSubject("[" + this.irBaseUrl + "]" + subject);
		message.setText(text);
		this.mailSender.send(message);
	}
}
