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
import edu.unc.lib.dl.agents.AgentManager;
import edu.unc.lib.dl.fedora.NotFoundException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.ingest.IngestException;
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
import edu.unc.lib.dl.util.TripleStoreQueryService;

/**
 * @author steve
 *
 */
public class SubmitServiceImpl implements SubmitService {
	protected final Log logger = LogFactory.getLog(getClass());
	private AgentManager agentManager;
	private DigitalObjectManager digitalObjectManager;
	private TripleStoreQueryService tripleStoreQueryService;

	private PID collectionsPID = null;

	private PID getCollectionsPID() {
		if(this.collectionsPID == null) {
			this.collectionsPID = this.getTripleStoreQueryService().fetchByRepositoryPath(Constants.COLLECTIONS);
		}
		return this.collectionsPID;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * edu.unc.lib.dl.service.SubmitService#metsSubmit(edu.unc.lib.dl.schema
	 * .MediatedSubmitIngestObject)
	 */
	public MetsSubmitIngestObject metsSubmit(MetsSubmitIngestObject request) {

	    // Create and start the thread
	    MetsSubmitThread thread = new MetsSubmitThread();
	    thread.setMetsSubmitIngestObject(request);
	    thread.start();

	    request.setMessage(Constants.IN_PROGRESS_THREADED);

		return request;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * edu.unc.lib.dl.service.SubmitService#mediatedSubmit(edu.unc.lib.dl.schema
	 * .MediatedSubmitIngestObject)
	 */
	public MediatedSubmitIngestObject mediatedSubmit(
			MediatedSubmitIngestObject request) {

	    // Create and start the thread
	    MediatedSubmitThread thread = new MediatedSubmitThread();
	    thread.setMediatedSubmitIngestObject(request);
	    thread.start();

	    request.setMessage(Constants.IN_PROGRESS_THREADED);

		return request;
	}

