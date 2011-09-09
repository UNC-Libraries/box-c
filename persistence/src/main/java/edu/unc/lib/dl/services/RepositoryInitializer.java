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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.springframework.core.io.Resource;
import org.springframework.ws.soap.client.SoapFaultClientException;

import edu.unc.lib.dl.agents.AgentManager;
import edu.unc.lib.dl.agents.GroupAgent;
import edu.unc.lib.dl.agents.PersonAgent;
import edu.unc.lib.dl.agents.SoftwareAgent;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.ManagementClient;
import edu.unc.lib.dl.fedora.NotFoundException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.fedora.ServiceException;
import edu.unc.lib.dl.ingest.IngestException;
import edu.unc.lib.dl.ingest.sip.METSPackageSIP;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.PremisEventLogger;
import edu.unc.lib.dl.util.TripleStoreQueryService;
import edu.unc.lib.dl.xml.FOXMLJDOMUtil;

public class RepositoryInitializer {

	private static class AutoInitializer implements Runnable {
		private RepositoryInitializer init = null;
		private ManagementClient mgmt = null;

		AutoInitializer(RepositoryInitializer init, ManagementClient mgmt) {
			this.init = init;
			this.mgmt = mgmt;
		}

		@Override
		public void run() {
			// loop to detect when fedora is up
			boolean fedoraUp = false;
			int maxAttempts = 20;
			int secondsBetweenAttempts = 5;
			for (int count = 1; count <= maxAttempts; count++) {
				log.info("Waiting " + secondsBetweenAttempts + " seconds for Fedora service to start...");
				try {
					Thread.sleep(secondsBetweenAttempts * 1000);
				} catch (InterruptedException interrupt) {
				}
				try {
					log.info("Contacting Fedora service (attempt " + count + "/" + maxAttempts + ")");
					fedoraUp = this.mgmt.getNextPID(1, "admin").size() == 1;
					break;
				} catch (Exception e) {
					// fedora still down..
				}
				log.info("Fedora service not available yet.");
			}
			if (fedoraUp) {
				Throwable initException = null;
				try {
					log.info("Fedora service contacted, starting repository initializer");
					init.initializeRepository(false);
				} catch (Error e) {
					initException = e;
				} catch (Exception e) {
					initException = e;
				} finally {
					if (initException != null) {
						log.error("REPOSITORY INITIALIZATION ERROR", initException);
						String host = init.getManagementClient().getFedoraContextUrl();
						init.getMailNotifier().sendAdministratorMessage(
								"REPOSITORY INITIALIZATION ERROR",
								"Cannot initialize Fedora repository at this URL: " + host + "\nERROR: "
										+ initException.getMessage());
					}
				}
			} else {
				String msg = "Cannot contact Fedora service after " + maxAttempts + " attempts at "
						+ secondsBetweenAttempts + " second intervals.";
				log.error("REPOSITORY INITIALIZATION ERROR: " + msg);
				String host = init.getManagementClient().getFedoraContextUrl();
				init.getMailNotifier().sendAdministratorMessage("REPOSITORY INITIALIZATION ERROR",
						"Cannot initialize Fedora repository at this URL: " + host + "\nERROR: " + msg);
			}
		}
	}

	private static final Log log = LogFactory.getLog(RepositoryInitializer.class);
	private AgentManager agentManager = null;
	private String autoinitialize = null;
	DigitalObjectManagerImpl digitalObjectManager = null;

	private MailNotifier mailNotifier = null;

	public MailNotifier getMailNotifier() {
		return mailNotifier;
	}

	public void setMailNotifier(MailNotifier mailNotifier) {
		this.mailNotifier = mailNotifier;
	}

	private FolderManager folderManager = null;
	private Map<String, String> initialAdministrators = null;
	private List<Resource> foxmlObjects = null;

	public List<Resource> getFoxmlObjects() {
		return foxmlObjects;
	}

	public void setFoxmlObjects(List<Resource> foxmlObjects) {
		this.foxmlObjects = foxmlObjects;
	}

	private ManagementClient managementClient = null;

	private TripleStoreQueryService tripleStoreQueryService = null;

	public AgentManager getAgentManager() {
		return agentManager;
	}

	public String getAutoinitialize() {
		return autoinitialize;
	}

	public DigitalObjectManagerImpl getDigitalObjectManager() {
		return digitalObjectManager;
	}

	public FolderManager getFolderManager() {
		return folderManager;
	}

	public Map<String, String> getInitialAdministrators() {
		return initialAdministrators;
	}

