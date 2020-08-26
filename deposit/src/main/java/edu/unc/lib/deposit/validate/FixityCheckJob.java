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
package edu.unc.lib.deposit.validate;

import static edu.unc.lib.dl.rdf.CdrDeposit.stagingLocation;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.deposit.work.AbstractDepositJob;
import edu.unc.lib.dl.event.PremisEventBuilder;
import edu.unc.lib.dl.event.PremisLogger;
import edu.unc.lib.dl.exceptions.InvalidChecksumException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.model.AgentPids;
import edu.unc.lib.dl.rdf.Premis;
import edu.unc.lib.dl.util.DigestAlgorithm;
import edu.unc.lib.dl.util.MultiDigestInputStreamWrapper;
import edu.unc.lib.dl.util.SoftwareAgentConstants.SoftwareAgent;

/**
 * Calculates digests for staged files, performing a fixity check if existing
 * digests were provided with the deposit.
 *
 * @author bbpennel
 */
public class FixityCheckJob extends AbstractDepositJob {
    private static final Logger log = LoggerFactory.getLogger(FixityCheckJob.class);

    private static final Collection<DigestAlgorithm> REQUIRED_ALGS = Collections.singleton(
            DigestAlgorithm.DEFAULT_ALGORITHM);

    public FixityCheckJob(String uuid, String depositUUID) {
        super(uuid, depositUUID);
        this.rollbackDatasetOnFailure = false;
    }

    @Override
    public void runJob() {
        log.debug("Performing FixityCheckJob for deposit {}", depositUUID);
        Model model = getWritableModel();

        List<Entry<PID, String>> stagingList = getPropertyPairList(model, stagingLocation);
        for (Entry<PID, String> stagingEntry : stagingList) {
            PID rescPid = stagingEntry.getKey();
            // Skip already checked files
            if (isObjectCompleted(rescPid)) {
                continue;
            }

            interruptJobIfStopped();

            String stagedPath = stagingEntry.getValue();
            URI stagedUri = URI.create(stagedPath);

            Resource objResc = model.getResource(rescPid.getRepositoryPath());

            try (InputStream fStream = Files.newInputStream(Paths.get(stagedUri))) {
                log.debug("Calculating digests for {}", stagedUri);
                MultiDigestInputStreamWrapper digestWrapper = new MultiDigestInputStreamWrapper(
                        fStream, getDigestsForResource(objResc), REQUIRED_ALGS);
                digestWrapper.checkFixity();
                log.debug("Verified fixity of {}", stagedUri);
                recordDigestsForResource(rescPid, objResc, digestWrapper.getDigests());
                markObjectCompleted(rescPid);
                log.debug("Completed fixity recording for {}", stagedUri);
            } catch (InvalidChecksumException e) {
                failJob(String.format("Fixity check failed for %s belonging to %s",
                        stagedUri, objResc.getURI()),
                        e.getMessage());
            } catch (IOException e) {
                failJob(e, "Failed to read file {0} for fixity check", stagedUri);
            }
        }
    }

    private Map<DigestAlgorithm, String> getDigestsForResource(Resource resc) {
        Map<DigestAlgorithm, String> digests = new HashMap<>();
        for (DigestAlgorithm algorithm : DigestAlgorithm.values()) {
            if (resc.hasProperty(algorithm.getDepositProperty())) {
                digests.put(algorithm, resc.getProperty(algorithm.getDepositProperty()).getString());
            }
        }
        return digests;
    }

    private void recordDigestsForResource(PID pid, Resource resc, Map<DigestAlgorithm, String> digests) {
        List<String> details = new ArrayList<>();
        // Store newly calculate digests into deposit model
        digests.forEach((alg, digest) -> {
            if (resc.hasProperty(alg.getDepositProperty())) {
                log.debug("{} fixity check for {} passed with value {}", alg.getName(), resc.getURI(), digest);
            } else {
                log.debug("Storing {} digest for {} with value {}", alg.getName(), resc.getURI(), digest);
                resc.addLiteral(alg.getDepositProperty(), digest);
            }
            details.add(alg.getName().toUpperCase() + " checksum calculated: " + digest);
        });

        // Store event for calculation of checksums
        PremisLogger premisDepositLogger = getPremisLogger(pid);
        PremisEventBuilder builder = premisDepositLogger.buildEvent(Premis.MessageDigestCalculation)
                .addSoftwareAgent(AgentPids.forSoftware(SoftwareAgent.depositService));
        details.forEach(builder::addEventDetail);
        builder.write();
    }
}