	private void setPremisVirusEvent(PreIngestEventLogger eventLogger,
			String date, String software, String person) {

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

		if ((date != null) && (software != null) && (person != null)) {
			try {
				eventLogger.addVirusScan(sdf.parse(date), software, person);
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

	}

	private void setPremisChecksumEvent(PreIngestEventLogger eventLogger,
			String date, String checksum, String person) {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

		if ((date != null) && (checksum != null) && (person != null)) {
			try {
				eventLogger.addMD5ChecksumCalculation(sdf.parse(date), checksum,
						person);
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
	}

	public AgentManager getAgentManager() {
		return agentManager;
	}

	public void setAgentManager(AgentManager agentManager) {
		this.agentManager = agentManager;
	}

	public DigitalObjectManager getDigitalObjectManager() {
		return digitalObjectManager;
	}

	public void setDigitalObjectManager(
			DigitalObjectManager digitalObjectManager) {
		this.digitalObjectManager = digitalObjectManager;
	}

	public CreateCollectionObject createCollection(
			CreateCollectionObject request) {

		try {

			File modsFile = File.createTempFile("test", "ing");
			FileOutputStream fos2 = new FileOutputStream(modsFile);
			fos2.write(request.getMetadata());
			fos2.close();

			Agent agent = agentManager.findPersonByOnyen(request
					.getAdminOnyen(), true);

			PID pid = new PID(request.getOwnerPid());
			Agent owner = agentManager.getAgent(pid, false);

			SingleFolderSIP sip = new SingleFolderSIP();

			sip.setContainerPID(this.getCollectionsPID());
			sip.setSlug(request.getFilePath());
			sip.setModsXML(modsFile);
			sip.setOwner(owner);
			sip.setCollection(true);

			digitalObjectManager.addSingleObject(sip, agent, "Added through UI");

			request.setMessage(Constants.SUCCESS);

		} catch (IOException e) {
			request.setMessage(Constants.FAILURE);

			e.printStackTrace();

			return request;
		} catch (NotFoundException e) {
			request.setMessage(Constants.FAILURE);

			e.printStackTrace();
			return request;
		} catch (IngestException e) {
			request.setMessage(Constants.FAILURE);

			e.printStackTrace();
			return request;
		} catch (Exception e) {
			request.setMessage(Constants.FAILURE);

			e.printStackTrace();
			return request;
		}

		request.setMetadata(new byte[0]);
		request.setFilePath("");

		return request;
	}

    class MetsSubmitThread extends Thread {
 		MetsSubmitIngestObject metsSubmitIngestObject;

        @Override
		public void run() {
    		boolean flag = false;

    		try {

    			String name = metsSubmitIngestObject.getFileName();

    			if (name.endsWith(".zip")) {
    				flag = true;
    			}

    			File file = new File(name);

    			Agent agent = agentManager.findPersonByOnyen(metsSubmitIngestObject
    					.getAdminOnyen(), true);

    			PID containerPID = getTripleStoreQueryService().fetchByRepositoryPath(metsSubmitIngestObject.getFilePath());

    			METSPackageSIP sip = new METSPackageSIP(containerPID,	file, agent, flag);
    			PreIngestEventLogger eventLogger = sip.getPreIngestEventLogger();

    			setPremisVirusEvent(eventLogger, metsSubmitIngestObject.getVirusDate(), metsSubmitIngestObject
    					.getVirusSoftware(), metsSubmitIngestObject.getOwnerPid());

    			digitalObjectManager.addBatch(sip, agent, "Added through UI");
    		} catch (IOException e) {
    			e.printStackTrace();
    		} catch (NotFoundException e) {
    			e.printStackTrace();
    		} catch (IngestException e) {
    			e.printStackTrace();
    		} catch (Exception e) {
    			e.printStackTrace();
    		}
        }

        public void setMetsSubmitIngestObject(
				MetsSubmitIngestObject metsSubmitIngestObject) {
			this.metsSubmitIngestObject = metsSubmitIngestObject;
		}
   }

    class MediatedSubmitThread extends Thread {
    	MediatedSubmitIngestObject mediatedSubmitIngestObject;

    	@Override
		public void run() {
    		try {
    			File file = new File(mediatedSubmitIngestObject.getFileName());

//    			logger.debug("mediatedSubmit: "+mediatedSubmitIngestObject.getFileName());
//    			logger.debug("mediatedSubmit file exists: "+file.exists());
//    			logger.debug("mediatedSubmit file: "+file.getAbsolutePath());
//    			logger.debug("mediatedSubmit file can read: "+file.canRead());
//    			logger.debug("mediatedSubmit file length: "+file.length());

    			File modsFile = new File(mediatedSubmitIngestObject.getMetadataName());

    			Agent agent = agentManager.findPersonByOnyen(mediatedSubmitIngestObject
    					.getAdminOnyen(), true);

    			PID pid = new PID(mediatedSubmitIngestObject.getOwnerPid());
    			Agent owner = agentManager.getAgent(pid, false);

    			SingleFileSIP sip = new SingleFileSIP();
    			PreIngestEventLogger eventLogger = sip.getPreIngestEventLogger();

    			setPremisVirusEvent(eventLogger, mediatedSubmitIngestObject.getVirusDate(), mediatedSubmitIngestObject
    					.getVirusSoftware(), mediatedSubmitIngestObject.getOwnerPid());

    			setPremisChecksumEvent(eventLogger, mediatedSubmitIngestObject.getChecksumDate(), mediatedSubmitIngestObject
    					.getChecksum(), mediatedSubmitIngestObject.getOwnerPid());

    			PID containerPID = getTripleStoreQueryService().fetchByRepositoryPath(mediatedSubmitIngestObject.getFilePath());
    			sip.setContainerPID(containerPID);
    			sip.setData(file);
    			sip.setFileLabel(mediatedSubmitIngestObject.getOrigFileName());
    			sip.setMimeType(mediatedSubmitIngestObject.getMimetype());
    			sip.setModsXML(modsFile);
    			sip.setOwner(owner);

    			digitalObjectManager.addBatch(sip, agent, "Added through UI");
    		} catch (NotFoundException e) {
    			e.printStackTrace();
    		} catch (IngestException e) {
    			e.printStackTrace();
    		} catch (Exception e) {
    			e.printStackTrace();
    		}
        }

		public void setMediatedSubmitIngestObject(
				MediatedSubmitIngestObject mediatedSubmitIngestObject) {
			this.mediatedSubmitIngestObject = mediatedSubmitIngestObject;
		}
    }

	public TripleStoreQueryService getTripleStoreQueryService() {
		return tripleStoreQueryService;
	}

	public void setTripleStoreQueryService(TripleStoreQueryService tripleStoreQueryService) {
		this.tripleStoreQueryService = tripleStoreQueryService;
	}
}
