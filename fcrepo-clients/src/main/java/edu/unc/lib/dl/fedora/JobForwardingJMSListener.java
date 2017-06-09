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
package edu.unc.lib.dl.fedora;

import java.io.StringReader;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listener for Fedora JMS queue/topic which interprets the incoming text messages and forwards relevant message data on
 * to all registered listener jobs
 *
 * @author bbpennel
 * @date Mar 17, 2014
 */
public class JobForwardingJMSListener implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(JobForwardingJMSListener.class);

    private final List<ListenerJob> listenerJobs;

    public JobForwardingJMSListener() {
        // This construct is particularly suited for large ingests, where the list of listeners changes slowly.
        listenerJobs = new CopyOnWriteArrayList<ListenerJob>();
    }

    public void registerListener(ListenerJob listener) {
        listenerJobs.add(listener);
    }

    public void unregisterListener(ListenerJob listener) {
        listenerJobs.remove(listener);
    }

    @Override
    public void onMessage(Message message) {

        log.debug("Received message");

        // If no jobs are listening, ignore the message
        if (listenerJobs.size() == 0) {
            return;
        }

        if (message instanceof TextMessage) {
            try {
                TextMessage msg = (TextMessage) message;

                String messageBody = msg.getText();
                Document msgDoc = new SAXBuilder().build(new StringReader(messageBody));

                log.debug("Message contents:\n{}", messageBody);

                // This does not need to be synchronized because it is using CopyOnWriteArrayList
                for (ListenerJob listener : listenerJobs) {
                    listener.onEvent(msgDoc);
                }

            } catch (Exception e) {
                log.error("onMessage failed", e);
            }
        } else {
            throw new IllegalArgumentException("Message must be of type TextMessage");
        }
    }
}
