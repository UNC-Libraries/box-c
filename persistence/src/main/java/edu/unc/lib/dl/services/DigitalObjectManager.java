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
import java.util.List;

import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.NotFoundException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.ingest.IngestException;
import edu.unc.lib.dl.update.UpdateException;
import edu.unc.lib.dl.util.ContentModelHelper.Datastream;
import edu.unc.lib.dl.util.ContentModelHelper.Model;
import edu.unc.lib.dl.util.ResourceType;

/**
 * 
 * @author count0
 *
 */
public interface DigitalObjectManager {

    /**
     * Changes the content models of the subject to the content models necessary
     * to change to the new resource type
     *
     * @param subject
     * @param newType
     * @throws IngestException
     */
    public void editResourceType(List<PID> subjects, ResourceType newType, String user) throws UpdateException;

    /**
     * Completely removes the specified objects and any children. Updates
     * containing objects to remove any references.
     *
     * @param pids
     *            the PIDs of the objects to purge
     * @param message
     *            log message
     * @return the list of deleted PIDs, including children
     */
    public abstract List<PID> delete(PID pid, String user, String message) throws IngestException, NotFoundException;

    /**
     * Inactivates (remove without purge) the specified object and updates any
     * ancillary services. If a container is specified, then all its parts will
     * also be made inactive.
     *
     * @param pids
     *            the PIDs of the objects to inactive
     */
    // public abstract void inactivate(PID id, Agent user, String message)
    // throws IngestException;

    /**
     * Updates the specified source datastream on an object with appropriate
     * additions to preservation logs. Note that this method will only update
     * user supplied datastreams. For updates to MD_DESCRIPTIVE, see
     * updateDescription.
     *
     * @param pid
     *            PID of the object to update
     * @param datastreamName
     *            name of the datastream to update
     * @param user
     *            agent object representing the user
     * @param message
     *            log message explaining this update
     */
    public abstract String updateSourceData(PID object, String datastreamName, File newDataFile, String checksum,
            String label, String mimetype, String user, String message) throws IngestException;

    /**
     * Updates the descriptive metadata for an object. The supplied file must be
     * valid MODS XML and conform with additional CDR MODS requirements.
     *
     * @param pid
     *            PID of the object to update
     * @param newMODSFile
     *            MODS XML file
     * @param user
     *            agent performing this action
     * @param message
     *            log message explaining this action
     * @throws IngestException
     */
    public abstract String updateDescription(PID pid, File newMODSFile, String checksum, String user, String message)
            throws IngestException;

    /**
     * Adds or replaces the contents of the specified datastream with the file
     * content.
     * 
     * @param pid
     * @param datastream
     * @param content
     * @param mimetype
     * @param user
     * @param message
     * @throws UpdateException
     */
    public abstract String addOrReplaceDatastream(PID pid, Datastream datastream, File content, String mimetype,
            String user, String message) throws UpdateException;

    public abstract String addOrReplaceDatastream(PID pid, Datastream datastream, String label, File content,
            String mimetype, String user, String message) throws UpdateException;

    /**
     * Moves the specified objects from their current containers to another
     * existing container. The destination path must correspond to a object
     * having the Container model. Note that the List may include PIDs from a
     * variety of different containers. The items in the List are inserted among
     * children in the new parent according to their index within the List,
     * unless some other sort is specified on the parent.
     *
     * @param movingPids
     *            a List of PIDs to move, in order for insert
     * @param destination
     *            the PID of the destination for this action
     * @param user
     *            agent performing this action
     * @param message
     *            log message explaining this action
     * @throws IngestException
     */
    public abstract void move(List<PID> moving, PID destination, String user, String message) throws IngestException;

    /**
     * Adds children objects to the specified container.
     *
     * @param container
     * @param children
     * @throws FedoraException
     * @throws IngestException
     */
    public void addChildrenToContainer(PID container, List<PID> children) throws FedoraException, IngestException;

    /**
     * Attempts to rollback a failed move operation by returning part way moved
     * objects to their original source container and cleaning up removal
     * markers
     *
     * @param source
     * @param moving
     * @throws IngestException
     */
    public void rollbackMove(PID source, List<PID> moving) throws IngestException;

    public abstract boolean isAvailable();

    /**
     * Adds a container to the specified parent container.
     * 
     * @param name
     *            container name
     * @param parent
     *            parent pid
     * @param extraModel
     *            an additional Model beyond Container or null
     * @param user
     *            depositor username
     * @param mods
     *            optional MODS XML as byte array
     * @return PID of the new container
     */
    public abstract PID createContainer(String name, PID parent, Model extraModel, String user, byte[] mods)
            throws IngestException;

    /**
     * Sets or clears the default web object for the aggregate objects
     * containing the given pids
     *
     * @param dwos
     * @param user
     * @throws UpdateException
     */
    void editDefaultWebObject(List<PID> dwo, boolean clear, String user) throws UpdateException;

}