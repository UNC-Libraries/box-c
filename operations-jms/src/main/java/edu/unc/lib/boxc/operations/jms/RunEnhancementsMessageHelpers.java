package edu.unc.lib.boxc.operations.jms;

import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.ATOM_NS;
import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.CDR_MESSAGE_NS;

import org.jdom2.Document;
import org.jdom2.Element;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.operations.jms.JMSMessageUtil.CDRActions;

/**
 * Helper methods for run enhancement messages
 *
 * @author bbpennel
 */
public class RunEnhancementsMessageHelpers {

    private RunEnhancementsMessageHelpers() {
    }

    /**
     * Generate the body for a run enhancement request message
     *
     * @param userid
     * @param pid
     * @param force
     * @return
     */
    public static Document makeEnhancementOperationBody(String userid, PID pid, Boolean force) {
        Document msg = new Document();
        Element entry = new Element("entry", ATOM_NS);
        entry.addContent(new Element("author", ATOM_NS)
                .addContent(new Element("name", ATOM_NS).setText(userid)));

        Element paramForce = new Element("force", CDR_MESSAGE_NS);

        if (force) {
            paramForce.setText("true");
        } else {
            paramForce.setText("false");
        }

        Element enhancements = new Element(CDRActions.RUN_ENHANCEMENTS.getName(), CDR_MESSAGE_NS);
        enhancements.addContent(new Element("pid", CDR_MESSAGE_NS).setText(pid.getRepositoryPath()));
        enhancements.addContent(paramForce);
        entry.addContent(enhancements);

        msg.addContent(entry);

        return msg;
    }
}
