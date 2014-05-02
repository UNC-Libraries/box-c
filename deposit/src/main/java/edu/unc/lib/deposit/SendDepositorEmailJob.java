package edu.unc.lib.deposit;

import java.util.HashMap;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import com.samskivert.mustache.Template;

import edu.unc.lib.deposit.work.AbstractDepositJob;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;

public class SendDepositorEmailJob extends AbstractDepositJob implements Runnable {
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

	private Template htmlTemplate = null;
	private Template textTemplate = null;

	public SendDepositorEmailJob() {
	}

	public SendDepositorEmailJob(String uuid, String depositUUID) {
		super(uuid, depositUUID);
	}

	@Override
	public void run() {
		// prepare template data
		Map<String, Object> data = new HashMap<String, Object>();
		Map<String, String> status = this.getDepositStatus();
		if(!status.containsKey(DepositField.depositorEmail.name())) return;
		data.putAll(status);
		data.put("baseUrl", this.getBaseUrl());
		boolean error = status.containsKey(DepositField.errorMessage.name());
		data.put("error", Boolean.valueOf(error));
		
		// execute template, address and send
		String html = htmlTemplate.execute(data);
		String text = textTemplate.execute(data);
		MimeMessage mimeMessage = mailSender.createMimeMessage();
		try {
			MimeMessageHelper message = new MimeMessageHelper(mimeMessage, MimeMessageHelper.MULTIPART_MODE_MIXED);
			String addy = status.get(DepositField.depositorEmail.name());
			message.addTo(addy);
			if(error) {
				message.setSubject("CDR deposit error");
			} else {
				message.setSubject("CDR deposit complete");
			}
			message.setFrom(getFromAddress());
			message.setText(text, html);
			this.mailSender.send(mimeMessage);
		} catch(MessagingException e) {
			failJob(e, null, "Cannot send notification email");
		}
	}

}
