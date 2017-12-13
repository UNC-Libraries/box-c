package edu.unc.lib.dl.services;

import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.ATOM_NS;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.jdom2.Document;
import org.jdom2.output.XMLOutputter;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

/**
 * A class to hold common helper methods for message senders
 *
 * @author harring
 *
 */
public abstract class AbstractMessageSender {
    protected JmsTemplate jmsTemplate;

    protected void sendMessage(Document msg) {
        XMLOutputter out = new XMLOutputter();
        final String msgStr = out.outputString(msg);

        jmsTemplate.send(new MessageCreator() {

            @Override
            public Message createMessage(Session session) throws JMSException {
                return session.createTextMessage(msgStr);
            }
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
