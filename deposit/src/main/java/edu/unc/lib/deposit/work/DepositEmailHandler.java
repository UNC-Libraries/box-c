package edu.unc.lib.deposit.work;

import java.util.HashMap;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import com.samskivert.mustache.Template;

import edu.unc.lib.dl.util.DepositStatusFactory;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositState;

public class DepositEmailHandler {
	private static final Logger LOG = LoggerFactory.getLogger(DepositEmailHandler.class);
	
	private String baseUrl;
	private JavaMailSender mailSender = null;
	private String fromAddress = null;
	
	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public JavaMailSender getMailSender() {
		return mailSender;
	}

	public void setMailSender(JavaMailSender mailSender) {
		this.mailSender = mailSender;
	}

	public String getFromAddress() {
		return fromAddress;
	}

	public void setFromAddress(String fromAddress) {
		this.fromAddress = fromAddress;
	}

	public Template getHtmlTemplate() {
		return htmlTemplate;
	}

	public void setHtmlTemplate(Template htmlTemplate) {
		this.htmlTemplate = htmlTemplate;
	}

	public Template getTextTemplate() {
		return textTemplate;
	}

	public void setTextTemplate(Template textTemplate) {
		this.textTemplate = textTemplate;
	}
	
	private DepositStatusFactory depositStatusFactory;

	protected DepositStatusFactory getDepositStatusFactory() {
		return depositStatusFactory;
	}

	public void setDepositStatusFactory(
			DepositStatusFactory depositStatusFactory) {
		this.depositStatusFactory = depositStatusFactory;
	}

	private Template htmlTemplate = null;
	private Template textTemplate = null;

	public DepositEmailHandler() {
	}

	public void sendDepositResults(String depositUUID) {
		Map<String, String> status = this.getDepositStatusFactory().get(depositUUID);
		
		if (!status.containsKey(DepositField.depositorEmail.name())) {
			LOG.info("No depositor email for {}", depositUUID);
			return;
		} else {
			LOG.info("Sending deposit results email for {} to {}", depositUUID, status.get(DepositField.depositorEmail.name()));
		}

		// prepare template data
		Map<String, Object> data = new HashMap<String, Object>();
		
		data.putAll(status);
		
		data.put("baseUrl", this.getBaseUrl());
		
		boolean failed = status.get(DepositField.state.name()).equals(DepositState.failed.name());
		data.put("failed", Boolean.valueOf(failed));
		
		// execute template, address and send
		String html = htmlTemplate.execute(data);
		String text = textTemplate.execute(data);
		
		MimeMessage mimeMessage = mailSender.createMimeMessage();
		try {
			MimeMessageHelper message = new MimeMessageHelper(mimeMessage, MimeMessageHelper.MULTIPART_MODE_MIXED);
			String addy = status.get(DepositField.depositorEmail.name());
			message.addTo(addy);
			if (failed) {
				message.setSubject("CDR deposit failed: " + status.get(DepositField.fileName.name()));
			} else {
				message.setSubject("CDR deposit complete: " + status.get(DepositField.fileName.name()));
			}
			message.setFrom(getFromAddress());
			message.setText(text, html);
			this.mailSender.send(mimeMessage);
		} catch (MessagingException e) {
			LOG.error("Cannot send notification email", e);
		}
	}

}
