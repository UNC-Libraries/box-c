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
package edu.unc.lib.dl.util;

import java.io.IOException;
import java.io.InputStream;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessagePreparator;

/**
 * @author Gregory Jansen
 *
 */
public class MockJavaMailSender implements JavaMailSender {
    private final Log log = LogFactory.getLog(MockJavaMailSender.class);
    private JavaMailSenderImpl wrapped = null;

    public MockJavaMailSender(JavaMailSenderImpl wrapped) {
        this.wrapped = wrapped;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.springframework.mail.javamail.JavaMailSender#createMimeMessage()
     */
    @Override
    public MimeMessage createMimeMessage() {
        return this.wrapped.createMimeMessage();
    }

    private void logit(MimeMessage msg) {
        log.debug("Dumping message content to standard out.");
        try {
            msg.writeTo(System.out);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.springframework.mail.javamail.JavaMailSender#createMimeMessage(java.io.InputStream)
     */
    @Override
    public MimeMessage createMimeMessage(InputStream contentStream) throws MailException {
        return this.wrapped.createMimeMessage(contentStream);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.springframework.mail.javamail.JavaMailSender#send(javax.mail.internet.MimeMessage)
     */
    @Override
    public void send(MimeMessage mimeMessage) throws MailException {
        logit(mimeMessage);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.springframework.mail.javamail.JavaMailSender#send(javax.mail.internet.MimeMessage[])
     */
    @Override
    public void send(MimeMessage[] mimeMessages) throws MailException {
        for (int i = 0; i < mimeMessages.length; i++) {
            logit(mimeMessages[i]);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.springframework.mail.javamail.JavaMailSender#send(org.springframework.mail.javamail.MimeMessagePreparator)
     */
    @Override
    public void send(MimeMessagePreparator mimeMessagePreparator) throws MailException {
        // eh, not used right now..
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.springframework.mail.javamail.JavaMailSender#send(org.springframework.mail.javamail.MimeMessagePreparator[])
     */
    @Override
    public void send(MimeMessagePreparator[] mimeMessagePreparators) throws MailException {
        // eh, not used right now..
    }

    /*
     * (non-Javadoc)
     *
     * @see org.springframework.mail.MailSender#send(org.springframework.mail.SimpleMailMessage)
     */
    @Override
    public void send(SimpleMailMessage simpleMessage) throws MailException {
        // eh, not used right now..
    }

    /*
     * (non-Javadoc)
     *
     * @see org.springframework.mail.MailSender#send(org.springframework.mail.SimpleMailMessage[])
     */
    @Override
    public void send(SimpleMailMessage[] simpleMessages) throws MailException {
        // eh, not used right now..
    }

}
