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
/**
 *
 */
package edu.unc.lib.dl.service.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.unc.lib.dl.agents.Agent;
import edu.unc.lib.dl.agents.AgentFactory;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.ingest.IngestException;
import edu.unc.lib.dl.ingest.aip.DepositRecord;
import edu.unc.lib.dl.ingest.sip.METSPackageSIP;
import edu.unc.lib.dl.ingest.sip.PreIngestEventLogger;
import edu.unc.lib.dl.ingest.sip.SingleFileSIP;
import edu.unc.lib.dl.ingest.sip.SingleFolderSIP;
import edu.unc.lib.dl.schema.CreateCollectionObject;
import edu.unc.lib.dl.schema.MediatedSubmitIngestObject;
import edu.unc.lib.dl.schema.MetsSubmitIngestObject;
import edu.unc.lib.dl.service.SubmitService;
import edu.unc.lib.dl.services.DigitalObjectManager;
import edu.unc.lib.dl.util.Constants;
import edu.unc.lib.dl.util.DepositMethod;
import edu.unc.lib.dl.util.TripleStoreQueryService;

/**
 * @author steve
 * 
 */
public class SubmitServiceImpl implements SubmitService {
	protected final Log logger = LogFactory.getLog(getClass());
	private AgentFactory agentManager;
	private DigitalObjectManager digitalObjectManager;
	private TripleStoreQueryService tripleStoreQueryService;

	private PID collectionsPID = null;

