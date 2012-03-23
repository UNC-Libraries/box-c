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
import java.io.DataInputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.kahadb.util.ByteArrayInputStream;
import org.jdom.Element;
import org.joda.time.DateTime;

import edu.unc.lib.dl.agents.Agent;
import edu.unc.lib.dl.agents.AgentFactory;
import edu.unc.lib.dl.fedora.ManagementClient;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.schema.FixityReplicationObject;
import edu.unc.lib.dl.service.FixityReplicationService;
import edu.unc.lib.dl.util.Constants;
import edu.unc.lib.dl.util.PremisEventLogger;

/**
 * @author steve
 * 
 */
public class FixityReplicationServiceImpl implements FixityReplicationService {
	protected final Log logger = LogFactory.getLog(getClass());
	private AgentFactory agentManager;
	private ManagementClient managementClient;

	public enum LogType {
		GOOD_REPLICATION, BAD_REPLICATION, GOOD_FIXITY, BAD_FIXITY
	};

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.unc.lib.dl.service.SubmitService#metsSubmit(edu.unc.lib.dl.schema .MediatedSubmitIngestObject)
	 */
	public FixityReplicationObject fixityReplication(FixityReplicationObject request) {
		String error = null;

		Agent agent = agentManager.findPersonByOnyen(request.getAdminOnyen(), true);

		error = processLogFile(LogType.GOOD_REPLICATION, request.getGoodReplicationFile(), agent);

		if (error == null)
			error = processLogFile(LogType.BAD_REPLICATION, request.getBadReplicationFile(), agent);

		if (error == null)
			error = processLogFile(LogType.GOOD_FIXITY, request.getGoodFixityFile(), agent);

		if (error == null)
			error = processLogFile(LogType.BAD_FIXITY, request.getBadFixityFile(), agent);

		// Don't send files back down
		request.setGoodReplicationFile(null);
		request.setBadReplicationFile(null);
		request.setGoodFixityFile(null);
		request.setBadFixityFile(null);

		if (error != null) {
			request.setMessage(error);
		} else {
			request.setMessage(Constants.IN_PROGRESS_THREADED);
		}
		return request;
	}

	private String processLogFile(LogType id, byte[] logFile, Agent agent) {
		String error = null;

		try {

			logger.debug("1...");

			DataInputStream dis = new DataInputStream(new ByteArrayInputStream(logFile));

			logger.debug("2...");

			BufferedReader br = new BufferedReader(new InputStreamReader(dis));

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

			logger.debug("4...");
			String strLine;

			logger.debug("5...");
			while ((strLine = br.readLine()) != null) {
				logger.debug("6...");
				logger.debug(strLine);
			}
			dis.close();

		} catch (Exception e) {
			logger.error("Fixity/Replication unexpected exception", e);
			error = e.getLocalizedMessage();
		}

		return error;
	}

	private String goodReplication(BufferedReader br, Agent agent) {
		String error = null;

		try {

			String strLine;

			logger.debug("a...");
			while ((strLine = br.readLine()) != null) {
				logger.debug("b...");
				logger.debug(strLine);
			}

		} catch (Exception e) {
			logger.error("Fixity/Replication unexpected exception", e);
			error = e.getLocalizedMessage();
		}

		return error;
	}

	private String badReplication(BufferedReader br, Agent agent) {
		String error = null;

		try {

			String strLine;

			String timestamp = br.readLine(); // first line is the timestamp

			logger.debug("badReplication");
			while ((strLine = br.readLine()) != null) {
				logger.debug("in while, input: " + strLine);

				// if line starts with "uuid_", process it

				strLine = strLine.trim();
				if (strLine.startsWith("uuid_")) {

					// TODO: extract UUID for all cases
					
					
					// extract UUID
					String uuid = strLine.substring(strLine.indexOf("uuid_"), strLine.indexOf(" "));
					if (uuid == null) {
						logger.error("Could not process bad replication check entry: " + strLine);
					} else {
						logger.debug(uuid);

						uuid = convertUUID(uuid);

						PID pid = new PID(uuid);

						PremisEventLogger logger = new PremisEventLogger(agent);
						DateTime dateTime = new DateTime(timestamp);

						Element event = logger.logEvent(PremisEventLogger.Type.REPLICATION, "Replication check failed at "
								+ timestamp, pid);
						logger.addDetailedOutcome(event, "failure", " Message: " + strLine, null);
						this.managementClient.writePremisEventsToFedoraObject(logger, pid);
					}
				}
			}

		} catch (Exception e) {
			logger.error("Fixity/Replication unexpected exception", e);
			error = e.getLocalizedMessage();
		}

		
		return error;
	}

