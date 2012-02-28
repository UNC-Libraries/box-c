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
package edu.unc.lib.dl.ingest.aip;

import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.Set;

import org.jdom.Document;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.ingest.IngestException;
import edu.unc.lib.dl.util.ContainerPlacement;
import edu.unc.lib.dl.util.PremisEventLogger;

public interface ArchivalInformationPackage {
	public static final String DATA_SUBDIR = "data";

	public void delete();

	public PremisEventLogger getEventLogger();

	public File getFileForUrl(String path);

	public Document getFOXMLDocument(PID pid);

	public File getFOXMLFile(PID pid);

	public Set<PID> getPIDs();

	public File getTempFOXDir();

	public Set<PID> getTopPIDs();

	public void saveFOXMLDocument(PID pid, Document doc);

	public void setTopPIDs(Set<PID> topPIDs);

	public void setSendEmail(boolean sendEmail);

	public boolean getSendEmail();

	public void setEmailRecipients(List<URI> recipients);

	public List<URI> getEmailRecipients();

	/**
	 * @param parentPath
	 * @param topPID
	 * @param designatedOrder
	 * @param sipOrder
	 */
	public void setContainerPlacement(PID parentPID, PID topPID, Integer designatedOrder, Integer sipOrder, String label);

	/**
	 * Gets the ContainerPlacement object for the specified pid, representing its ordered placement within its parent
	 * container.
	 * 
	 * @param pid
	 * @return
	 */
	public ContainerPlacement getContainerPlacement(PID pid);

	/**
	 * @param message
	 * @param submitter
	 * @throws IngestException
	 */
	void prepareIngest(String message, String submitter) throws IngestException;

	/**
	 * Gets the ID of the deposit.
	 * 
	 * @return
	 */
	public PID getDepositID();

	/**
	 * Sets the ID of the deposit.
	 * 
	 * @param pid
	 */
	public void setDepositID(PID pid);

}
