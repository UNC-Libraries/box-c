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

import java.io.BufferedReader;
import java.io.FileReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Element;
import org.jdom.Namespace;

import edu.unc.lib.dl.fedora.ManagementClient;
import edu.unc.lib.dl.fedora.NotFoundException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.schema.FixityReplicationObject;
import edu.unc.lib.dl.service.FixityReplicationService;
import edu.unc.lib.dl.util.Constants;
import edu.unc.lib.dl.util.PremisEventLogger;

import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

/**
 * 
 * 
 */
public class FixityReplicationServiceImpl implements FixityReplicationService {
	protected final Log logger = LogFactory.getLog(getClass());
	private ManagementClient managementClient;
	public static final Namespace NS = Namespace.getNamespace(JDOMNamespaceUtil.PREMIS_V2_NS.getURI());
	public static final String PID_TYPE = "PID";

	public enum LogType {
		GOOD_REPLICATION, BAD_REPLICATION, GOOD_FIXITY, BAD_FIXITY
	};

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.unc.lib.dl.service.SubmitService#metsSubmit(edu.unc.lib.dl.schema .MediatedSubmitIngestObject)
	 */
	public FixityReplicationObject fixityReplication(FixityReplicationObject request) {

		// Don't send files back down
		request.setGoodReplicationFile(null);
		request.setBadReplicationFile(null);
		request.setGoodFixityFile(null);
		request.setBadFixityFile(null);

		// Create and start the thread
		FixityReplicationThread thread = new FixityReplicationThread();
		thread.setRequest(request);
		thread.start();

		request.setMessage(Constants.IN_PROGRESS_THREADED);

		return request;
	}

	private String processLogFile(LogType id, String logFile, String agent) {
		String error = null;

		try {
			BufferedReader br = new BufferedReader(new FileReader(logFile));

			switch (id) {
				case GOOD_REPLICATION: {
					error = goodReplication(br, agent);
					break;
				}
				case BAD_REPLICATION: {
					error = badReplication(br, agent);
					break;
				}
				case GOOD_FIXITY: {
					error = goodFixity(br, agent);
					break;
				}
				case BAD_FIXITY: {
					error = badFixity(br, agent);
					break;
				}
			}

		} catch (Exception e) {
			logger.warn("Fixity/Replication unexpected exception", e);
			error = e.getLocalizedMessage();
		}

		return error;
	}

	private String goodReplication(BufferedReader br, String agent) {
		String error = null;

		String strLine = null;

		try {

			String timestamp = br.readLine(); // first line is the timestamp

			logger.debug("goodReplication");
			while ((strLine = br.readLine()) != null) {
				logger.debug("in while, input: " + strLine);

				strLine = strLine.trim();

				// extract UUID
				if (strLine.contains("uuid_")) {
					int uuidIndex = strLine.indexOf("uuid_");
					String uuid = strLine.substring(uuidIndex, strLine.indexOf(" ", uuidIndex));
					if (uuid == null) {
						logger.warn("Could not process bad replication check entry: " + strLine);
					} else {
						logger.debug(uuid);
						String fullUuid = uuid;

						uuid = convertUUID(uuid);

						PID pid = new PID(uuid);

						PremisEventLogger pelogger = new PremisEventLogger(agent);

						logger.debug("timestamp: " + timestamp + " uuid: " + uuid + " strLine: " + strLine);

						Element event = pelogger.logEvent(PremisEventLogger.Type.REPLICATION,
								"Replication check succeeded on " + timestamp, pid);

						addLinkingObjectIdentifier(fullUuid, event);
						
						PremisEventLogger.addDetailedOutcome(event, "success", "Message: " + strLine, null);

						try {
							this.managementClient.writePremisEventsToFedoraObject(pelogger, pid);
						} catch (NotFoundException e) {
							logger.debug("Assuming this is from the object not existing: " + strLine + " | exception: "
									+ e.getLocalizedMessage());
						}
					}
				}
			}

		} catch (Exception e) {
			logger.warn("Fixity/Replication unexpected exception", e);
			error = e.getLocalizedMessage();
		}

		return error;
	}