	private String goodFixity(BufferedReader br, Agent agent) {
		String error = null;

		try {

			String strLine;

			String timestamp = br.readLine(); // first line is the timestamp

			logger.debug("goodFixity");
			while ((strLine = br.readLine()) != null) {
				logger.debug("in while, input: " + strLine);

				// if line starts with "uuid_", process it

				strLine = strLine.trim();
				if (strLine.startsWith("uuid_")) {

					// extract UUID
					String uuid = strLine.substring(strLine.indexOf("uuid_"), strLine.indexOf(" "));
					if (uuid == null) {
						logger.error("Could not process good fixity check entry: " + strLine);
					} else {
						logger.debug(uuid);

						uuid = convertUUID(uuid);

						PID pid = new PID(uuid);

						PremisEventLogger logger = new PremisEventLogger(agent);
						DateTime dateTime = new DateTime(timestamp);

						Element event = logger.logEvent(PremisEventLogger.Type.FIXITY_CHECK, "Fixity check succeeded at "
								+ timestamp, pid);
						logger.addDetailedOutcome(event, "success", " Message: " + strLine, null);
						this.managementClient.writePremisEventsToFedoraObject(logger, pid);
					}
				}
			}

		} catch (Exception e) {
			logger.error("Fixity/Replication unexpected exception", e);
			error = e.getLocalizedMessage();
		}

		return error;
	}

	private String badFixity(BufferedReader br, Agent agent) {
		String error = null;

		try {

			String strLine;

			String timestamp = br.readLine(); // first line is the timestamp

			logger.debug("badFixity");
			while ((strLine = br.readLine()) != null) {
				logger.debug("in while, input: " + strLine);

				// if line starts with "ERROR:", process it

				strLine = strLine.trim();
				if (strLine.startsWith("ERROR:")) {
					if (!strLine.endsWith("does not exist")) { // Object does not exist in iRODS; user will see this in bad
																				// fixity log; we have no object to store this message on

						// extract UUID
						String uuid = strLine.substring(strLine.indexOf("uuid_"), strLine.indexOf(","));
						if (uuid == null) {
							logger.error("Could not process bad fixity check entry: " + strLine);
						} else {
							logger.debug(uuid);

							uuid = convertUUID(uuid);

							PID pid = new PID(uuid);

							PremisEventLogger logger = new PremisEventLogger(agent);
							DateTime dateTime = new DateTime(timestamp);

							Element event = logger.logEvent(PremisEventLogger.Type.FIXITY_CHECK, "Fixity check failed at "
									+ timestamp, pid);
							logger.addDetailedOutcome(event, "failure", " Message: " + strLine, null);
							this.managementClient.writePremisEventsToFedoraObject(logger, pid);
						}
					}
				}
			}

		} catch (Exception e) {
			logger.error("Fixity/Replication unexpected exception", e);
			error = e.getLocalizedMessage();
		}

		return error;
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

	private void setPremisVirusEvent(PremisEventLogger eventLogger, String date, String software, String person) {

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

		if ((date != null) && (software != null) && (person != null)) {
			// try {
			// eventLogger.addVirusScan(sdf.parse(date), software, person);
			// } catch (ParseException e) {
			// logger.error("date parse exception", e);
			// }
		}

	}

	private void setPremisChecksumEvent(PremisEventLogger eventLogger, String date, String checksum, String person) {

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

		if ((date != null) && (checksum != null) && (person != null)) {
			// try {
			// eventLogger.addMD5ChecksumCalculation(sdf.parse(date), checksum,
			// person);
			// } catch (ParseException e) {
			// logger.error("date parse exception", e);
			// }
		}
	}

	public AgentFactory getAgentManager() {
		return agentManager;
	}

	public void setAgentManager(AgentFactory agentManager) {
		this.agentManager = agentManager;
	}

	public ManagementClient getManagementClient() {
		return managementClient;
	}

	public void setManagementClient(ManagementClient managementClient) {
		this.managementClient = managementClient;
	}
}
