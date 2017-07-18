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
package edu.unc.lib.dl.cdr.services.solr;

import static edu.unc.lib.dl.util.ContentModelHelper.CDRProperty.embargoUntil;
import static edu.unc.lib.dl.util.ContentModelHelper.Datastream.RELS_EXT;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.fedora.DatastreamDocument;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.ManagementClient;
import edu.unc.lib.dl.fedora.OptimisticLockException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.services.OperationsMessageSender;
import edu.unc.lib.dl.util.IndexingActionType;
import edu.unc.lib.dl.util.PremisEventLogger;
import edu.unc.lib.dl.util.TripleStoreQueryService;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

/**
 * Service which clears all expired embargoes and triggers reindexing of impacted objects
 * 
 * @author bbpennel
 */
public class EmbargoUpdateService {
    private static final Logger LOG = LoggerFactory.getLogger(EmbargoUpdateService.class);

    private TripleStoreQueryService tripleStoreQueryService;
    private ManagementClient managementClient;
    private OperationsMessageSender messageSender;
    private static final String EMBARGO_USER = "embargo-update-service";

    private final String staleEmbargoQuery;

    public EmbargoUpdateService() throws IOException {
        staleEmbargoQuery = IOUtils.toString(
                this.getClass().getResourceAsStream("embargo-update-candidates.sparql"), "UTF-8");
    }

    public void updateEmbargoes() {
        List<PID> candidates = this.findCandidateObjects();
        LOG.info("Clearing {} expired embargoes", candidates.size());

        if (candidates != null && candidates.size() > 0) {
            // Remove expired embargoes
            for (PID pid : candidates) {
                removeEmbargo(pid);
            }

            // Trigger reindexing of newly unembargoed objects
            messageSender.sendIndexingOperation(EMBARGO_USER,
                    candidates, IndexingActionType.UPDATE_ACCESS);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private List<PID> findCandidateObjects() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        String windowEnd = formatter.format(calendar.getTime());

        // replace model URI and date tokens
        String query = String.format(staleEmbargoQuery, tripleStoreQueryService.getResourceIndexModelUri(),
                windowEnd);

        List<PID> expiringEmbargoes = new ArrayList<>();
        List<Map> bindings = (List<Map>) ((Map) tripleStoreQueryService.sendSPARQL(query).get("results"))
                .get("bindings");
        for (Map binding : bindings) {
            expiringEmbargoes.add(new PID((String) ((Map) binding.get("pid")).get("value")));
        }

        return expiringEmbargoes;
    }

    private void removeEmbargo(PID pid) {
        while (true) {
            try {
                DatastreamDocument ds = managementClient.getXMLDatastreamIfExists(pid, RELS_EXT.getName());
                Element descEl = ds.getDocument().getRootElement().getChild("Description", JDOMNamespaceUtil.RDF_NS);
                String embargo = descEl.getChildTextTrim(embargoUntil.getPredicate(), embargoUntil.getNamespace());
                descEl.removeChildren(embargoUntil.getPredicate(), embargoUntil.getNamespace());

                managementClient.modifyDatastream(pid, RELS_EXT.getName(), "Clearing expired embargo",
                        ds.getLastModified(), ds.getDocument());

                // Record an event indicating that an embargo expired for this object
                PremisEventLogger logger = new PremisEventLogger(EMBARGO_USER);
                Element event = logger.logEvent(PremisEventLogger.Type.MIGRATION,
                        "Embargo expiration", pid);
                PremisEventLogger.addDetailedOutcome(event, "success",
                        "Expired an embargo which ended " + embargo, null);
                managementClient.writePremisEventsToFedoraObject(logger, pid);
                return;
            } catch (OptimisticLockException e) {
                LOG.debug("Failed to get optimistic lock on {}, retrying", pid);
            } catch (FedoraException e) {
                LOG.error("Failed to clear embargo on {}", pid, e);
                return;
            }
        }
    }

    public void setTripleStoreQueryService(TripleStoreQueryService tripleStoreQueryService) {
        this.tripleStoreQueryService = tripleStoreQueryService;
    }

    public void setManagementClient(ManagementClient managementClient) {
        this.managementClient = managementClient;
    }

    public void setMessageSender(OperationsMessageSender messageSender) {
        this.messageSender = messageSender;
    }
}
