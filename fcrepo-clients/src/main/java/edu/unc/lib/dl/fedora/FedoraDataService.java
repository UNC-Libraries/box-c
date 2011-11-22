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

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.jdom.Content;
import org.jdom.Document;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.TripleStoreQueryService.PathInfo;

/**
 * Fedora data retrieval class used for accessing data streams and performing Mulgara queries to generate XML views of
 * Fedora objects for outside usage.
 * 
 * @author Gregory Jansen
 * @author Ben Pennell
 */
public class FedoraDataService {

	private static final Logger LOG = LoggerFactory.getLogger(FedoraDataService.class);

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
	 * Retrieves a view-inputs document containing the FOXML datastream for the object identified by simplepid
	 * 
	 * @param simplepid
	 * @return
	 * @throws FedoraException
	 */
	public Document getFoxmlViewXML(String simplepid) throws FedoraException {
		final PID pid = new PID(simplepid);
		Document result = new Document();
		final Element inputs = new Element("view-inputs");
		result.setRootElement(inputs);

		ExecutorService executor = Executors.newFixedThreadPool(1);

		Callable<Content> callable = new GetFoxml(pid);
		Future<Content> future = executor.submit(callable);

		try {
			Content foxml = future.get(5000, TimeUnit.MILLISECONDS);
			if (foxml != null) {
				inputs.addContent(foxml);
				return result;
			}
		} catch (Exception e) {
			throw new ServiceException("Failed to get FOXML body for " + pid.getPid() + ".", e);
		}

		return null;
	}

	/**
	 * Retrieves a view-inputs document containing the Mods datastream for the object identified by simplepid
	 * 
	 * @param simplepid
	 * @return
	 * @throws FedoraException
	 */
	public Document getModsViewXML(String simplepid) throws FedoraException {
		final PID pid = new PID(simplepid);
		Document result = new Document();
		final Element inputs = new Element("view-inputs");
		result.setRootElement(inputs);

		ExecutorService executor = Executors.newFixedThreadPool(1);

		Callable<Content> callable = new GetMods(pid);
		Future<Content> future = executor.submit(callable);

		try {
			Content mods = future.get(5000, TimeUnit.MILLISECONDS);
			if (mods != null) {
				inputs.addContent(mods);
				return result;
			}
		} catch (Exception e) {
			throw new ServiceException("Failed to get MODS data for " + pid.getPid() + ".", e);
		}

		return null;
	}

	public Document getObjectViewXML(String simplepid) throws FedoraException {
		return getObjectViewXML(simplepid, false);
	}

	/**
	 * Retrieves a view-inputs document containing the FOXML, Fedora path information, parent collection pid, permissions
	 * and order within parent folder for the object identified by simplepid
	 * 
	 * @param simplepid
	 * @return
	 * @throws FedoraException
	 */
	public Document getObjectViewXML(String simplepid, boolean failOnException) throws FedoraException {
		final PID pid = new PID(simplepid);
		Document result = new Document();
		final Element inputs = new Element("view-inputs");
		result.setRootElement(inputs);

		List<Callable<Content>> callables = new ArrayList<Callable<Content>>();

		callables.add(new GetFoxml(pid));
		callables.add(new GetPathInfo(pid));
		callables.add(new GetParentCollection(pid));
		callables.add(new GetPermissions(pid));
		callables.add(new GetOrderWithinParent(pid));

		ExecutorService executor = Executors.newFixedThreadPool(callables.size());

		Collection<Future<Content>> futures = new ArrayList<Future<Content>>(callables.size());

		for (Callable<Content> callable : callables) {
			futures.add(executor.submit(callable));
		}

		for (Future<Content> future : futures) {
			try {
				Content results = future.get(10000L, TimeUnit.MILLISECONDS);
				if (results != null) {
					inputs.addContent(results);
				}
			} catch (Exception e) {
				if (failOnException) {
					throw new ServiceException("Failed to getObjectViewXML for " + pid.getPid(), e);
				}
				LOG.error("Failed to getObjectViewXML for " + pid.getPid() + ", continuing.", e);
			}
		}

		return result;
	}

	public edu.unc.lib.dl.fedora.ManagementClient getManagementClient() {
		return managementClient;
	}

	public void setManagementClient(edu.unc.lib.dl.fedora.ManagementClient managementClient) {
		this.managementClient = managementClient;
	}

	public edu.unc.lib.dl.util.TripleStoreQueryService getTripleStoreQueryService() {
		return tripleStoreQueryService;
	}

	public void setTripleStoreQueryService(edu.unc.lib.dl.util.TripleStoreQueryService tripleStoreQueryService) {
		this.tripleStoreQueryService = tripleStoreQueryService;
	}

	public void setAccessControlUtils(edu.unc.lib.dl.fedora.AccessControlUtils accessControlUtils) {
		this.accessControlUtils = accessControlUtils;
	}

