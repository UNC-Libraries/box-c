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

import org.apache.commons.codec.digest.DigestUtils;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.JedisPool;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.ManagementClient;
import edu.unc.lib.dl.services.DigitalObjectManager;
import edu.unc.lib.dl.util.ContentModelHelper.Datastream;

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

	@Override
	public void process(UpdateInformationPackage uip) throws UpdateException, UIPException {
		if (!(uip instanceof BulkMetadataUIP)) {
			throw new UIPException("Incorrect UIP class, found " + uip.getClass().getName() + ", expected "
					+ BulkMetadataUIP.class.getName());
		}
		
		BulkMetadataUIP bulkUIP = (BulkMetadataUIP) uip;
		
		try {
			BulkMetadataPartUIP singleUIP;
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
				}
			}
		} catch (FedoraException | IOException e) {
			throw new UpdateException("Failed to perform metadata update for user " + uip.getUser(), e);
		} finally {
			bulkUIP.close();
		}
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

	private JedisPool jedisPool;
	public JedisPool getJedisPool() {
		return jedisPool;
	}

	public void setJedisPool(JedisPool jedisPool) {
		this.jedisPool = jedisPool;
	}
}
