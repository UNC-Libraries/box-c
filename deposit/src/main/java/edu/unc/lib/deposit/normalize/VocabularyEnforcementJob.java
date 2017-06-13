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
package edu.unc.lib.deposit.normalize;

import static edu.unc.lib.deposit.work.DepositGraphUtils.walkChildrenDepthFirst;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaderSAX2Factory;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import edu.unc.lib.deposit.work.AbstractDepositJob;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;
import edu.unc.lib.dl.util.VocabularyHelperManager;
import edu.unc.lib.dl.xml.VocabularyHelper;

/**
 * Examines the descriptive metadata for this ingest and determines if it is valid according to the given vocabularies.
 * If a preferred authoritative version for a term is found, it will replace the original value.
 *
 * @author bbpennel
 * @date Jun 26, 2014
 */
public class VocabularyEnforcementJob extends AbstractDepositJob {
    private static final Logger log = LoggerFactory.getLogger(VocabularyEnforcementJob.class);

    @Autowired
    VocabularyHelperManager vocabManager;

    public VocabularyEnforcementJob() {
    }

    public VocabularyEnforcementJob(String uuid, String depositUUID) {
        super(uuid, depositUUID);
    }

    @Override
    public void runJob() {
        Model model = getWritableModel();

        // Get the list of all objects being ingested in this job
        List<String> resourcePIDs = new ArrayList<>();
        Bag deposit = model.getBag(getDepositPID().getURI());
        walkChildrenDepthFirst(deposit, resourcePIDs, true);

        SAXBuilder sb = new SAXBuilder(new XMLReaderSAX2Factory(false));

        // Vocabulary mappings need to be resolved against the destination since they are not in the hierarchy yet
        PID destinationPID = PIDs.get(getDepositStatus().get(DepositField.containerId.name()));

        for (String resourcePID : resourcePIDs) {
            PID pid = PIDs.get(resourcePID);
            File modsFile = new File(getDescriptionDir(), pid.getUUID() + ".xml");

            // Check if the resource has a description
            if (modsFile.exists()) {
                try {
                    Document modsDoc = sb.build(modsFile);

                    // Update the MODS document to use approved terms when possible
                    // if the vocabularies support remapping
                    log.debug("Updating document terms for {} within destination {}", pid, destinationPID);
                    boolean modified = updateDocumentTerms(destinationPID, modsDoc.getRootElement());

                    // Update the mods document if it was changed
                    if (modified) {
                        try (FileOutputStream fos = new FileOutputStream(modsFile)) {
                            new XMLOutputter(Format.getPrettyFormat()).output(modsDoc.getDocument(), fos);
                        }
                    }

                    // Capture any invalid affiliations as relations
                    log.debug("Adding invalid terms for {} within destination {}", pid, destinationPID);
                    addInvalidTerms(pid, destinationPID, modsDoc.getRootElement(), model);

                } catch (JDOMException | IOException e) {
                    log.error("Failed to parse description file {}", modsFile.getAbsolutePath(), e);
                }
            }
        }
    }

    private boolean updateDocumentTerms(PID pid, Element doc) throws JDOMException {
        boolean result = false;

        Set<VocabularyHelper> helpers = vocabManager.getRemappingHelpers(pid);
        if (helpers != null) {
            for (VocabularyHelper helper : helpers) {
                result = helper.updateDocumentTerms(doc) || result;
            }
        }

        return result;
    }

    /**
     * Adds invalid terms from applicable controlled vocabularies to the specified object as relationships.
     *
     * @param pid
     * @param destination
     * @param doc
     * @param model
     * @throws JDOMException
     */
    private void addInvalidTerms(PID pid, PID destination, Element doc, Model model) throws JDOMException {
        Map<String, Set<String>> invalidTermMap = vocabManager.getInvalidTermsWithPrefix(destination, doc);
        if (invalidTermMap == null) {
            return;
        }

        for (Entry<String, Set<String>> invalidTermEntry : invalidTermMap.entrySet()) {
            Set<String> invalidTerms = invalidTermEntry.getValue();

            // Persist the list of invalid vocab terms out to the model
            if (invalidTerms.size() > 0) {
                Resource resource = model.getResource(pid.getURI());
                for (String invalid : invalidTerms) {
                    model.addLiteral(resource, Cdr.invalidTerm, model.createLiteral(invalid));
                }
            }
        }
    }

}
