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
package edu.unc.lib.dl.services;

import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.ATOM_NS;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.CDR_MESSAGE_NS;
import static edu.unc.lib.dl.xml.NamespaceConstants.CDR_MESSAGE_AUTHOR_URI;

import java.util.Collection;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

import edu.unc.lib.dl.fedora.PID;

/**
 * @author Gregory Jansen
 *
 */
public class OperationsMessageSender {

	@SuppressWarnings("unused")
	private static final Logger LOG = LoggerFactory.getLogger(OperationsMessageSender.class);

	private JmsTemplate jmsTemplate = null;

	/**
	 * Sends a message about a repository add operation
	 * @param userid the submitter
	 * @param timestamp time of the add in ISO8601
	 * @param destinations containers added to
	 * @param added objects added
	 * @param reordered objects reordered by this add
	 */
	public void sendAddOperation(String userid, Collection<PID> destinations, Collection<PID> added,
			Collection<PID> reordered) {
		Document msg = new Document();
		Element contentEl = createAtomEntry(msg, userid, destinations.iterator().next(), "add");
		Element add = new Element("add", CDR_MESSAGE_NS);
		contentEl.addContent(add);

		for (PID destination : destinations) {
			add.addContent(new Element("parent", CDR_MESSAGE_NS).setText(destination.getPid()));
		}

		Element subjects = new Element("subjects", CDR_MESSAGE_NS);
		add.addContent(subjects);
		for (PID sub : added) {
			subjects.addContent(new Element("pid", CDR_MESSAGE_NS).setText(sub.getPid()));
		}

		Element reorderedEl = new Element("reordered", CDR_MESSAGE_NS);
		add.addContent(reorderedEl);
		for (PID re : reordered) {
			reorderedEl.addContent(new Element("pid", CDR_MESSAGE_NS).setText(re.getPid()));
		}

		// send
		XMLOutputter out = new XMLOutputter();
		final String msgStr = out.outputString(msg);

		this.jmsTemplate.send(new MessageCreator() {

			@Override
			public Message createMessage(Session session) throws JMSException {
				return session.createTextMessage(msgStr);
			}

		});
		LOG.debug("sent add operation JMS message using JMS template:" + this.getJmsTemplate().toString());
	}

	public void sendRemoveOperation(String userid, PID destination, Collection<PID> removed,
			Collection<PID> reordered) {
		Document msg = new Document();
		Element contentEl = createAtomEntry(msg, userid, destination, "remove");
		Element remove = new Element("remove", CDR_MESSAGE_NS);
		contentEl.addContent(remove);

		remove.addContent(new Element("parent", CDR_MESSAGE_NS).setText(destination.getPid()));

		Element subjects = new Element("subjects", CDR_MESSAGE_NS);
		remove.addContent(subjects);
		for (PID sub : removed) {
			subjects.addContent(new Element("pid", CDR_MESSAGE_NS).setText(sub.getPid()));
		}

		Element reorderedEl = new Element("reordered", CDR_MESSAGE_NS);
		remove.addContent(reorderedEl);
		for (PID re : reordered) {
			reorderedEl.addContent(new Element("pid", CDR_MESSAGE_NS).setText(re.getPid()));
		}

		// send
		XMLOutputter out = new XMLOutputter();
		final String msgStr = out.outputString(msg);

		this.jmsTemplate.send(new MessageCreator() {

			@Override
			public Message createMessage(Session session) throws JMSException {
				return session.createTextMessage(msgStr);
			}

		});
	}

	public void sendMoveOperation(String userid, Collection<PID> sources, PID destination,
			Collection<PID> moved, Collection<PID> reordered) {
		Document msg = new Document();
		Element contentEl = createAtomEntry(msg, userid, destination, "move");
		Element move = new Element("move", CDR_MESSAGE_NS);
		contentEl.addContent(move);

		move.addContent(new Element("parent", CDR_MESSAGE_NS).setText(destination.getPid()));

		for (PID old : sources) {
			move.addContent(new Element("oldParent", CDR_MESSAGE_NS).setText(old.getPid()));
		}

		Element subjects = new Element("subjects", CDR_MESSAGE_NS);
		move.addContent(subjects);
		for (PID sub : moved) {
			subjects.addContent(new Element("pid", CDR_MESSAGE_NS).setText(sub.getPid()));
		}

		Element reorderedEl = new Element("reordered", CDR_MESSAGE_NS);
		move.addContent(reorderedEl);
		for (PID re : reordered) {
			reorderedEl.addContent(new Element("pid", CDR_MESSAGE_NS).setText(re.getPid()));
		}

		// send
		XMLOutputter out = new XMLOutputter();
		final String msgStr = out.outputString(msg);

		this.jmsTemplate.send(new MessageCreator() {

			@Override
			public Message createMessage(Session session) throws JMSException {
				return session.createTextMessage(msgStr);
			}

		});
	}

	public void sendReorderOperation(String userid, PID destination, Collection<PID> reordered) {
		Document msg = new Document();
		Element contentEl = createAtomEntry(msg, userid, destination, "reorder");
		Element reorder = new Element("reorder", CDR_MESSAGE_NS);
		contentEl.addContent(reorder);

		reorder.addContent(new Element("parent", CDR_MESSAGE_NS).setText(destination.getPid()));

		Element reorderedEl = new Element("reordered", CDR_MESSAGE_NS);
		reorder.addContent(reorderedEl);
		for (PID re : reordered) {
			reorderedEl.addContent(new Element("pid", CDR_MESSAGE_NS).setText(re.getPid()));
		}

		// send
		XMLOutputter out = new XMLOutputter();
		final String msgStr = out.outputString(msg);

		this.jmsTemplate.send(new MessageCreator() {

			@Override
			public Message createMessage(Session session) throws JMSException {
				return session.createTextMessage(msgStr);
			}

		});
	}

	/**
	 * @param msg
	 * @param userid
	 * @param pid
	 * @return
	 */
	private Element createAtomEntry(Document msg, String userid, PID contextpid, String operation) {
		Element entry = new Element("entry", ATOM_NS);
		msg.addContent(entry);
		entry.addContent(new Element("id", ATOM_NS).setText("urn:uuid:" + UUID.randomUUID().toString()));
		DateTime now = new DateTime(DateTimeUtils.currentTimeMillis(), DateTimeZone.UTC);
		entry.addContent(new Element("updated", ATOM_NS).setText(now.toString()));
		entry.addContent(new Element("author", ATOM_NS).addContent(new Element("name", ATOM_NS).setText(userid))
				.addContent(new Element("uri", ATOM_NS).setText(CDR_MESSAGE_AUTHOR_URI)));
		entry.addContent(new Element("title", ATOM_NS).setText(operation).setAttribute("type", "text"));
		entry.addContent(new Element("summary", ATOM_NS).setText(contextpid.getPid()).setAttribute("type", "text"));
		Element content = new Element("content", ATOM_NS).setAttribute("type", "text/xml");
		entry.addContent(content);
		return content;
	}

	public JmsTemplate getJmsTemplate() {
		return jmsTemplate;
	}

	public void setJmsTemplate(JmsTemplate jmsTemplate) {
		this.jmsTemplate = jmsTemplate;
	}

}