	public ManagementClient getManagementClient() {
		return managementClient;
	}

	public TripleStoreQueryService getTripleStoreQueryService() {
		return tripleStoreQueryService;
	}

	/**
	 * This is a callback method for Spring bean initialization. It initializes the repository if necessary.
	 */
	public void init() {
		if ("yes".equals(this.getAutoinitialize().toLowerCase())) {
			// start a thread that waits for Fedora availability
			AutoInitializer ai = new AutoInitializer(this, this.getManagementClient());
			new Thread(ai, "RepositoryAutoInitializer").start();
		}
	}

	/**
	 * Initializes the repository, adding necessary system objects to Fedora.
	 *
	 * @param forceDelete
	 *           if true then any existing repository objects will be removed
	 */
	public void initializeRepository(boolean forceDelete) {
		try {
			if (this.agentManager == null || this.digitalObjectManager == null || this.folderManager == null
					|| this.foxmlObjects == null || this.initialAdministrators == null || this.managementClient == null
					|| this.tripleStoreQueryService == null) {
				log.error("RepositoryInitializer is missing dependencies and cannot run as configured.");
				return;
			}
			SoftwareAgent repoSoftware = AgentManager.getRepositorySoftwareAgentStub();
			// if (forceDelete) {
			// PID existingRepo =
			// this.getTripleStoreQueryService().fetchByRepositoryPath("/");
			// if (existingRepo != null) {
			// // get the children and delete them.
			// log.warn("FORCE INITIALIZING THE REPOSITORY TREE");
			// try {
			// this.getDigitalObjectManager().delete(existingRepo, repoSoftware,
			// "RepositoryInitializer is force deleting all REPOSITORY TREE objects.");
			// } catch (IngestException e) {
			// throw new Error("Unexpected", e);
			// } catch (NotFoundException e) {
			// throw new Error("Unexpected", e);
			// }
			// }
			// }

			PID root = this.getTripleStoreQueryService().fetchByRepositoryPath("/");
			if (root != null) {
				this.digitalObjectManager.setAvailable(true);
				log.warn("Repository ready (previously initialized).  Please disable RepositoryInitializer after initial startup of an empty repository.");
				return;
			}

			ingestFOXMLObjects();

			// create REPOSITORY folder
			String slug = "REPOSITORY";
			PID pid = ContentModelHelper.Administrative_PID.REPOSITORY.getPID();
			Document foxml = FOXMLJDOMUtil.makeFOXMLDocument(pid.getPid());
			FOXMLJDOMUtil.setProperty(foxml, FOXMLJDOMUtil.ObjectProperty.label, "Repository Folder");
			PremisEventLogger logger = new PremisEventLogger(repoSoftware);
			logger.logEvent(PremisEventLogger.Type.CREATION, "Created repository root container.", pid);

			// upload the MD_EVENTS datastream
			Document MD_EVENTS = new Document(logger.getObjectEvents(pid));
			String eventsLoc = this.getManagementClient().upload(MD_EVENTS);
			Element eventsEl = FOXMLJDOMUtil.makeLocatorDatastream("MD_EVENTS", "M", eventsLoc, "text/xml", "URL",
					"PREMIS Events Metadata", false, null);
			foxml.getRootElement().addContent(eventsEl);

			PID realpid = this.getManagementClient().ingest(foxml, ManagementClient.Format.FOXML_1_1,
					"initializing repository");
			this.getManagementClient().addLiteralStatement(realpid,
					ContentModelHelper.CDRProperty.slug.getURI().toString(), slug, null); // sending
			// null
			// datatype
			this.getManagementClient().addResourceStatement(realpid,
					ContentModelHelper.FedoraProperty.hasModel.getURI().toString(),
					ContentModelHelper.Model.CONTAINER.getURI().toString());

			GroupAgent adminGroup = AgentManager.getAdministrativeGroupAgentStub();

			// Now ingest the basic admin folders
			// the METS file will be deleted, so copy it to temp space first..
			File upload = null;
			try {
				upload = File.createTempFile("repoinit", ".xml");
				BufferedInputStream in = new BufferedInputStream(this.getClass().getClassLoader()
						.getResourceAsStream("repository_bootstrap_mets.xml"));
				BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(upload), 4096);
				byte[] bytes = new byte[4096];
				for (int len = in.read(bytes); len > 0; len = in.read(bytes)) {
					out.write(bytes, 0, len);
				}
				in.close();
				out.close();
			} catch (IOException e) {
				throw new Error(e);
			}

			// make object manager available for bootstrap ingests and further
			// operations
			this.getDigitalObjectManager().setAvailable(true, "available");

			try {
				METSPackageSIP basics = new METSPackageSIP("/", upload, adminGroup, false);
				basics.setAllowIndexing(false);
				this.getDigitalObjectManager().add(basics, repoSoftware, "adding admin folders objects", false);
				// this.getDigitalObjectManager().add(this.getAdministrorSIP(),
				// null, "adding administrator agents");
			} catch (IOException e) {
				throw new Error("Could not add bootstrap METS file.", e);
			} catch (IngestException e) {
				throw new Error("Could not ingest bootstrap METS file.", e);
			}

			// Now create the administrative users
			try {
				adminGroup = this.getAgentManager().addGroupAgent(adminGroup, repoSoftware, "creating admin group");
			} catch (IngestException e) {
				throw new Error("Could not create administrator group.", e);
			}

			// create the repository management software agent
			try {
				repoSoftware = this.getAgentManager().addSoftwareAgent(repoSoftware, repoSoftware,
						"creating repository management software agent");
			} catch (IngestException e) {
				throw new Error("Could not create repository management software agent.", e);
			}

			try {
				for (Map.Entry<String, String> admin : this.initialAdministrators.entrySet()) {
					PersonAgent a = new PersonAgent(admin.getValue(), admin.getKey());
					a = this.getAgentManager().addPersonAgent(a, repoSoftware, "initializing administrators");
					this.getAgentManager().addMembership(adminGroup, a, adminGroup);
				}
			} catch (IngestException e) {
				throw new Error("Cannot initialize administrators, see log", e);
			} catch (NotFoundException e) {
				throw new Error("Cannot initialize administrators, see log", e);
			}

			// create the collections folder
			try {
				this.getFolderManager().createPath("/Collections", repoSoftware, repoSoftware);
			} catch (IngestException e) {
				throw new Error("Cannot initialize collections folder, see log", e);
			}
			this.digitalObjectManager.setAvailable(true);
			log.info("Repository initialized");
		} catch (FedoraException e) {
			log.error("Could not initialize repository, Fedora service fault", e);
			throw new Error("Could not initialize repository, Fedora service fault", e);
		} catch (ServiceException e) {
			log.error("Fedora service misconfigured or unavailable during initialization", e);
			throw new Error("Fedora service misconfigured or unavailable during initialization", e);
		} catch (RuntimeException e) {
			log.error("Could not initialize repository, unexpected exception", e);
			throw new Error("Could not initialize repository, unexpected exception", e);
		}
	}

	/**
	 * loads every XML file in foxml-objects, purges their pids and ingests them.
	 *
	 * @throws FedoraException
	 */
	private void ingestFOXMLObjects() throws FedoraException {
		if (this.getFoxmlObjects() != null) {
			for (Resource r : this.getFoxmlObjects()) {
				log.info("Loading foxml object: " + r.getFilename());
				Document f = null;
				try {
					f = new org.jdom.input.SAXBuilder().build(r.getInputStream());
					PID p = new PID(f.getRootElement().getAttributeValue("PID"));
					this.getManagementClient().purgeObject(p, "reinitializing repository", false);
					log.info("old object " + p + " was removed");
				} catch (JDOMException e) {
					throw new Error("Unexpected exception", e);
				} catch (IOException e) {
					throw new Error("Unexpected exception", e);
				} catch (NotFoundException ignored) {
				} catch (FedoraException ignored) {
				} catch (SoapFaultClientException ignored) {
				}
				this.getManagementClient().ingest(f, edu.unc.lib.dl.fedora.ManagementClient.Format.FOXML_1_1,
						"Initializing foxml objects");
			}
		}
	}

	public void setAgentManager(AgentManager agentManager) {
		this.agentManager = agentManager;
	}

	public void setAutoinitialize(String autoinitialize) {
		this.autoinitialize = autoinitialize;
	}

	public void setDigitalObjectManager(DigitalObjectManagerImpl digitalObjectManager) {
		this.digitalObjectManager = digitalObjectManager;
	}

	public void setFolderManager(FolderManager folderManager) {
		this.folderManager = folderManager;
	}

	public void setInitialAdministrators(Map<String, String> initialAdministrators) {
		this.initialAdministrators = initialAdministrators;
	}

	public void setManagementClient(ManagementClient managementClient) {
		this.managementClient = managementClient;
	}

	public void setTripleStoreQueryService(TripleStoreQueryService tripleStoreQueryService) {
		this.tripleStoreQueryService = tripleStoreQueryService;
	}

}
