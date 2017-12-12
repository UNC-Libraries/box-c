package edu.unc.lib.dl.services;

import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.ATOM_NS;
import static edu.unc.lib.dl.xml.NamespaceConstants.CDR_MESSAGE_AUTHOR_URI;

import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;
import org.joda.time.DateTimeUtils;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.JMSMessageUtil.CDRActions;

/**
 * A class to hold common helper methods for message senders
 *
 * @author harring
 *
 */
public abstract class AbstractMessageSender {
    protected JmsTemplate jmsTemplate;

    protected Element createAtomEntry(String userid, PID contextpid, CDRActions operation) {
        return createAtomEntry(userid, contextpid, operation.toString(), "urn:uuid:" + UUID.randomUUID().toString());
    }

    protected Element createAtomEntry(String userid, PID contextpid, String operation, String messageId) {
        Document msg = new Document();
        Element entry = new Element("entry", ATOM_NS);
        msg.addContent(entry);
        entry.addContent(new Element("id", ATOM_NS).setText(messageId));
        DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
        String timestamp = fmt.print(DateTimeUtils.currentTimeMillis());
        entry.addContent(new Element("updated", ATOM_NS).setText(timestamp));
        entry.addContent(new Element("author", ATOM_NS).addContent(new Element("name", ATOM_NS).setText(userid))
                .addContent(new Element("uri", ATOM_NS).setText(CDR_MESSAGE_AUTHOR_URI)));
        entry.addContent(new Element("title", ATOM_NS)
                .setText(operation).setAttribute("type", "text"));
        entry.addContent(new Element("summary", ATOM_NS).setText(contextpid.getRepositoryPath())
                .setAttribute("type", "text"));
        Element content = new Element("content", ATOM_NS).setAttribute("type", "text/xml");
        entry.addContent(content);
        return content;
    }

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
