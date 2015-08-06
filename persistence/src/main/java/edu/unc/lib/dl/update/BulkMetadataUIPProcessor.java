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
package edu.unc.lib.dl.update;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.ManagementClient;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.services.DigitalObjectManager;
import edu.unc.lib.dl.util.ContentModelHelper.Datastream;
import edu.unc.lib.dl.util.RedisWorkerConstants;

/**
 * Update processor which performs bulk metadata imports from a CDR metadata package
 * 
 * @author bbpennel
 * @date Jul 21, 2015
 */
public class BulkMetadataUIPProcessor implements UIPProcessor {
	
	private static Logger log = LoggerFactory.getLogger(BulkMetadataUIPProcessor.class);
	
	private DigitalObjectManager digitalObjectManager;
	private ManagementClient managementClient;
	private UIPUpdatePipeline pipeline;
	private JedisPool jedisPool;

	@Override
	public void process(UpdateInformationPackage uip) throws UpdateException, UIPException {
		if (!(uip instanceof BulkMetadataUIP)) {
			throw new UIPException("Incorrect UIP class, found " + uip.getClass().getName() + ", expected "
					+ BulkMetadataUIP.class.getName());
		}
		
		BulkMetadataUIP bulkUIP = (BulkMetadataUIP) uip;
		
		if (bulkUIP.isExistingUpdate()) {
			resume(bulkUIP);
		} else {
			// Store data related to this update in case it is interrupted
			storeUpdateInformation(bulkUIP);
		}
		
		// Wait for the repository to become available before proceeding with updates
		while (!managementClient.isRepositoryAvailable()) {
			try {
				Thread.sleep(10000L);
			} catch (InterruptedException e) {
				return;
			}
		}
		
		try {
			BulkMetadataDatastreamUIP singleUIP;
			while ((singleUIP = bulkUIP.getNextUpdate()) != null) {
				
				for (java.util.Map.Entry<String, Element> entry : singleUIP.getIncomingData().entrySet()) {
					pipeline.processUIP(singleUIP);
					
					// Check to see if the checksum of the new datastream matches the existing
					edu.unc.lib.dl.fedora.types.Datastream datastream
							= managementClient.getDatastream(singleUIP.getPID(), entry.getKey());
					
					// New datastream, create it
					if (datastream == null) {
						File contentFile = File.createTempFile("content", null);
						try {
							XMLOutputter xmlOutput = new XMLOutputter(Format.getRawFormat());
							try (FileOutputStream outStream = new FileOutputStream(contentFile)) {
								xmlOutput.output(entry.getValue(), outStream);
							}
							
							digitalObjectManager.addOrReplaceDatastream(singleUIP.getPID(),
									Datastream.getDatastream(entry.getKey()), contentFile, "text/xml",
									uip.getUser(), uip.getMessage());
						} finally {
							contentFile.delete();
						}
					} else {
						Format formatForChecksum = Format.getCompactFormat();
						formatForChecksum.setOmitDeclaration(false);
						XMLOutputter checksumOutputter = new XMLOutputter(formatForChecksum);
						
						String incomingChecksum = DigestUtils.md5Hex(
								checksumOutputter.outputString(entry.getValue().getDocument()).trim().replaceAll("\r\n", ""));
						if (!incomingChecksum.equals(datastream.getChecksum())) {
							XMLOutputter rawOutputter = new XMLOutputter(Format.getRawFormat());
							// or the checksums don't match, so update
							managementClient.modifyDatastream(singleUIP.getPID(), entry.getKey(), null,
									singleUIP.getLastModified(), rawOutputter.outputString(entry.getValue()).getBytes("UTF-8"));
							log.info("Updated {} for object {} during bulk update",
									new Object[] { entry.getKey(), singleUIP.getPID().getPid()});
						} else {
							log.debug("Skipping update of {} because the content has not changed.");
						}
					}

					// Store information about the last update completed so we can resume if interrupted
					updateResumptionPoint(bulkUIP.getPID(), singleUIP);
				}
			}
			// Finalize the update and clean up the trash
			cleanup(bulkUIP);
		} catch (FedoraException | IOException e) {
			throw new UpdateException("Failed to perform metadata update for user " + uip.getUser(), e);
		} finally {
			bulkUIP.close();
		}
	}
	
	private void updateResumptionPoint(PID uipPID, BulkMetadataDatastreamUIP singleUIP) {
		Jedis jedis = jedisPool.getResource();
		Map<String, String> values = new HashMap<>();
		values.put("lastPid", singleUIP.getPID().getPid());
		values.put("lastDatastream", singleUIP.getDatastream());
		jedis.hmset(RedisWorkerConstants.BULK_RESUME_PREFIX + uipPID.getPid(), values);
	}
	
	private void storeUpdateInformation(BulkMetadataUIP uip) {
		Jedis jedis = jedisPool.getResource();
		Map<String, String> values = new HashMap<>();
		values.put("email", uip.getEmailAddress());
		values.put("user", uip.getUser());
		values.put("groups", uip.getGroups().toString());
		values.put("filePath", uip.getImportFile().getAbsolutePath());
		jedis.hmset(RedisWorkerConstants.BULK_UPDATE_PREFIX + uip.getPID().getPid(), values);
	}
	
	/**
	 * Resumes an interrupted update using the last resumption point stored, moving the update cursor up to the point
	 * where the next getNextUpdate call will return the information for the next datastream after where the previous run
	 * left off.
	 * 
	 * @param uip
	 * @throws UpdateException
	 */
	private void resume(BulkMetadataUIP uip) throws UpdateException {
		Jedis jedis = jedisPool.getResource();
		Map<String, String> resumeValues = jedis.hgetAll(RedisWorkerConstants.BULK_RESUME_PREFIX + uip.getPID().getPid());
		if (resumeValues == null) {
			// No resumption info, so store update info just in case
			storeUpdateInformation(uip);
			return;
		}
		
		// If the update file doesn't exist anymore, clear this update out so it doesn't stick around forever
		if (!uip.getImportFile().exists()) {
			cleanup(uip);
			throw new UpdateException("Unable to resume update " + uip.getPID() + ", could not find update file");
		}
		
		// Move the update cursor past the last updated object
		uip.seekNextUpdate(new PID(resumeValues.get("lastPid")), resumeValues.get("lastDatastream"));
	}
	
	/**
	 * Cleans up resumption information and files related to the update
	 * @param uip
	 */
	private void cleanup(BulkMetadataUIP uip) {
		String pid = uip.getPID().getPid();
		
		Jedis jedis = jedisPool.getResource();
		jedis.del(RedisWorkerConstants.BULK_UPDATE_PREFIX + pid);
		jedis.del(RedisWorkerConstants.BULK_RESUME_PREFIX + pid);
		
		uip.getImportFile().delete();
	}

	public DigitalObjectManager getDigitalObjectManager() {
		return digitalObjectManager;
	}

	public void setDigitalObjectManager(DigitalObjectManager digitalObjectManager) {
		this.digitalObjectManager = digitalObjectManager;
	}

	public ManagementClient getManagementClient() {
		return managementClient;
	}

	public void setManagementClient(ManagementClient managementClient) {
		this.managementClient = managementClient;
	}
	
	public UIPUpdatePipeline getPipeline() {
		return pipeline;
	}

	public void setPipeline(UIPUpdatePipeline pipeline) {
		this.pipeline = pipeline;
	}

	public JedisPool getJedisPool() {
		return jedisPool;
	}

	public void setJedisPool(JedisPool jedisPool) {
		this.jedisPool = jedisPool;
	}
}