	private PID getCollectionsPID() {
		if (this.collectionsPID == null) {
			this.collectionsPID = this.getTripleStoreQueryService().fetchByRepositoryPath(Constants.COLLECTIONS);
		}
		return this.collectionsPID;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.unc.lib.dl.service.SubmitService#metsSubmit(edu.unc.lib.dl.schema .MediatedSubmitIngestObject)
	 */
	public MetsSubmitIngestObject metsSubmit(MetsSubmitIngestObject request) {
		String error = null;
		try {
			String name = request.getFileName();
			boolean zipFlag = false;
			if (name.endsWith(".zip")) {
				zipFlag = true;
			}
			File file = new File(name);
			Agent agent = agentManager.findPersonByOnyen(request.getAdminOnyen(), true);
			Agent owner = agentManager.getAgent(new PID(request.getOwnerPid()), false);
			PID containerPID = getTripleStoreQueryService().fetchByRepositoryPath(request.getFilePath());
			METSPackageSIP sip = new METSPackageSIP(containerPID, file, zipFlag);
			PreIngestEventLogger eventLogger = sip.getPreIngestEventLogger();
			setPremisVirusEvent(eventLogger, request.getVirusDate(), request.getVirusSoftware(), request.getOwnerPid());
			String note = "Added through UI";
			if (request.getMessage() != null) {
				note = request.getMessage();
			}
			DepositRecord record = new DepositRecord(agent, owner, DepositMethod.WebForm);
			record.setMessage(note);
			digitalObjectManager.addToIngestQueue(sip, record);
		} catch (IOException e) {
			logger.error("unexpected io error", e);
			error = "There was an unexpected error processing your ingest:\n <br />" + e.getLocalizedMessage();
		} catch (IngestException e) {
			error = e.getLocalizedMessage();
		} catch (Exception e) {
			logger.error("unexpected exception", e);
			error = e.getLocalizedMessage();
		}
		if (error != null) {
			request.setMessage(error);
		} else {
			request.setMessage(Constants.IN_PROGRESS_THREADED);
		}
		return request;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.unc.lib.dl.service.SubmitService#mediatedSubmit(edu.unc.lib.dl.schema .MediatedSubmitIngestObject)
	 */
	public MediatedSubmitIngestObject mediatedSubmit(MediatedSubmitIngestObject mediatedSubmitIngestObject) {
		String error = null;
		try {
			File file = new File(mediatedSubmitIngestObject.getFileName());

			// logger.debug("mediatedSubmit: "+mediatedSubmitIngestObject.getFileName());
			// logger.debug("mediatedSubmit file exists: "+file.exists());
			// logger.debug("mediatedSubmit file: "+file.getAbsolutePath());
			// logger.debug("mediatedSubmit file can read: "+file.canRead());
			// logger.debug("mediatedSubmit file length: "+file.length());

			File modsFile = new File(mediatedSubmitIngestObject.getMetadataName());

			Agent agent = agentManager.findPersonByOnyen(mediatedSubmitIngestObject.getAdminOnyen(), true);

			PID pid = new PID(mediatedSubmitIngestObject.getOwnerPid());
			Agent owner = agentManager.getAgent(pid, false);

			SingleFileSIP sip = new SingleFileSIP();
			PreIngestEventLogger eventLogger = sip.getPreIngestEventLogger();

			setPremisVirusEvent(eventLogger, mediatedSubmitIngestObject.getVirusDate(),
					mediatedSubmitIngestObject.getVirusSoftware(), mediatedSubmitIngestObject.getOwnerPid());

			setPremisChecksumEvent(eventLogger, mediatedSubmitIngestObject.getChecksumDate(),
					mediatedSubmitIngestObject.getChecksum(), mediatedSubmitIngestObject.getOwnerPid());

			PID containerPID = getTripleStoreQueryService()
					.fetchByRepositoryPath(mediatedSubmitIngestObject.getFilePath());
			sip.setContainerPID(containerPID);
			sip.setData(file);
			sip.setFileLabel(mediatedSubmitIngestObject.getOrigFileName());
			sip.setMimeType(mediatedSubmitIngestObject.getMimetype());
			sip.setModsXML(modsFile);
			DepositRecord record = new DepositRecord(agent, owner, DepositMethod.WebForm);
			record.setMessage("Added through UI");
			digitalObjectManager.addToIngestQueue(sip, record);
		} catch (IngestException e) {
			error = e.getLocalizedMessage();
		} catch (Exception e) {
			logger.error("Unexpected ingest exception", e);
			error = "An unexpect error occurred during ingest:\n<br />" + e.getLocalizedMessage();
		}
		if (error != null) {
			mediatedSubmitIngestObject.setMessage(error);
		} else {
			mediatedSubmitIngestObject.setMessage(Constants.IN_PROGRESS_THREADED);
		}
		return mediatedSubmitIngestObject;
	}

	private void setPremisVirusEvent(PreIngestEventLogger eventLogger, String date, String software, String person) {

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

		if ((date != null) && (software != null) && (person != null)) {
			try {
				eventLogger.addVirusScan(sdf.parse(date), software, person);
			} catch (ParseException e) {
				logger.error("date parse exception", e);
			}
		}

	}

	private void setPremisChecksumEvent(PreIngestEventLogger eventLogger, String date, String checksum, String person) {

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

		if ((date != null) && (checksum != null) && (person != null)) {
			if (checksum.trim().length() > 0) {
				try {
					eventLogger.addMD5ChecksumCalculation(sdf.parse(date), null, person);
				} catch (ParseException e) {
					logger.error("date parse exception", e);
				}
			}
		}
	}

	public AgentFactory getAgentManager() {
		return agentManager;
	}

	public void setAgentManager(AgentFactory agentManager) {
		this.agentManager = agentManager;
	}

	public DigitalObjectManager getDigitalObjectManager() {
		return digitalObjectManager;
	}

	public void setDigitalObjectManager(DigitalObjectManager digitalObjectManager) {
		this.digitalObjectManager = digitalObjectManager;
	}

	public CreateCollectionObject createCollection(CreateCollectionObject request) {

		try {

			File modsFile = File.createTempFile("test", "ing");
			FileOutputStream fos2 = new FileOutputStream(modsFile);
			fos2.write(request.getMetadata());
			fos2.close();

			Agent agent = agentManager.findPersonByOnyen(request.getAdminOnyen(), true);

			PID pid = new PID(request.getOwnerPid());
			Agent owner = agentManager.getAgent(pid, false);

			SingleFolderSIP sip = new SingleFolderSIP();

			sip.setContainerPID(this.getCollectionsPID());
			sip.setSlug(request.getFilePath());
			sip.setModsXML(modsFile);
			sip.setCollection(true);

			DepositRecord record = new DepositRecord(agent, owner, DepositMethod.WebForm);
			record.setMessage("Added through UI");
			digitalObjectManager.addWhileBlocking(sip, record);

			request.setMessage(Constants.SUCCESS);

		} catch (IOException e) {
			request.setMessage(Constants.FAILURE);
			logger.error("Unexpected IO exception", e);
			return request;
		} catch (IngestException e) {
			request.setMessage("Error during ingest: " + e.getLocalizedMessage());
			logger.error("Ingest exception", e);
			return request;
		} catch (Exception e) {
			request.setMessage(Constants.FAILURE);
			logger.error("Unexpected exception", e);
			return request;
		}

		request.setMetadata(new byte[0]);
		request.setFilePath("");

		return request;
	}

	public TripleStoreQueryService getTripleStoreQueryService() {
		return tripleStoreQueryService;
	}

	public void setTripleStoreQueryService(TripleStoreQueryService tripleStoreQueryService) {
		this.tripleStoreQueryService = tripleStoreQueryService;
	}
}
