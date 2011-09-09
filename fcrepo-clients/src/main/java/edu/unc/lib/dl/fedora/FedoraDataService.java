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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.TripleStoreQueryService.PathInfo;

/**
 * Fedora data retrieval class used for accessing data streams and performing Mulgara queries
 * to generate XML views of Fedora objects for outside usage.
 * 
 * @author Gregory Jansen
 * @author Ben Pennell
 */
public class FedoraDataService {

	private static final Logger LOG = LoggerFactory
			.getLogger(FedoraDataService.class);

	private edu.unc.lib.dl.fedora.AccessClient accessClient = null;

	private edu.unc.lib.dl.fedora.ManagementClient managementClient = null;

	private edu.unc.lib.dl.util.TripleStoreQueryService tripleStoreQueryService = null;

	private edu.unc.lib.dl.fedora.AccessControlUtils accessControlUtils = null;
	
	public edu.unc.lib.dl.fedora.AccessClient getAccessClient() {
		return accessClient;
	}

	public void setAccessClient(edu.unc.lib.dl.fedora.AccessClient accessClient) {
		this.accessClient = accessClient;
	}

	/**
	 * Retrieves a view-inputs document containing the FOXML datastream for the object
	 * identified by simplepid 
	 * @param simplepid
	 * @return 
	 * @throws FedoraException
	 */
	public Document getFoxmlViewXML(String simplepid) throws FedoraException {
		final PID pid = new PID(simplepid);
		Document result = new Document();
		final Element inputs = new Element("view-inputs");
		result.setRootElement(inputs);
		CountDownLatch cdl = new CountDownLatch(1);
		new StopLatchedThread(cdl, new GetFoxml(pid, inputs)).start();
		try {
			cdl.await(5000, TimeUnit.MILLISECONDS);
		} catch (InterruptedException expected) {
		}

		return result;
	}
	
	/**
	 * Retrieves a view-inputs document containing the Mods datastream for the object
	 * identified by simplepid 
	 * @param simplepid
	 * @return 
	 * @throws FedoraException
	 */
	public Document getModsViewXML(String simplepid) throws FedoraException {
		final PID pid = new PID(simplepid);
		Document result = new Document();
		final Element inputs = new Element("view-inputs");
		result.setRootElement(inputs);
		CountDownLatch cdl = new CountDownLatch(1);
		new StopLatchedThread(cdl, new GetMods(pid, inputs)).start();
		try {
			cdl.await(5000, TimeUnit.MILLISECONDS);
		} catch (InterruptedException expected) {
		}

		return result;
	}
	
	/**
	 * Retrieves a view-inputs document containing the FOXML, Fedora path information,
	 * parent collection pid, permissions and order within parent folder for the object
	 * identified by simplepid
	 * @param simplepid
	 * @return 
	 * @throws FedoraException
	 */
	public Document getObjectViewXML(String simplepid) throws FedoraException {
		final PID pid = new PID(simplepid);
		Document result = new Document();
		final Element inputs = new Element("view-inputs");
		result.setRootElement(inputs);

		// parallel threads for speed
		CountDownLatch cdl = new CountDownLatch(5);
		new StopLatchedThread(cdl, new GetFoxml(pid, inputs)).start();
		new StopLatchedThread(cdl, new GetPathInfo(pid, inputs)).start();
		new StopLatchedThread(cdl, new GetParentCollection(pid, inputs)).start();
		new StopLatchedThread(cdl, new GetPermissions(pid, inputs)).start();
		new StopLatchedThread(cdl, new GetOrderWithinParent(pid, inputs)).start();
		try {
			cdl.await(16000, TimeUnit.MILLISECONDS);
		} catch (InterruptedException expected) {
		}

		return result;
	}

	public edu.unc.lib.dl.fedora.ManagementClient getManagementClient() {
		return managementClient;
	}

	public void setManagementClient(
			edu.unc.lib.dl.fedora.ManagementClient managementClient) {
		this.managementClient = managementClient;
	}

	public edu.unc.lib.dl.util.TripleStoreQueryService getTripleStoreQueryService() {
		return tripleStoreQueryService;
	}

	public void setTripleStoreQueryService(
			edu.unc.lib.dl.util.TripleStoreQueryService tripleStoreQueryService) {
		this.tripleStoreQueryService = tripleStoreQueryService;
	}

	public void setAccessControlUtils(
			edu.unc.lib.dl.fedora.AccessControlUtils accessControlUtils) {
		this.accessControlUtils = accessControlUtils;
	}

	public class StopLatchedThread extends Thread {
		private final CountDownLatch stopLatch;

		public StopLatchedThread(CountDownLatch stopLatch, Runnable task) {
			super(task);
			this.stopLatch = stopLatch;
		}

		@Override
		public void run() {
			try {
				super.run();
			} finally {
				stopLatch.countDown();
			}
		}
	}
	
