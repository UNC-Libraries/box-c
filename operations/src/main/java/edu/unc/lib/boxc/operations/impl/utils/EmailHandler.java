package edu.unc.lib.boxc.operations.impl.utils;

import java.io.File;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

/**
 * Service for sending simple emails
 *
 * @author harring
 *
 */
public class EmailHandler {
    private static final Logger log = LoggerFactory.getLogger(EmailHandler.class);

    private String fromAddress;
    private JavaMailSender mailSender;

    /**
     * Send an email to the supplied address
     *
     * @param toAddress recipient email address
     * @param subject Subject line for the email
     * @param body body content
     * @param attachment optional file attachment
     */
    public void sendEmail(String toAddress, String subject, String body, String filename, File attachment) {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, MimeMessageHelper.MULTIPART_MODE_MIXED);

            helper.setSubject(subject);
            helper.setFrom(fromAddress);
            helper.setText(body);
            helper.setTo(toAddress);
            if (attachment != null) {
                helper.addAttachment(filename, attachment);
            }
            mailSender.send(mimeMessage);
            log.debug("Sending XML export email to {}", toAddress);
        } catch (MessagingException e) {
            log.error("Cannot send notification email", e);
        }
    }

    public void setMailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

}
