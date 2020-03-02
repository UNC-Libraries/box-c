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
package edu.unc.lib.dl.cdr.services.processing;

/**
 * @author lfarrell
 */
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.ATOM_NS;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.CDR_MESSAGE_NS;

import edu.unc.lib.dl.model.DatastreamPids;
import io.dropwizard.metrics5.Timer;
import org.jdom2.Document;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;

import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.metrics.TimerFactory;
import edu.unc.lib.dl.search.solr.model.*;
import edu.unc.lib.dl.services.AbstractMessageSender;
import edu.unc.lib.dl.ui.service.SolrQueryLayerService;

public class RunEnhancementsService extends AbstractMessageSender {
    private static final Logger LOG = LoggerFactory.getLogger(RunEnhancementsService.class);
    private static final Timer timer = TimerFactory.createTimerForClass(RunEnhancementsService.class);

    private AccessControlService aclService;

    @Autowired
    protected SolrQueryLayerService queryLayer;
    private Datastream recordInfo;

    public void run(AgentPrincipals agent, ArrayList<HashMap> objectPids, Boolean force) {
        try (Timer.Context context = timer.time()) {
            for (HashMap objectPid : objectPids) {
                String uuid = (String) objectPid.get("pid");
                PID pid = PIDs.get(uuid);

                aclService.assertHasAccess("User does not have permission to run enhancements",
                        pid, agent.getPrincipals(), Permission.runEnhancements);

                LOG.debug("sending solr update message for {} of type {}", pid, "runEnhancements");

                String objectType = (String) objectPid.get("objectType");

                if (!objectType.equals("File")) {
                    SearchRequest searchRequest = new SearchRequest();
                    searchRequest.setAccessGroups(agent.getPrincipals());
                    searchRequest.setSearchState(new SearchState());
                    searchRequest.setRootPid(pid);
                    searchRequest.setApplyCutoffs(false);
                    SearchResultResponse resultResponse = queryLayer.performSearch(searchRequest);

                    for (BriefObjectMetadata metadata : resultResponse.getResultList()) {
                        createMessage(metadata, pid, agent.getUsername(), force);
                    }
                } else {
                    SimpleIdRequest searchRequest = new SimpleIdRequest(uuid, agent.getPrincipals());
                    BriefObjectMetadata metadata = queryLayer.getObjectById(searchRequest);
                    createMessage(metadata, pid, agent.getUsername(), force);
                }
            }
        } catch (Exception e) {
            LOG.warn(e.getMessage());
        }
    }

    public void setAclService(AccessControlService aclService) {
        this.aclService = aclService;
    }

    private void createMessage(BriefObjectMetadata metadata, PID pid, String username, Boolean force) {
        List<String> ids = metadata.getDatastream();

        for (String id : ids) {
            recordInfo = new Datastream(id);
            String filePath = DatastreamPids.getOriginalFilePid(pid).toString();

            if (recordInfo.getName().equals("original_file")) {
                Document msg = makeEnhancementOperationBody(username,
                        filePath, recordInfo.getMimetype(), force);
                sendMessage(msg);
            }
        }
    }

    private Document makeEnhancementOperationBody(String userid, String filePath, String mimeType, Boolean force) {
        Document msg = new Document();
        Element entry = new Element("entry", ATOM_NS);
        entry.addContent(new Element("author", ATOM_NS)
                .addContent(new Element("name", ATOM_NS).setText(userid)));
        entry.addContent(new Element("pid", ATOM_NS).setText(filePath));
        entry.addContent(new Element("mimeType", ATOM_NS).setText(mimeType));

        if (force) {
            Element paramForce = new Element("force", CDR_MESSAGE_NS);
            paramForce.setText("force");
        }
        msg.addContent(entry);

        return msg;
    }
}
