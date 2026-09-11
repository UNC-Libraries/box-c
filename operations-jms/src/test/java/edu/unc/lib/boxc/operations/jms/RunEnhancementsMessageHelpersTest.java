package edu.unc.lib.boxc.operations.jms;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import org.jdom2.Element;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.ATOM_NS;
import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.CDR_MESSAGE_NS;
import static edu.unc.lib.boxc.operations.jms.JMSMessageUtil.CDRActions.RUN_ENHANCEMENTS;
import static edu.unc.lib.boxc.operations.jms.RunEnhancementsMessageHelpers.AUDIO_ACCESS_COPY;
import static edu.unc.lib.boxc.operations.jms.RunEnhancementsMessageHelpers.ENHANCEMENT_LIST;
import static edu.unc.lib.boxc.operations.jms.RunEnhancementsMessageHelpers.VIDEO_ACCESS_COPY;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class RunEnhancementsMessageHelpersTest {
    private final PID pid = PIDs.get(UUID.randomUUID().toString());

    @Test
    public void makeEnhancementOperationBodyTest() {
        var userId = "user1";
        var doc = RunEnhancementsMessageHelpers.makeEnhancementOperationBody(userId,
                pid, true, List.of(AUDIO_ACCESS_COPY, VIDEO_ACCESS_COPY));

        Element body = doc.getRootElement();
        var authorElement = body.getChild("author", ATOM_NS);
        var name = authorElement.getChild("name", ATOM_NS).getTextTrim();
        assertEquals(userId, name);

        var enhancementsElement = body.getChild(RUN_ENHANCEMENTS.getName(), CDR_MESSAGE_NS);
        var forceValue = enhancementsElement.getChildTextTrim("force", CDR_MESSAGE_NS);
        var enhancementsToRun = enhancementsElement.getChildTextTrim(ENHANCEMENT_LIST, CDR_MESSAGE_NS);
        var pidValue = enhancementsElement.getChildTextTrim("pid", CDR_MESSAGE_NS);

        assertEquals("true", forceValue);
        assertEquals("audioAccessCopy,videoAccessCopy", enhancementsToRun);
        assertEquals(pid.getRepositoryPath(), pidValue);
    }
}
