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
package edu.unc.lib.deposit.work;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import com.samskivert.mustache.Template;

import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.DepositStatusFactory;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositState;

/**
 * 
 * @author mdaines
 *
 */
public class DepositEmailHandler {

    protected SimpleDateFormat embargoDateFormat = new SimpleDateFormat("yyyy-MM-dd");

    private static final Logger LOG = LoggerFactory.getLogger(DepositEmailHandler.class);

    private DepositStatusFactory depositStatusFactory;

    private String baseUrl;
    private JavaMailSender mailSender = null;
    private String administratorEmail = null;

    private Template completedHtmlTemplate = null;
    private Template completedTextTemplate = null;

    private Template failedHtmlTemplate = null;
    private Template failedTextTemplate = null;

    @Autowired
    private Dataset dataset;

    //@Autowired
    //private FedoraAccessControlService accessControlService;

    protected DepositStatusFactory getDepositStatusFactory() {
        return depositStatusFactory;
    }

    public void setDepositStatusFactory(
            DepositStatusFactory depositStatusFactory) {
        this.depositStatusFactory = depositStatusFactory;
    }

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

    public String getAdministratorEmail() {
        return administratorEmail;
    }

    public void setAdministratorEmail(String administratorEmail) {
        this.administratorEmail = administratorEmail;
    }

    public Template getCompletedHtmlTemplate() {
        return completedHtmlTemplate;
    }

    public void setCompletedHtmlTemplate(Template completedHtmlTemplate) {
        this.completedHtmlTemplate = completedHtmlTemplate;
    }

    public Template getCompletedTextTemplate() {
        return completedTextTemplate;
    }

    public void setCompletedTextTemplate(Template completedTextTemplate) {
        this.completedTextTemplate = completedTextTemplate;
    }

    public Template getFailedHtmlTemplate() {
        return failedHtmlTemplate;
    }

    public void setFailedHtmlTemplate(Template failedHtmlTemplate) {
        this.failedHtmlTemplate = failedHtmlTemplate;
    }

    public Template getFailedTextTemplate() {
        return failedTextTemplate;
    }

    public void setFailedTextTemplate(Template failedTextTemplate) {
        this.failedTextTemplate = failedTextTemplate;
    }

    public DepositEmailHandler() {
    }

    public void sendDepositResults(String depositUUID) {
        DepositState state = this.getDepositStatusFactory().getState(depositUUID);

        switch (state) {
        case failed:
            sendFailed(depositUUID);
            break;
        case finished:
            sendCompleted(depositUUID);
            break;
        default:
            LOG.error("Don't know how to send deposit results email for state {}, for deposit {}", state, depositUUID);
        }
    }

    private void sendFailed(String depositUUID) {
        LOG.info("Sending deposit failed email for {}", depositUUID);

        Map<String, String> status = this.getDepositStatusFactory().get(depositUUID);

        Map<String, Object> data = new HashMap<String, Object>();
        data.putAll(status);
        data.put("baseUrl", this.getBaseUrl());

        String html = failedHtmlTemplate.execute(data);
        String text = failedTextTemplate.execute(data);

        MimeMessage mimeMessage = mailSender.createMimeMessage();

        try {
            MimeMessageHelper message = new MimeMessageHelper(mimeMessage, MimeMessageHelper.MULTIPART_MODE_MIXED);

            String depositorEmail = status.get(DepositField.depositorEmail.name());

            if (depositorEmail != null) {
                message.addTo(depositorEmail);
            }

            if (getAdministratorEmail() != null) {
                message.addTo(getAdministratorEmail());
            }

            message.setSubject("CDR deposit failed");
            message.setFrom(getAdministratorEmail());
            message.setText(text, html);

            this.mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            LOG.error("Cannot send notification email", e);
        }
    }

    private void sendCompleted(String depositUUID) {
        Map<String, String> status = this.getDepositStatusFactory().get(depositUUID);

        String depositorEmail = status.get(DepositField.depositorEmail.name());

        if (depositorEmail == null) {
            return;
        }

        LOG.info("Sending deposit completed email for {}", depositUUID);

        // If there is a "main object" in this deposit (it has exactly one top-level object), try linking to that.
        // If there isn't, use the container. With this scheme, there is the possibility that we could have multiple
        // top-level objects that are not public, but a container that is public, in which case the email wouldn't
        // make sense. However, form deposits currently always have a "main object".

        String objectPid = getMainObjectPidForDeposit(depositUUID);

        if (objectPid == null) {
            objectPid = status.get(DepositField.containerId.name());
        }

        //ObjectAccessControlsBean accessControls = accessControlService.getObjectAccessControls(PIDs.get(objectPid));
        Date embargoUntil = null; //accessControls.getLastActiveEmbargoUntilDate();
      //accessControls.getRoles(new AccessGroupSet(AccessGroupConstants.PUBLIC_GROUP)).contains(UserRole.patron);
        boolean hasPatronRoleForPublicGroup = true;

        Map<String, Object> data = new HashMap<String, Object>();
        data.putAll(status);
        data.put("baseUrl", this.getBaseUrl());
        data.put("objectPid", objectPid);

        if (embargoUntil != null) {
            data.put("embargoUntil", embargoDateFormat.format(embargoUntil));
            data.put("isEmbargoed", new Boolean(true));
        } else if (hasPatronRoleForPublicGroup) {
            data.put("isOpen", new Boolean(true));
        } else {
            data.put("isClosed", new Boolean(true));
        }

        String html = completedHtmlTemplate.execute(data);
        String text = completedTextTemplate.execute(data);

        MimeMessage mimeMessage = mailSender.createMimeMessage();

        try {
            MimeMessageHelper message = new MimeMessageHelper(mimeMessage, MimeMessageHelper.MULTIPART_MODE_MIXED);

            message.addTo(depositorEmail);
            message.setSubject("CDR deposit complete");
            message.setFrom(getAdministratorEmail());
            message.setText(text, html);

            this.mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            LOG.error("Cannot send notification email", e);
        }

    }

    private String getMainObjectPidForDeposit(String depositUUID) {
        try {
            PID depositPID = PIDs.get(depositUUID);

            String uri = depositPID.getURI();
            this.dataset.begin(ReadWrite.READ);
            Model model = this.dataset.getNamedModel(uri).begin();

            String depositPid = depositPID.getURI();
            Bag depositBag = model.getBag(depositPid);

            List<String> topLevelPids = new ArrayList<String>();
            DepositGraphUtils.walkChildrenDepthFirst(depositBag, topLevelPids, false);

            // There is a "main object" if the deposit has exactly one top-level object.
            if (topLevelPids.size() == 1) {
                return PIDs.get(topLevelPids.get(0)).toString();
            } else {
                return null;
            }
        } finally {
            if (this.dataset.isInTransaction()) {
                this.dataset.end();
            }
        }
    }

}
