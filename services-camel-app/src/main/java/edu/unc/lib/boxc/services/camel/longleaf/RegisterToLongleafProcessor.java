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
package edu.unc.lib.boxc.services.camel.longleaf;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.substringAfterLast;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.ProducerTemplate;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;
import org.fcrepo.client.FedoraHeaderConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.common.metrics.HistogramFactory;
import edu.unc.lib.boxc.common.metrics.TimerFactory;
import edu.unc.lib.boxc.fcrepo.exceptions.ServiceException;
import edu.unc.lib.boxc.fcrepo.utils.ClientFaultResolver;
import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.model.api.exceptions.ObjectTypeMismatchException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.persist.impl.transfer.FileSystemTransferHelpers;
import io.dropwizard.metrics5.Histogram;
import io.dropwizard.metrics5.Timer;

/**
 * Processor which registers binaries in longleaf
 *
 * @author bbpennel
 * @author smithjp
 */
public class RegisterToLongleafProcessor extends AbstractLongleafProcessor {
    private static final Logger log = LoggerFactory.getLogger(RegisterToLongleafProcessor.class);

    private static final Histogram batchSizeHistogram = HistogramFactory
            .createHistogram("longleafRegisterBatchSize");
    private static final Timer timer = TimerFactory.createTimerForClass(RegisterToLongleafProcessor.class);

    public static final List<String> REGISTERABLE_IDS = asList(
            DatastreamType.MD_DESCRIPTIVE.getId(),
            DatastreamType.MD_DESCRIPTIVE_HISTORY.getId(),
            DatastreamType.MD_EVENTS.getId(),
            DatastreamType.ORIGINAL_FILE.getId(),
            DatastreamType.TECHNICAL_METADATA.getId()
        );

    private RepositoryObjectLoader repoObjLoader;

    private FcrepoClient fcrepoClient;

    private String registrationSuccessfulEndpoint;

    /**
     * The exchange here is expected to be a batch message containing a List
     * of binary PIDs for registration, in string form.
     */
    @SuppressWarnings("unchecked")
    @Override
    public void process(Exchange exchange) throws Exception {
        Message aggrMsg = exchange.getIn();

        // Key is the digest key, value is a map of storage uri to digest value
        Map<String, List<DigestEntry>> digestsMap = new HashMap<>();
        List<DigestEntry> md5Entries = new ArrayList<>();
        List<DigestEntry> sha1Entries = new ArrayList<>();
        digestsMap.put("md5", md5Entries);
        digestsMap.put("sha1", sha1Entries);

        List<String> messages = aggrMsg.getBody(List.class);
        for (String fcrepoUri : messages) {
            try {
                PID pid = PIDs.get(fcrepoUri);
                BinaryObject binObj = repoObjLoader.getBinaryObject(pid);

                String md5 = trimFedoraDigest(binObj.getMd5Checksum(), ":");
                String sha1 = trimFedoraDigest(binObj.getSha1Checksum(), ":");
                URI storageUri = binObj.getContentUri();

                if (storageUri == null) {
                    log.error("Unable to register {}, it did not have a content URI.");
                    continue;
                }

                if (md5 != null) {
                    md5Entries.add(new DigestEntry(fcrepoUri, storageUri, md5));
                }
                if (sha1 != null) {
                    sha1Entries.add(new DigestEntry(fcrepoUri, storageUri, sha1));
                }
                if (md5 == null && sha1 == null) {
                    sha1Entries.add(new DigestEntry(fcrepoUri, storageUri, calculateSha1(pid)));
                }
            } catch (NotFoundException | ObjectTypeMismatchException e) {
                log.error("Cannot register {}: {}", fcrepoUri, e.getMessage());
            }
        }

        try (Timer.Context context = timer.time()) {
            registerFiles(messages, digestsMap, exchange);
        }
    }

    private String calculateSha1(PID pid) {
        try (FcrepoResponse response = fcrepoClient.head(pid.getRepositoryUri())
                .addHeader(FedoraHeaderConstants.WANT_DIGEST, "sha")
                .perform()) {
            String digestVal = response.getHeaderValue(FedoraHeaderConstants.DIGEST);
            if (digestVal == null) {
                throw new ServiceException("Failed to calculate sha1 for " + pid.getRepositoryPath());
            } else {
                return trimFedoraDigest(digestVal, "=");
            }
        } catch (IOException e) {
            throw new ServiceException(e);
        } catch (FcrepoOperationFailedException e) {
            ClientFaultResolver.resolve(e);
            return null;
        }
    }