	private String badReplication(BufferedReader br, String agent) {
		String error = null;

		try {

			String strLine;

			String timestamp = br.readLine(); // first line is the timestamp

			logger.debug("badReplication");
			while ((strLine = br.readLine()) != null) {
				logger.debug("in while, input: " + strLine);

				strLine = strLine.trim();

				// Examples
				// ERROR: lsUtil: srcPath
				// /cdrZone/home/fedora/admin_REPOSITORY_SOFTWARE+MD_EVENTS+MD_EVENTS./uuid_cb95ebe0-0860-4856-bbc4-1430d2004a6a+MD_EVENTS+MD_EVENTS.0
				// does not exist or user lacks access permission
				// /cdrZone/home/fedora/datastreams/2011/1021/16/11/uuid_13754cd5-649c-4fb9-8460-37dcd74d57e0+DATA_FILE+DATA_FILE.0
				// has not been replicated to cdrResc2

				// extract UUID
				if (strLine.contains("uuid_")) {
					int uuidIndex = strLine.indexOf("uuid_");
					String uuid = strLine.substring(uuidIndex, strLine.indexOf(" ", uuidIndex));
					if (uuid == null) {
						logger.warn("Could not process bad replication check entry: " + strLine);
					} else {
						logger.debug(uuid);
						String fullUuid = uuid;

						uuid = convertUUID(uuid);

						PID pid = new PID(uuid);

						PremisEventLogger pelogger = new PremisEventLogger(agent);

						Element event = pelogger.logEvent(PremisEventLogger.Type.REPLICATION, "Replication check failed on "
								+ timestamp, pid);

						addLinkingObjectIdentifier(fullUuid, event);
						
						PremisEventLogger.addDetailedOutcome(event, "failure", "Message: " + strLine, null);

						logger.debug("timestamp: " + timestamp + " pid: " + pid.getPid() + " strLine: " + strLine);

						try {
							this.managementClient.writePremisEventsToFedoraObject(pelogger, pid);
						} catch (NotFoundException e) {
							logger.debug("Assuming this is from the object not existing: " + strLine + " | exception: "
									+ e.getLocalizedMessage());
						}
					}
				}
			}

		} catch (Exception e) {
			logger.warn("Fixity/Replication unexpected exception", e);
			error = e.getLocalizedMessage();
		}

		return error;
	}

	private String goodFixity(BufferedReader br, String agent) {
		String error = null;

		try {

			String strLine;

			String timestamp = br.readLine(); // first line is the timestamp

			logger.debug("goodFixity");
			while ((strLine = br.readLine()) != null) {
				logger.debug("in while, input: " + strLine);

				// if line starts with "uuid_", process it

				strLine = strLine.trim();
				if (strLine.contains("uuid_")) {

					// extract UUID
					int uuidIndex = strLine.indexOf("uuid_");
					String uuid = strLine.substring(uuidIndex, strLine.indexOf(" ", uuidIndex));
					if (uuid == null) {
						logger.warn("Could not process good fixity check entry: " + strLine);
					} else {
						logger.debug(uuid);
						String fullUuid = uuid;

						uuid = convertUUID(uuid);

						PID pid = new PID(uuid);

						PremisEventLogger pelogger = new PremisEventLogger(agent);

						logger.debug("timestamp: " + timestamp + " pid: " + pid.getPid() + " strLine: " + strLine);

						Element event = pelogger.logEvent(PremisEventLogger.Type.FIXITY_CHECK, "Fixity check succeeded on "
								+ timestamp, pid);

						addLinkingObjectIdentifier(fullUuid, event);
						
						PremisEventLogger.addDetailedOutcome(event, "success", "Message: " + strLine, null);

						try {
							this.managementClient.writePremisEventsToFedoraObject(pelogger, pid);
						} catch (NotFoundException e) {
							logger.debug("Assuming this is from the object not existing: " + strLine + " | exception: "
									+ e.getLocalizedMessage());
						}
					}
				}
			}

		} catch (Exception e) {
			logger.warn("Fixity/Replication unexpected exception", e);
			error = e.getLocalizedMessage();
		}

		return error;
	}