	/**
	 * Super class for Runnable objects used for retrieving metadata related
	 * to a specific Fedora object.
	 * @author bbpennel
	 *
	 */
	private abstract class DataThread implements Runnable {
		protected PID pid;
		protected Element inputs;
		
		public DataThread(PID pid, Element inputs){
			this.pid = pid;
			this.inputs = inputs;
		}
		
	}
	
	/**
	 * Retrieves FOXML and adds the results as a child of inputs. 
	 *
	 */
	private class GetFoxml extends DataThread {
		public GetFoxml(PID pid, Element inputs){
			super(pid, inputs);
		}
		
		@Override
		public void run() {
			try {
				
				LOG.debug("Get FOXML for pid " + pid.getPid());
				
				// add foxml
				Document foxml;
				foxml = getManagementClient().getObjectXML(pid);
				
				inputs.addContent(foxml.getRootElement().detach());
			} catch (FedoraException e) {
				throw new ServiceException("Failed to retrieve FOXML for " + pid.getPid() + " due to Fedora Exception", e);
			} catch (Exception e) {
				throw new ServiceException("Failed to retrieve FOXML for " + pid.getPid(), e);
			}
		}
	}
	
	/**
	 * Retrieves object path information indicating the object hierarchy leading up to the
	 * specified object and adds the results as a child of inputs named "path". 
	 *
	 */
	private class GetPathInfo extends DataThread {
		public GetPathInfo(PID pid, Element inputs){
			super(pid, inputs);
		}
		
		@Override
		public void run() {
			// add path info
			List<PathInfo> path = getTripleStoreQueryService()
					.lookupRepositoryPathInfo(pid);
			Element pathEl = new Element("path");
			inputs.addContent(pathEl);
			for (PathInfo i : path) {
				Element p = new Element("object");
				p.setAttribute("label", i.getLabel());
				p.setAttribute("pid", i.getPid().getPid());
				p.setAttribute("slug", i.getSlug());
				pathEl.addContent(p);
			}
		}
	}
	
	/**
	 * Retrieves Mods datastream for pid and adds the results as a child of inputs. 
	 *
	 */
	private class GetMods extends DataThread {
		public GetMods(PID pid, Element inputs){
			super(pid, inputs);
		}
		
		@Override
		public void run() {
			// add MODS
			try {
				byte[] modsBytes = getAccessClient().getDatastreamDissemination(pid, "MD_DESCRIPTIVE", null).getStream();
				Document mods = edu.unc.lib.dl.fedora.ClientUtils
						.parseXML(modsBytes);
				inputs.addContent(mods.getRootElement().detach());
			} catch (Exception e) {
				throw new ServiceException(e);
			}
		}
	}
	
	/**
	 * Retrieves the pid identifying the most immediate collection containing the object
	 * identified and adds the results as a child of inputs named "parentCollection". 
	 *
	 */
	private class GetParentCollection extends DataThread {
		public GetParentCollection(PID pid, Element inputs){
			super(pid, inputs);
		}
		
		@Override
		public void run() {
			try {
				PID parentCollection = getTripleStoreQueryService()
						.fetchParentCollection(pid);
				if (parentCollection == null)
					return;
				Element parentColEl = new Element("parentCollection");
				parentColEl.setText(parentCollection.getPid());
				inputs.addContent(parentColEl);
			} catch (Exception e) {
				throw new ServiceException(e);
			}
		}
	}
	
	/**
	 * Calculates and returns the effective permissions for the given pid, taking into 
	 * account inherited permissions.  Results are added as a child of inputs named "permissions". 
	 *
	 */
	private class GetPermissions extends DataThread {
		public GetPermissions(PID pid, Element inputs){
			super(pid, inputs);
		}
		
		@Override
		public void run() {
			accessControlUtils.processCdrAccessControl(pid, inputs);
		}
	}
	
	/**
	 * Retrieves the internal sort order value for the default sort within
	 * the folder/collection containing the object identified by pid and stores
	 * the results as a child of inputs named "order".
	 * 
	 */
	private class GetOrderWithinParent extends DataThread {
		public GetOrderWithinParent(PID pid, Element inputs){
			super(pid, inputs);
		}
		
		@Override
		public void run() {
			try {
				PID container = getTripleStoreQueryService()
						.fetchContainer(pid);
				byte[] structMapBytes = getAccessClient()
						.getDatastreamDissemination(container,
								"MD_CONTENTS", null).getStream();
				SAXParserFactory factory = SAXParserFactory.newInstance();
				factory.setNamespaceAware(true);
				factory.setValidating(false);
				SAXParser saxParser = factory.newSAXParser();
				StructMapOrderExtractor handler = new StructMapOrderExtractor(
						pid);
				saxParser.parse(new ByteArrayInputStream(structMapBytes),
						handler);
				if (handler.getOrder() != null) {
					Element orderEl = new Element("order");
					orderEl.setText(handler.getOrder());
					inputs.addContent(orderEl);
				}
			} catch (Exception e) {
				throw new ServiceException(e);
			}
		}
	}
}