    /**
     * Executes longleaf register command for a batch of files with digests
     *
     * @param messages list of fcrepoUris from the exchange
     * @param digestsMap mapping of digest algorithms to paths plus digest values
     * @param exchange the current camel exchange
     */
    private void registerFiles(List<String> messages, Map<String, List<DigestEntry>> digestsMap, Exchange exchange) {
        long start = System.currentTimeMillis();

        StringBuilder sb = new StringBuilder();
        MutableInt cnt = new MutableInt(0);
        digestsMap.entrySet().forEach(digestGroup -> {
            List<DigestEntry> digestEntries = digestGroup.getValue();
            if (digestEntries.size() == 0) {
                return;
            }
            sb.append(digestGroup.getKey()).append(":\n");

            digestEntries.forEach(manifestEntry -> {
                Path filePath = Paths.get(manifestEntry.storageUri);
                String basePath = FileSystemTransferHelpers.getBaseBinaryPath(filePath);
                sb.append(manifestEntry.digest)
                    .append(' ')
                    .append(basePath)
                    .append(' ')
                    .append(filePath)
                    .append('\n');
                cnt.increment();
            });
        });

        int entryCount = cnt.getValue();
        // Nothing to register
        if (entryCount == 0) {
            return;
        }

        // Record statistics about the number of objects registered
        batchSizeHistogram.update(entryCount);

        ExecuteResult result = executeCommand("register --force -m @-", sb.toString());

        Map<String, String> successful = new HashMap<>();
        if (result.exitVal == 0) {
            log.info("Successfully registered {} entries to longleaf", entryCount);

            digestsMap.values().stream().flatMap(values -> values.stream())
                      .forEach(d -> successful.put(d.fcrepoUri, d.storageUri.toString()));
            sendSuccessMessage(successful, exchange);
        } else {
            // trim successfully registered files out of the message before failing
            if (!result.completed.isEmpty()) {
                result.completed.stream()
                    .map(completedPath -> findDigestEntry(completedPath, digestsMap))
                    .filter(digestEntry -> digestEntry != null)
                    .forEach(digestEntry -> {
                        messages.remove(digestEntry.fcrepoUri);
                        successful.put(digestEntry.fcrepoUri, digestEntry.storageUri.toString());
                    });

                sendSuccessMessage(successful, exchange);
            }
            if (messages.isEmpty()) {
                log.error("Result from longleaf indicates registration failed, but there are "
                        + "no failed URIs remaining. See longleaf logs for details.");
                return;
            }
            throw new ServiceException("Failed to register " + entryCount + " entries to Longleaf.  "
                    + "Check longleaf logs, command returned: " + result.exitVal);
        }

        log.info("Longleaf registration completed in: {} ms", (System.currentTimeMillis() - start));
    }

    private void sendSuccessMessage(Map<String, String> successful, Exchange exchange) {
        if (successful.size() == 0) {
            return;
        }
        ProducerTemplate template = exchange.getContext().createProducerTemplate();
        template.sendBody(registrationSuccessfulEndpoint, successful);
    }

    private DigestEntry findDigestEntry(String seekPath, Map<String, List<DigestEntry>> digestsMap) {
        URI seekUri = Paths.get(seekPath).toUri();
        Optional<DigestEntry> entry = digestsMap.values().stream().flatMap(List::stream)
            .filter(de -> de.storageUri.equals(seekUri))
            .findFirst();
        if (entry.isPresent()) {
            return entry.get();
        } else {
            return null;
        }
    }

    private static class DigestEntry {
        protected String fcrepoUri;
        protected URI storageUri;
        protected String digest;

        public DigestEntry(String fcrepoUri, URI storageUri, String digest) {
            this.fcrepoUri = fcrepoUri;
            this.storageUri = storageUri;
            this.digest = digest;
        }
    }

    private String trimFedoraDigest(String fedoraDigest, String separator) {
        if (fedoraDigest == null) {
            return null;
        }
        return substringAfterLast(fedoraDigest, separator);
    }

    public void setRepositoryObjectLoader(RepositoryObjectLoader repoObjLoader) {
        this.repoObjLoader = repoObjLoader;
    }

    public void setFcrepoClient(FcrepoClient fcrepoClient) {
        this.fcrepoClient = fcrepoClient;
    }

    public void setRegistrationSuccessfulEndpoint(String registrationSuccessfulEndpoint) {
        this.registrationSuccessfulEndpoint = registrationSuccessfulEndpoint;
    }

    /**
     * @param fcrepuUri uri of object
     * @return true if the object is a binary that should be registered in longleaf.
     */
    public static boolean registerableBinary(Exchange exchange) {
        String fcrepoUri = (String) exchange.getIn().getHeader(FCREPO_URI);
        String dsId = StringUtils.substringAfterLast(fcrepoUri, "/");

        if (dsId.equals("fcr:metadata")) {
            return false;
        }

        if (REGISTERABLE_IDS.contains(dsId)) {
            return true;
        } else {
            // Also registerable if the datastream is a deposit manifest
            PID dsPid = PIDs.get(fcrepoUri);
            return dsPid.getQualifier().equals(RepositoryPathConstants.DEPOSIT_RECORD_BASE);
        }
    }
}
