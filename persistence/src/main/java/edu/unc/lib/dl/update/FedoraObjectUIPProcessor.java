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
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import edu.unc.lib.dl.services.OperationsMessageSender;
import edu.unc.lib.dl.util.ContentModelHelper.Datastream;
import edu.unc.lib.dl.util.IndexingActionType;

/**
 *
 * @author bbpennel
 *
 */
public class FedoraObjectUIPProcessor implements UIPProcessor {
    private static Logger log = Logger.getLogger(FedoraObjectUIPProcessor.class);

//    private DigitalObjectManager digitalObjectManager;
    private UIPUpdatePipeline pipeline;
//    private AccessClient accessClient;
    private OperationsMessageSender operationsMessageSender;
//    private AccessControlService aclService;

    private Map<String, Datastream> virtualDatastreamMap;

    public FedoraObjectUIPProcessor() {
    }

    @Override
    public void process(UpdateInformationPackage uip) throws UpdateException, UIPException {
        if (!(uip instanceof FedoraObjectUIP)) {
            throw new UIPException("Incorrect UIP class, found " + uip.getClass().getName() + ", expected "
                    + FedoraObjectUIP.class.getName());
        }
        log.debug("Preparing to process Fedora Object UIP for operation " + uip.getOperation() + " on "
                + uip.getPID().getPid());

        FedoraObjectUIP fuip = (FedoraObjectUIP) uip;

//        if (fuip.getIncomingData().containsKey("ACL")
//                && !aclService.hasAccess(uip.getPID(), GroupsThreadStore.getGroups(), Permission.editAccessControl)) {
//            throw new UpdateException("Insufficient privileges to update access controls for " + uip.getPID());
//        }
//
//        fuip.storeOriginalDatastreams(accessClient);

        uip = pipeline.processUIP(uip);
        Map<String, File> modifiedFiles = uip.getModifiedFiles();
        if (modifiedFiles != null) {
            Datastream targetedDatastream = this.getTargetedDatastream(fuip);
            // If no datastream was targeted then updating all modified datastreams
            if (targetedDatastream == null) {
                for (Entry<String, File> modifiedFile : modifiedFiles.entrySet()) {
                    Datastream datastream = Datastream.getDatastream(modifiedFile.getKey());
                    if (datastream != null && modifiedFile.getValue() != null) {
                        log.debug("Adding/replacing datastream " + datastream.getName() + " on " + uip.getPID());
//                        digitalObjectManager.addOrReplaceDatastream(uip.getPID(), datastream, modifiedFile.getValue(),
//                                uip.getMimetype(modifiedFile.getKey()), uip.getUser(), uip.getMessage());
                    }
                }
            } else {
                log.debug("Adding/replacing targeted " + targetedDatastream.getName()
                        + " with " + uip.getModifiedFiles());
                log.debug("Specifically with file: " + modifiedFiles.get(targetedDatastream.getName()));
                // Datastream was specifically targeted, so only perform updates to it
                // The reasoning for filtering it down at this step is that other datastreams may have been involved in
                // early steps to compute the new value for the targeted datastream, but we don't want to commit those
                // changes
//                digitalObjectManager.addOrReplaceDatastream(uip.getPID(), targetedDatastream,
//                        modifiedFiles.get(targetedDatastream.getName()),
//                        uip.getMimetype(targetedDatastream.getName()),
//                        uip.getUser(), uip.getMessage());
            }

            // Issue indexing operations based on the data updated
//            Collection<IndexingActionType> indexingActions = getIndexingActions(fuip);
//            if (indexingActions != null) {
//                for (IndexingActionType actionType : indexingActions) {
//                    operationsMessageSender.sendIndexingOperation(uip.getUser(),
//                            Arrays.asList(uip.getPID()), actionType);
//                }
//            }
        }
    }

    private Collection<IndexingActionType> getIndexingActions(FedoraObjectUIP fuip) {
        if (fuip.getModifiedData().size() == 0) {
            return null;
        }
        Collection<IndexingActionType> actionTypes = new HashSet<>(fuip.getModifiedData().size());
        // Only detecting ACL changes this way for now as it would otherwise be
        // unidentifiable from other RELS_EXT updates
        if (fuip.getIncomingData().containsKey("ACL")
                && fuip.getModifiedData().containsKey(Datastream.RELS_EXT.getName())) {
            actionTypes.add(IndexingActionType.UPDATE_ACCESS);
        }
        return actionTypes;
    }

    private Datastream getTargetedDatastream(FedoraObjectUIP fuip) {
        String pid = fuip.getPID().getPid();
        int index = pid.indexOf('/');
        if (index == -1 || index == pid.length() - 1) {
            return null;
        }
        String datastreamName = pid.substring(index + 1);
        Datastream datastream = Datastream.getDatastream(datastreamName);
        if (datastream != null) {
            return datastream;
        }
        return this.virtualDatastreamMap.get(datastreamName);
    }

    public UIPUpdatePipeline getPipeline() {
        return pipeline;
    }

    public void setPipeline(UIPUpdatePipeline pipeline) {
        this.pipeline = pipeline;
    }

    public void setVirtualDatastreamMap(Map<String, Datastream> virtualDatastreamMap) {
        this.virtualDatastreamMap = virtualDatastreamMap;
    }

    public void setOperationsMessageSender(OperationsMessageSender operationsMessageSender) {
        this.operationsMessageSender = operationsMessageSender;
    }
}