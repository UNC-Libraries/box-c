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
package edu.unc.lib.persist.services;

import java.io.File;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

/**
 *
 * @author harring
 *
 */
public class EmailHandler {
    private static final Logger log = LoggerFactory.getLogger(EmailHandler.class);

    private String fromAddress;
    private JavaMailSender mailSender;

    /**
     *
     * @param toAddress
     * @param subject
     * @param body
     * @param attachment
     */
    public void sendEmail(String toAddress, String subject, String body, String filename, File attachment) {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, MimeMessageHelper.MULTIPART_MODE_MIXED);

            helper.setSubject(subject);
            helper.setFrom(fromAddress);
            helper.setText(body);
            helper.setTo(toAddress);
            helper.addAttachment("xml_export.zip", attachment);
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
