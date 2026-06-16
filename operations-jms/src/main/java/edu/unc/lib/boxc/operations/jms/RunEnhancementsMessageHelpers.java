package edu.unc.lib.boxc.operations.jms;

import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.ATOM_NS;
import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.CDR_MESSAGE_NS;

import org.jdom2.Document;
import org.jdom2.Element;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.operations.jms.JMSMessageUtil.CDRActions;

import java.util.List;

/**
 * Helper methods for run enhancement messages
 *
 * @author bbpennel
 */
public class RunEnhancementsMessageHelpers {
    public static final String ENHANCEMENT_LIST = "enhancementList";
    public static final String IMAGE_ACCESS_COPY = "imageAccessCopy";
    public static final String EXTRACT_FULLTEXT = "extractFulltext";
    public static final String AUDIO_ACCESS_COPY = "audioAccessCopy";
    public static final String VIDEO_ACCESS_COPY = "videoAccessCopy";
    public static final String MACHINE_GEN_DESCRIPTION = "machineGenDescription";
    public static final List<String> DEFAULT_ENHANCEMENTS = List.of(IMAGE_ACCESS_COPY,EXTRACT_FULLTEXT,
            AUDIO_ACCESS_COPY, VIDEO_ACCESS_COPY, MACHINE_GEN_DESCRIPTION);
    public static final String DEFAULT_ENHANCEMENTS_STRING = String.join(",", DEFAULT_ENHANCEMENTS);
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

        Element paramEnhancementList = new Element("enhancementList", CDR_MESSAGE_NS);
        paramEnhancementList.setText(DEFAULT_ENHANCEMENTS_STRING);

        Element enhancements = new Element(CDRActions.RUN_ENHANCEMENTS.getName(), CDR_MESSAGE_NS);
        enhancements.addContent(new Element("pid", CDR_MESSAGE_NS).setText(pid.getRepositoryPath()));
        enhancements.addContent(paramForce);
        entry.addContent(enhancements);

        msg.addContent(entry);

        return msg;
    }

    /**
     * Generate the body for a run enhancement request message
     *
     * @param userid
     * @param pid
     * @param force
     * @param enhancementList list of enhancements to run
     * @return
     */
    public static Document makeEnhancementOperationBody(String userid, PID pid, Boolean force, List<String> enhancementList) {
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

        Element paramEnhancementList = new Element("enhancementList", CDR_MESSAGE_NS);
        paramEnhancementList.setText(String.join(",",enhancementList));

        Element enhancements = new Element(CDRActions.RUN_ENHANCEMENTS.getName(), CDR_MESSAGE_NS);
        enhancements.addContent(new Element("pid", CDR_MESSAGE_NS).setText(pid.getRepositoryPath()));
        enhancements.addContent(paramForce);
        enhancements.addContent(paramEnhancementList);
        entry.addContent(enhancements);

        msg.addContent(entry);

        return msg;
    }
}
