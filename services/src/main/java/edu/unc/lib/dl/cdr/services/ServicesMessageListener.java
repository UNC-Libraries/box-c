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
package edu.unc.lib.dl.cdr.services;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.jdom.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import edu.unc.lib.dl.fedora.ClientUtils;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

public class ServicesMessageListener implements MessageListener {
	@SuppressWarnings("unused")
	private static final Logger LOG = LoggerFactory.getLogger(ServicesMessageListener.class);
	
	private ServicesConductor servicesConductor = null;
	
	public ServicesMessageListener(){
		
	}
	
	@Override
	public void onMessage(Message message) {
		LOG.debug("I HAVE A MESSAGE!!!!!");
		if (message instanceof TextMessage) {
			try {
				String msgText = ((TextMessage) message).getText();
				LOG.debug(msgText);
				Document msgXML = ClientUtils.parseXML(msgText.getBytes());
				servicesConductor.add(msgXML);
			} catch (JMSException e) {
				LOG.error("onMessage failed", e);
			} catch (SAXException e) {
				LOG.error("onMessage failed", e);
			}
		} else {
			throw new IllegalArgumentException("Message must be of type TextMessage");
		}
	}

	public ServicesConductor getServicesConductor() {
		return servicesConductor;
	}

	public void setServicesConductor(ServicesConductor servicesConductor) {
		this.servicesConductor = servicesConductor;
	}
}