	private String badFixity(BufferedReader br, String agent) {
		String error = null;

		try {

			String strLine;

			String timestamp = br.readLine(); // first line is the timestamp

			logger.debug("badFixity");
			while ((strLine = br.readLine()) != null) {
				logger.debug("in while, input: " + strLine);

				// if line starts with "ERROR:", process it

				// /cdrZone/home/fedora/uuid_20120323.txt failed a checksum on resource cdrResc
				// ERROR: chksumDataObjUtil: rcDataObjChksum error for /cdrZone/home/fedora/uuid_20120323.txt status =
				// -314000 USER_CHKSUM_MISMATCH
				// ERROR: chksumUtil: chksum error for /cdrZone/home/fedora/uuid_20120323.txt, status = -314000 status =
				// -314000 USER_CHKSUM_MISMATCH
				// Total checksum performed = 1, Failed checksum = 1

				strLine = strLine.trim();
				if (strLine.startsWith("/")) {

					// extract UUID
					int uuidIndex = strLine.indexOf("uuid_");
					String uuid = strLine.substring(uuidIndex, strLine.indexOf(" ", uuidIndex));
					if (uuid == null) {
						logger.warn("Could not process bad fixity check entry: " + strLine);
					} else {
						logger.debug(uuid);
						String fullUuid = uuid;

						uuid = convertUUID(uuid);

						PID pid = new PID(uuid);

						PremisEventLogger pelogger = new PremisEventLogger(agent);

						logger.debug("timestamp: " + timestamp + " pid: " + pid.getPid() + " strLine: " + strLine);

						Element event = pelogger.logEvent(PremisEventLogger.Type.FIXITY_CHECK, "Fixity check failed on "
								+ timestamp, pid);

						addLinkingObjectIdentifier(fullUuid, event);
						
						PremisEventLogger.addDetailedOutcome(event, "failure", "Message: " + strLine, null);

						try {
							this.managementClient.writePremisEventsToFedoraObject(pelogger, pid);
						} catch (NotFoundException e) {
							logger.debug("Assuming this is from the object not existing: " + strLine + " | exception: "
									+ e.getLocalizedMessage());
						}
					}
				}
			}

		} catch (Exception e) {
			logger.warn("Fixity/Replication unexpected exception", e);
			error = e.getLocalizedMessage();
		}

		return error;
	}

	private void addLinkingObjectIdentifier(String input, Element event) {
		String pid = null;
		String dataStream = null;

		// /cdrZone/home/fedora/datastreams/2011/1021/16/11/uuid_13754cd5-649c-4fb9-8460-37dcd74d57e0+DATA_FILE+DATA_FILE.0 has not been replicated to cdrResc2

		
		logger.debug("addLinkingObjectIdentifier: "+input);
		
		if (input.contains("+")) {
			pid = input.substring(0, input.indexOf("+"));
			pid = pid.replace("_", ":");

			int plusIndex = input.indexOf("+");
			dataStream = input.substring(plusIndex+1, input.indexOf("+", plusIndex+1));

			logger.debug("addLinkingObjectIdentifier: "+pid+"/"+dataStream);
			
//			if (dataStream.contains(".")) { // remove version information
//				dataStream = dataStream.substring(0, dataStream.indexOf("."));
//			}

		} else
			return; // no datastream to process

		Element source = new Element("linkingObjectIdentifier", NS);
		source.addContent(new Element("linkingObjectIdentifierType", NS).setText(PID_TYPE));
		source.addContent(new Element("linkingObjectIdentifierValue", NS).setText(pid + "/" + dataStream));
		event.addContent(source);
	}

	// convert iRODS filename UUID to CDR UUID
	private String convertUUID(String input) {
		String result = null;

		// example input: uuid_8e5b10b2-8c85-49dc-8b99-2cba752a9ff0+MD_EVENTS+MD_EVENTS.1

		if (input.contains("+"))
			result = input.substring(0, input.indexOf("+"));
		else
			result = input;

		result = result.replace("_", ":");

		return result;
	}

	public ManagementClient getManagementClient() {
		return managementClient;
	}

	public void setManagementClient(ManagementClient managementClient) {
		this.managementClient = managementClient;
	}

	class FixityReplicationThread extends Thread {
		FixityReplicationObject request;

		@Override
		public void run() {

			String agent = request.getAdminOnyen();

			if (notNull(request.getGoodReplicationFileName()))
				processLogFile(LogType.GOOD_REPLICATION, request.getGoodReplicationFileName(), agent);

			if (notNull(request.getBadReplicationFileName()))
				processLogFile(LogType.BAD_REPLICATION, request.getBadReplicationFileName(), agent);

			if (notNull(request.getGoodFixityFileName()))
				processLogFile(LogType.GOOD_FIXITY, request.getGoodFixityFileName(), agent);

			if (notNull(request.getBadFixityFileName()))
				processLogFile(LogType.BAD_FIXITY, request.getBadFixityFileName(), agent);
		}

		public void setRequest(FixityReplicationObject request) {
			this.request = request;
		}

		private boolean notNull(String value) {
			if ((value == null) || (value.equals(""))) {
				return false;
			}

			return true;
		}
	}
}
