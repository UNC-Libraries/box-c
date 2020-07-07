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
package edu.unc.lib.dl.services.camel.longleaf;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.substringAfterLast;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;
import org.fcrepo.client.FedoraHeaderConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.fcrepo4.BinaryObject;
import edu.unc.lib.dl.fcrepo4.ClientFaultResolver;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fcrepo4.RepositoryPathConstants;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.fedora.ServiceException;
import edu.unc.lib.dl.metrics.HistogramFactory;
import edu.unc.lib.dl.metrics.TimerFactory;
import edu.unc.lib.dl.model.DatastreamType;
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

    /**
     * The exchange here is expected to be a batch message containing a List
     * of binary PIDs for registration, in string form.
     */
    @SuppressWarnings("unchecked")
    @Override
    public void process(Exchange exchange) throws Exception {
        Message aggrMsg = exchange.getIn();

        // Key is the digest key, value is a map of storage uri to digest value
        Map<String, Map<URI, String>> digestsMap = new HashMap<>();
        Map<URI, String> md5Map = new HashMap<>();
        Map<URI, String> sha1Map = new HashMap<>();
        digestsMap.put("md5", md5Map);
        digestsMap.put("sha1", sha1Map);

        List<String> messages = aggrMsg.getBody(List.class);
        for (String fcrepoUri : messages) {
            try {
                PID pid = PIDs.get(fcrepoUri);
                BinaryObject binObj = repoObjLoader.getBinaryObject(pid);

                String md5 = trimFedoraDigest(binObj.getMd5Checksum(), ":");
                String sha1 = trimFedoraDigest(binObj.getSha1Checksum(), ":");
                URI storageUri = binObj.getContentUri();

                if (md5 != null) {
                    md5Map.put(storageUri, md5);
                }
                if (sha1 != null) {
                    sha1Map.put(storageUri, sha1);
                }
                if (md5 == null && sha1 == null) {
                    sha1Map.put(storageUri, calculateSha1(pid));
                }
            } catch (Exception e) {
                log.error("Failed to add {} to batch for regisration to longleaf", fcrepoUri, e);
            }
        }

        try (Timer.Context context = timer.time()) {
            registerFiles(digestsMap);
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
     * @param digestsMap mapping of digest algorithms to paths plus digest values
     */
    private void registerFiles(Map<String, Map<URI, String>> digestsMap) {
        long start = System.currentTimeMillis();

        StringBuilder sb = new StringBuilder();
        MutableInt cnt = new MutableInt(0);
        digestsMap.entrySet().forEach(digestGroup -> {
            Map<URI, String> digestToPath = digestGroup.getValue();
            if (digestToPath.size() == 0) {
                return;
            }
            sb.append(digestGroup.getKey()).append(":\n");

            digestToPath.entrySet().forEach(manifestEntry -> {
                sb.append(manifestEntry.getValue())
                    .append(' ')
                    .append(Paths.get(manifestEntry.getKey()).toString())
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

        int exitVal = executeCommand("register --force -m @-", sb.toString());

        if (exitVal == 0) {
            log.info("Successfully registered {} entries to longleaf", entryCount);
        } else {
            throw new ServiceException("Failed to register " + entryCount + " entries to Longleaf.  "
                    + "Check longleaf logs, command returned: " + exitVal);
        }

        log.info("Longleaf registration completed in: {} ms", (System.currentTimeMillis() - start));
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
