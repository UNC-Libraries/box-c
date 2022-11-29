package edu.unc.lib.boxc.deposit.work;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import com.samskivert.mustache.Template;

import edu.unc.lib.boxc.auth.api.AccessPrincipalConstants;
import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositField;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositState;
import edu.unc.lib.boxc.deposit.impl.model.DepositModelManager;
import edu.unc.lib.boxc.deposit.impl.model.DepositStatusFactory;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;

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
    protected DepositModelManager depositModelManager;

    @Autowired
    private AccessControlService aclService;
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

        Map<String, Object> data = new HashMap<>();
        data.putAll(status);
        data.put("baseUrl", formatBaseUrl());

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

            message.setSubject("DCR deposit failed");
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

        AccessGroupSet publicPrincipals = new AccessGroupSetImpl(AccessPrincipalConstants.PUBLIC_PRINC);

        boolean hasPatronRoleForPublicGroup = aclService.hasAccess(
                PIDs.get(objectPid), publicPrincipals, Permission.viewOriginal);

        Map<String, Object> data = new HashMap<>();
        data.putAll(status);
        data.put("baseUrl", formatBaseUrl());
        data.put("objectPid", objectPid);

        if (hasPatronRoleForPublicGroup) {
            data.put("isOpen", true);
        } else {
            data.put("isClosed", true);
        }

        String html = completedHtmlTemplate.execute(data);
        String text = completedTextTemplate.execute(data);

        MimeMessage mimeMessage = mailSender.createMimeMessage();

        try {
            MimeMessageHelper message = new MimeMessageHelper(mimeMessage, MimeMessageHelper.MULTIPART_MODE_MIXED);

            message.addTo(depositorEmail);
            message.setSubject("DCR deposit complete");
            message.setFrom(getAdministratorEmail());
            message.setText(text, html);

            this.mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            LOG.error("Cannot send notification email", e);
        }

    }

    private String formatBaseUrl() {
        String url = this.getBaseUrl();
        if (url.endsWith("/")) {
           return StringUtils.chop(url);
        }
        return url;
    }

    private String getMainObjectPidForDeposit(String depositUUID) {
        try {
            PID depositPID = PIDs.get(depositUUID);

            Model model = depositModelManager.getReadModel(depositPID);

            String depositPid = depositPID.getURI();
            Bag depositBag = model.getBag(depositPid);

            List<String> topLevelPids = new ArrayList<>();
            DepositGraphUtils.walkChildrenDepthFirst(depositBag, topLevelPids, false);

            // There is a "main object" if the deposit has exactly one top-level object.
            if (topLevelPids.size() == 1) {
                return PIDs.get(topLevelPids.get(0)).toString();
            } else {
                return null;
            }
        } finally {
            depositModelManager.end();
        }
    }

}
