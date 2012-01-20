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

import java.io.File;
import java.util.Collection;
import java.util.List;

import edu.unc.lib.dl.agents.Agent;
import edu.unc.lib.dl.agents.PersonAgent;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.NotFoundException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.ingest.IngestException;
import edu.unc.lib.dl.ingest.sip.SubmissionInformationPackage;
import edu.unc.lib.dl.util.ContainerPlacement;
import edu.unc.lib.dl.util.ContentModelHelper;

public interface DigitalObjectManager {

	/**
	 * Creates repository objects in a single transaction with appropriate additions to preservation logs. This method
	 * will also upload any local files referenced in the IngestContext. This method will report failure if any paths
	 * conflict with existing objects. This method will ingest files and objects in batches appropriate to the underlying
	 * architecture. The entire method is implemented as a single transaction and will send an email to specified
	 * recipients. This method also updates the parent folder object in the repository.
	 *
	 * @param containerPath
	 *           a path to the folder which will hold this object
	 * @param sip
	 *           a zip file containing content, metadata and METS manifest.
	 * @param owner
	 *           an agent object, usually a group, that will own this object
	 * @param user
	 *           an agent object representing the user performing ingest
	 * @param message
	 *           a log message for this ingest action
	 * @return a log of ingest events
	 */
	public abstract IngestResult addBatch(SubmissionInformationPackage sip, Agent user, String message) throws IngestException;

	/**
	 * Adds a relationship between two repository objects.
	 *
	 * @param subject
	 *           the subject PID
	 * @param rel
	 *           the relationship (enum)
	 * @param object
	 *           the object PID
	 * @throws NotFoundException
	 *            if either object is not found
	 * @throws IngestException
	 */
	public void addRelationship(PID subject, ContentModelHelper.Relationship rel, PID object) throws NotFoundException,
			IngestException;

	/**
	 * Completely removes the specified objects and any children. Updates containing objects to remove any references.
	 *
	 * @param pids
	 *           the PIDs of the objects to purge
	 * @param message
	 *           log message
	 * @return the list of deleted PIDs, including children
	 */
	public abstract List<PID> delete(PID pid, Agent user, String message) throws IngestException, NotFoundException;

	/**
	 * Inactivates (remove without purge) the specified object and updates any ancillary services. If a container is
	 * specified, then all its parts will also be made inactive.
	 *
	 * @param pids
	 *           the PIDs of the objects to inactive
	 */
	// public abstract void inactivate(PID id, Agent user, String message) throws IngestException;

	/**
	 * Removes a relationship between two repository objects
	 *
	 * @param subject
	 *           the subject PID
	 * @param rel
	 *           the relationship enum
	 * @param object
	 *           the object PID
	 * @throws NotFoundException
	 *            if either objects or their relationship are not found
	 * @throws IngestException
	 */
	public void purgeRelationship(PID subject, ContentModelHelper.Relationship rel, PID object)
			throws NotFoundException, IngestException;

	/**
	 * Updates the specified source datastream on an object with appropriate additions to preservation logs. Note that
	 * this method will only update user supplied datastreams. For updates to MD_DESCRIPTIVE, see updateDescription.
	 *
	 * @param pid
	 *           PID of the object to update
	 * @param datastreamName
	 *           name of the datastream to update
	 * @param user
	 *           agent object representing the user
	 * @param message
	 *           log message explaining this update
	 */
	public abstract String updateSourceData(PID object, String datastreamName, File newDataFile, String checksum,
			String label, String mimetype, Agent user, String message) throws IngestException;

	/**
	 * Updates the descriptive metadata for an object. The supplied file must be valid MODS XML and conform with
	 * additional CDR MODS requirements.
	 *
	 * @param pid
	 *           PID of the object to update
	 * @param newMODSFile
	 *           MODS XML file
	 * @param user
	 *           agent performing this action
	 * @param message
	 *           log message explaining this action
	 * @throws IngestException
	 */
	public abstract String updateDescription(PID pid, File newMODSFile, String checksum, Agent user, String message)
			throws IngestException;

	/**
	 * Moves the specified objects from their current containers to another existing container. The destination path must
	 * correspond to a object having the Container model. Note that the List may include PIDs from a variety of different
	 * containers. The items in the List are inserted among children in the new parent according to their index within
	 * the List, unless some other sort is specified on the parent.
	 *
	 * @param movingPids
	 *           a List of PIDs to move, in order for insert
	 * @param destinationPath
	 *           the repository path of the new parent container
	 * @param user
	 *           agent performing this action
	 * @param message
	 *           log message explaining this action
	 * @throws IngestException
	 */
	public abstract void move(List<PID> movingPids, String destinationPath, Agent user, String message)
			throws IngestException;

	public abstract boolean isAvailable();

	/**
	 * Adds a single object to the repository, without waiting in the queue. This method does not send email.
	 *
	 * @param sip
	 *           a SingleFileSIP, SingleFolderSIP, MultiFileObjectSIP or AgentSIP
	 * @param user
	 *           the submitter
	 * @param message
	 *           the ingest message
	 * @return the PID of the object added
	 */
	public abstract PID addSingleObject(SubmissionInformationPackage sip, Agent user, String message)
			throws IngestException;

}