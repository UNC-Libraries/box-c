package edu.unc.lib.boxc.operations.jms;

import org.jdom2.Document;
import org.jdom2.output.XMLOutputter;
import org.springframework.jms.core.JmsTemplate;

import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.ATOM_NS;

/**
 * A class to hold common helper methods for message senders
 *
 * @author harring
 *
 */
public class MessageSender {
    protected JmsTemplate jmsTemplate;

    public void sendMessage(Document msg) {
        XMLOutputter out = new XMLOutputter();
        final String msgStr = out.outputString(msg);
        sendMessage(msgStr);
    }

    public void sendMessage(String msgStr) {
        jmsTemplate.send(session -> {
            // Committing the session to flush changes in long running threads
            if (session.getTransacted()) {
                session.commit();
            }
            return session.createTextMessage(msgStr);
        });
    }

    protected String getMessageId(Document msg) {
        return msg.getRootElement().getChildText("id", ATOM_NS);
    }

    public JmsTemplate getJmsTemplate() {
        return jmsTemplate;
    }

    public void setJmsTemplate(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

}