	/**
	 * Retrieves FOXML and adds the results as a child of inputs.
	 * 
	 */
	private class GetFoxml implements Callable<Content> {
		private PID pid;

		public GetFoxml(PID pid) {
			this.pid = pid;
		}

		@Override
		public Content call() {
			try {

				LOG.debug("Get FOXML for pid " + pid.getPid());

				// add foxml
				Document foxml;
				foxml = managementClient.getObjectXML(pid);

				return foxml.getRootElement().detach();
			} catch (FedoraException e) {
				throw new ServiceException("Failed to retrieve FOXML for " + pid.getPid() + " due to Fedora Exception", e);
			} catch (Exception e) {
				throw new ServiceException("Failed to retrieve FOXML for " + pid.getPid(), e);
			}
		}
	}

	/**
	 * Retrieves object path information indicating the object hierarchy leading up to the specified object and adds the
	 * results as a child of inputs named "path".
	 * 
	 */
	private class GetPathInfo implements Callable<Content> {
		private PID pid;

		public GetPathInfo(PID pid) {
			this.pid = pid;
		}

		@Override
		public Content call() {
			LOG.debug("Get path info for " + pid.getPid());
			// add path info
			List<PathInfo> path = tripleStoreQueryService.lookupRepositoryPathInfo(pid);
			if (path == null || path.size() == 0)
				throw new ServiceException("No path information was returned for " + pid.getPid());
			Element pathEl = new Element("path");
			for (PathInfo i : path) {
				Element p = new Element("object");
				p.setAttribute("label", i.getLabel());
				p.setAttribute("pid", i.getPid().getPid());
				p.setAttribute("slug", i.getSlug());
				pathEl.addContent(p);
			}
			return pathEl;
		}
	}

	/**
	 * Retrieves Mods datastream for pid and adds the results as a child of inputs.
	 * 
	 */
	private class GetMods implements Callable<Content> {
		private PID pid;

		public GetMods(PID pid) {
			this.pid = pid;
		}

		@Override
		public Content call() {
			// add MODS
			try {
				LOG.debug("Get mods for " + pid.getPid());
				byte[] modsBytes = getAccessClient().getDatastreamDissemination(pid, "MD_DESCRIPTIVE", null).getStream();
				Document mods = edu.unc.lib.dl.fedora.ClientUtils.parseXML(modsBytes);
				return mods.getRootElement().detach();
			} catch (Exception e) {
				throw new ServiceException(e);
			}
		}
	}

	/**
	 * Retrieves the pid identifying the most immediate collection containing the object identified and adds the results
	 * as a child of inputs named "parentCollection".
	 * 
	 */
	private class GetParentCollection implements Callable<Content> {
		private PID pid;

		public GetParentCollection(PID pid) {
			this.pid = pid;
		}

		@Override
		public Content call() {
			try {
				LOG.debug("Get parent collection for " + pid.getPid());
				PID parentCollection = tripleStoreQueryService.fetchParentCollection(pid);
				if (parentCollection == null)
					return null;
				Element parentColEl = new Element("parentCollection");
				parentColEl.setText(parentCollection.getPid());
				return parentColEl;
			} catch (Exception e) {
				throw new ServiceException(e);
			}
		}
	}

	/**
	 * Calculates and returns the effective permissions for the given pid, taking into account inherited permissions.
	 * Results are added as a child of inputs named "permissions".
	 * 
	 */
	private class GetPermissions implements Callable<Content> {
		private PID pid;

		public GetPermissions(PID pid) {
			this.pid = pid;
		}

		@Override
		public Content call() {
			LOG.debug("Get access control for " + pid.getPid());
			return accessControlUtils.processCdrAccessControl(pid);
		}
	}

	/**
	 * Retrieves the internal sort order value for the default sort within the folder/collection containing the object
	 * identified by pid and stores the results as a child of inputs named "order".
	 * 
	 */
	private class GetOrderWithinParent implements Callable<Content> {
		private PID pid;

		public GetOrderWithinParent(PID pid) {
			this.pid = pid;
		}

		@Override
		public Content call() {
			try {
				LOG.debug("Get Order within Parent for " + pid.getPid());
				PID container = tripleStoreQueryService.fetchContainer(pid);
				byte[] structMapBytes = getAccessClient().getDatastreamDissemination(container, "MD_CONTENTS", null)
						.getStream();
				SAXParserFactory factory = SAXParserFactory.newInstance();
				factory.setNamespaceAware(true);
				factory.setValidating(false);
				SAXParser saxParser = factory.newSAXParser();
				StructMapOrderExtractor handler = new StructMapOrderExtractor(pid);
				saxParser.parse(new ByteArrayInputStream(structMapBytes), handler);
				if (handler.getOrder() != null) {
					Element orderEl = new Element("order");
					orderEl.setText(handler.getOrder());
					return orderEl;
				}
				return null;
			} catch (Exception e) {
				throw new ServiceException(e);
			}
		}
	}
}
