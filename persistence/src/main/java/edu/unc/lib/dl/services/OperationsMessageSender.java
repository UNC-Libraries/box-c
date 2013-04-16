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
import org.joda.time.DateTimeUtils;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.IndexingActionType;
import edu.unc.lib.dl.util.JMSMessageUtil;

/**
 * @author Gregory Jansen
 * 
 */
public class OperationsMessageSender {
	private static final Logger LOG = LoggerFactory.getLogger(OperationsMessageSender.class);

	private JmsTemplate jmsTemplate = null;

	public void sendAddOperation(String userid, Collection<PID> destinations, Collection<PID> added,
			Collection<PID> reordered, String depositId) {
		Document msg = new Document();
		Element contentEl = createAtomEntry(msg, userid, destinations.iterator().next(), "add");
		Element add = new Element("add", CDR_MESSAGE_NS);
		contentEl.addContent(add);

		add.addContent(new Element("depositId", CDR_MESSAGE_NS).setText(depositId));

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

		sendMessage(msg);
		LOG.debug("sent add operation JMS message using JMS template:" + this.getJmsTemplate().toString());
	}

	public void sendRemoveOperation(String userid, PID destination, Collection<PID> removed, Collection<PID> reordered) {
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

		sendMessage(msg);
	}

	public void sendMoveOperation(String userid, Collection<PID> sources, PID destination, Collection<PID> moved,
			Collection<PID> reordered) {
		Document msg = new Document();
		Element contentEl = createAtomEntry(msg, userid, destination, "move");
		Element move = new Element("move", CDR_MESSAGE_NS);
		contentEl.addContent(move);

		move.addContent(new Element("parent", CDR_MESSAGE_NS).setText(destination.getPid()));

		Element oldParents = new Element("oldParents", CDR_MESSAGE_NS);
		move.addContent(oldParents);
		for (PID old : sources) {
			oldParents.addContent(new Element("pid", CDR_MESSAGE_NS).setText(old.getPid()));
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

		sendMessage(msg);
	}

	public void sendReorderOperation(String userid, String timestamp, PID destination, Collection<PID> reordered) {
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

		sendMessage(msg);
	}

	/**
	 * 
	 * 
	 * @param userid
	 * @param pids
	 * @param publish
	 *           Subjects are published if true, unpublished if false
	 */
	public String sendPublishOperation(String userid, Collection<PID> pids, boolean publish) {
		String messageId = "urn:uuid:" + UUID.randomUUID().toString();
		Document msg = new Document();
		Element contentEl = createAtomEntry(msg, userid, pids.iterator().next(), "publish", messageId);
		
		Element publishEl = new Element("publish", CDR_MESSAGE_NS);
		contentEl.addContent(publishEl);

		Element publishValueEl = new Element("value", CDR_MESSAGE_NS);
		publishEl.addContent(publishValueEl);
		if (publish) {
			publishValueEl.setText("yes");
		} else {
			publishValueEl.setText("no");
		}

		Element subjects = new Element("subjects", CDR_MESSAGE_NS);
		publishEl.addContent(subjects);
		for (PID sub : pids) {
			subjects.addContent(new Element("pid", CDR_MESSAGE_NS).setText(sub.getPid()));
		}

		sendMessage(msg);
		
		return messageId;
	}
	
	public String sendIndexingOperation(String userid, Collection<PID> pids, IndexingActionType type) {
		String messageId = "urn:uuid:" + UUID.randomUUID().toString();
		Document msg = new Document();
		Element contentEl = createAtomEntry(msg, userid, pids.iterator().next(), JMSMessageUtil.CDRActions.INDEX.getName(), messageId);
		
		Element indexEl = new Element(type.getName(), CDR_MESSAGE_NS);
		contentEl.addContent(indexEl);

		Element subjects = new Element("subjects", CDR_MESSAGE_NS);
		indexEl.addContent(subjects);
		for (PID sub : pids) {
			subjects.addContent(new Element("pid", CDR_MESSAGE_NS).setText(sub.getPid()));
		}
		
		sendMessage(msg);
		
		return messageId;
	}
	
	private void sendMessage(Document msg) {
		XMLOutputter out = new XMLOutputter();
		final String msgStr = out.outputString(msg);

		this.jmsTemplate.send(new MessageCreator() {

			@Override
			public Message createMessage(Session session) throws JMSException {
				return session.createTextMessage(msgStr);
			}

		});
	}

	private Element createAtomEntry(Document msg, String userid, PID contextpid, String operation) {
		return createAtomEntry(msg, userid, contextpid, operation, "urn:uuid:" + UUID.randomUUID().toString());
	}
	
	/**
	 * @param msg
	 * @param userid
	 * @param pid
	 * @return
	 */
	private Element createAtomEntry(Document msg, String userid, PID contextpid, String operation, String messageId) {
		Element entry = new Element("entry", ATOM_NS);
		msg.addContent(entry);
		entry.addContent(new Element("id", ATOM_NS).setText(messageId));
		DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
		String timestamp = fmt.print(DateTimeUtils.currentTimeMillis());
		entry.addContent(new Element("updated", ATOM_NS).setText(timestamp));
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
