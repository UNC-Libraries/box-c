package edu.unc.lib.boxc.services.camel.longleaf;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.substringAfterLast;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.unc.lib.boxc.common.util.URIUtil;
import edu.unc.lib.boxc.services.camel.util.MessageUtil;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.ProducerTemplate;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.http.HttpEntity;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
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

    private String longleafBaseUri;

    private HttpClientConnectionManager httpClientConnectionManager;

    private CloseableHttpClient httpClient;

    private ProducerTemplate producerTemplate;

    private ObjectMapper objectMapper = new ObjectMapper();

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
     * Executes longleaf register via HTTP API for a batch of files with digests
     *
     * @param messages list of fcrepoUris from the exchange
     * @param digestsMap mapping of digest algorithms to paths plus digest values
     * @param exchange the current camel exchange
     */
    private void registerFiles(List<String> messages, Map<String, List<DigestEntry>> digestsMap, Exchange exchange) {
        long start = System.currentTimeMillis();

        StringBuilder sb = new StringBuilder();
        MutableInt cnt = new MutableInt(0);
        digestsMap.forEach((key, digestEntries) -> {
            if (digestEntries.isEmpty()) {
                return;
            }
            sb.append(key).append(":\n");

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

        String requestUrl = URIUtil.join(longleafBaseUri, "register");
        var postMethod = new HttpPost(requestUrl);
        try {
            var bodyMap = Map.of(
                    "manifest", "@-",
                    "body", sb.toString(),
                    "force", true);
            HttpEntity entity = EntityBuilder.create()
                    .setText(objectMapper.writeValueAsString(bodyMap))
                    .setContentType(ContentType.APPLICATION_JSON).build();
            postMethod.setEntity(entity);

            try (var response = getHttpClient().execute(postMethod)) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != 200) {
                    log.error("Longleaf registration request failed with status {}", statusCode);
                    throw new ServiceException("Failed to register " + entryCount
                            + " entries to Longleaf. HTTP status: " + statusCode);
                }

                String responseBody = EntityUtils.toString(response.getEntity());
                JsonNode responseJson = objectMapper.readTree(responseBody);

                JsonNode successNode = responseJson.get("success");
                JsonNode failureNode = responseJson.get("failure");
                boolean hasFailures = failureNode != null && !failureNode.isEmpty();

                Map<String, String> successful = new HashMap<>();
                if (!hasFailures) {
                    log.info("Successfully registered {} entries to longleaf", entryCount);

                    digestsMap.values().stream().flatMap(Collection::stream)
                              .forEach(d -> successful.put(d.fcrepoUri, d.storageUri.toString()));
                    sendSuccessMessage(successful, exchange);
                } else {
                    // Partial failure: extract successes from the response and remove them from messages
                    if (successNode != null && !successNode.isEmpty()) {
                        for (JsonNode successPath : successNode) {
                            String completedPath = successPath.asText();
                            DigestEntry digestEntry = findDigestEntry(completedPath, digestsMap);
                            if (digestEntry != null) {
                                messages.remove(digestEntry.fcrepoUri);
                                successful.put(digestEntry.fcrepoUri, digestEntry.storageUri.toString());
                            }
                        }
                        sendSuccessMessage(successful, exchange);
                    }
                    if (messages.isEmpty()) {
                        log.error("Result from longleaf indicates registration failed, but there are "
                                + "no failed URIs remaining. See longleaf logs for details.");
                        return;
                    }
                    throw new ServiceException("Failed to register " + entryCount + " entries to Longleaf. "
                            + failureNode.size() + " failures reported.");
                }
            }
        } catch (IOException e) {
            throw new ServiceException("Error communicating with longleaf at " + requestUrl, e);
        } finally {
            postMethod.releaseConnection();
        }

        log.info("Longleaf registration completed in: {} ms", (System.currentTimeMillis() - start));
    }

    private void sendSuccessMessage(Map<String, String> successful, Exchange exchange) {
        if (successful.isEmpty()) {
            return;
        }
        if (producerTemplate == null) {
            producerTemplate = exchange.getContext().createProducerTemplate();
        }
        producerTemplate.sendBody(registrationSuccessfulEndpoint, successful);
    }

    private DigestEntry findDigestEntry(String seekPath, Map<String, List<DigestEntry>> digestsMap) {
        URI seekUri = Paths.get(seekPath).toUri();
        Optional<DigestEntry> entry = digestsMap.values().stream().flatMap(List::stream)
            .filter(de -> de.storageUri.equals(seekUri))
            .findFirst();
        return entry.orElse(null);
    }

    private CloseableHttpClient getHttpClient() {
        if (httpClient == null) {
            httpClient = HttpClients.custom()
                    .setConnectionManager(httpClientConnectionManager)
                    .build();
        }
        return httpClient;
    }

    public void destroy() {
        if (producerTemplate != null) {
            try {
                producerTemplate.close();
            } catch (Exception e) {
                log.error("Failed to close producer template", e);
            }
        }
        if (httpClient != null) {
            try {
                httpClient.close();
            } catch (Exception e) {
                log.error("Failed to close http client", e);
            }
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

    public void setLongleafBaseUri(String longleafBaseUri) {
        this.longleafBaseUri = longleafBaseUri;
    }

    public void setHttpClientConnectionManager(HttpClientConnectionManager httpClientConnectionManager) {
        this.httpClientConnectionManager = httpClientConnectionManager;
    }

    /**
     * @param exchange
     * @return true if the object is a binary that should be registered in longleaf.
     */
    public static boolean registerableBinary(Exchange exchange) {
        String fcrepoUri = MessageUtil.getFcrepoUri(exchange.getIn());
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
