package edu.unc.lib.dl.cdr.services.processing;


import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.metrics.TimerFactory;
import edu.unc.lib.dl.services.AbstractMessageSender;
import io.dropwizard.metrics5.Timer;
import org.jdom2.Document;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.ATOM_NS;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.CDR_MESSAGE_NS;

public class RunEnhancementsService extends AbstractMessageSender {
    private static final Logger LOG = LoggerFactory.getLogger(RunEnhancementsService.class);
    private static final Timer timer = TimerFactory.createTimerForClass(RunEnhancementsService.class);

    private AccessControlService aclService;

    public void run(AgentPrincipals agent, List<String> objectPids, Boolean force) {
        try (Timer.Context context = timer.time()) {
            for (String objectPid : objectPids) {
                PID pid = PIDs.get(objectPid);
                aclService.assertHasAccess("User does not have permission to run enhancements",
                        pid, agent.getPrincipals(), Permission.runEnhancements);

                LOG.debug("sending solr update message for {} of type {}", pid, "runEnhancements");

                Document msg = makeEnhancementOperationBody(agent.getUsername(), pid, force);
                sendMessage(msg);
            }
        }
    }

    public void setAclService(AccessControlService aclService) {
        this.aclService = aclService;
    }

    private Document makeEnhancementOperationBody(String userid, PID targetPid, Boolean force) {
        Document msg = new Document();
        Element entry = new Element("entry", ATOM_NS);
        msg.addContent(entry);
        entry.addContent(new Element("author", ATOM_NS)
                .addContent(new Element("name", ATOM_NS).setText(userid)));
        entry.addContent(new Element("pid", ATOM_NS).setText(targetPid.getRepositoryPath()));
        entry.addContent(new Element("actionType", ATOM_NS)
                .setText("runEnhancements"));

        if (force) {
            Element paramForce = new Element("force", CDR_MESSAGE_NS);
            paramForce.setText("force");
        }

        return msg;
    }
}
