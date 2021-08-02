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
package edu.unc.lib.boxc.operations.jms;

import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.ATOM_NS;

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
public class MessageSender {
    protected JmsTemplate jmsTemplate;

    public void sendMessage(Document msg) {
        XMLOutputter out = new XMLOutputter();
        final String msgStr = out.outputString(msg);
        sendMessage(msgStr);
    }

    public void sendMessage(String msgStr) {
        jmsTemplate.send(new MessageCreator() {
            @Override
            public Message createMessage(Session session) throws JMSException {
                // Committing the session to flush changes in long running threads
                if (session.getTransacted()) {
                    session.commit();
                }
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
