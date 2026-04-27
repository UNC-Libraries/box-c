package edu.unc.lib.boxc.services.camel.longleaf;

import edu.unc.lib.boxc.fcrepo.exceptions.ServiceException;
import edu.unc.lib.boxc.persist.impl.transfer.FileSystemTransferHelpers;
import io.dropwizard.metrics5.Histogram;
import io.dropwizard.metrics5.Timer;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import edu.unc.lib.boxc.common.metrics.HistogramFactory;
import edu.unc.lib.boxc.common.util.URIUtil;
import edu.unc.lib.boxc.common.metrics.TimerFactory;

/**
 * Processor which deregisters binaries in longleaf
 *
 * @author bbpennel
 */
public class DeregisterLongleafProcessor extends AbstractLongleafProcessor {
    private static final Logger log = LoggerFactory.getLogger(DeregisterLongleafProcessor.class);

    private static final Histogram batchSizeHistogram = HistogramFactory
            .createHistogram("longleafDeregisterBatchSize");
    private static final Timer timer = TimerFactory.createTimerForClass(DeregisterLongleafProcessor.class);

    /**
     * The exchange here is expected to be a batch message containing a List
     * of binary uris for deregistration, where each uri is in string form.
     */
    @SuppressWarnings("unchecked")
    @Override
    public void process(Exchange exchange) throws Exception {
        Message aggrMsg = exchange.getIn();

        List<String> messages = aggrMsg.getBody(List.class);
        if (messages.isEmpty()) {
            return;
        }
        int entryCount = messages.size();

        log.debug("Deregistering {} binaries from longleaf", entryCount);

        String deregList = messages.stream().map(m -> {
            URI uri = URI.create(m);
            Path filePath;
            if ("file".equals(uri.getScheme())) {
                filePath = Paths.get(uri);
            } else if (uri.getScheme() == null && m.startsWith("/")) {
                // No scheme, assume it is a file path
                filePath = Paths.get(m);
            } else {
                log.warn("Ignoring invalid content URI during deregistration: {}", m);
                return null;
            }
            // Translate the content URI into its base logical path
            return FileSystemTransferHelpers.getBaseBinaryPath(filePath.normalize());
        }).filter(m -> m != null).collect(Collectors.joining("\n"));
        // No valid content URIs to deregister
        if (deregList.isEmpty()) {
            return;
        }

        try (Timer.Context context = timer.time()) {
            deregisterFiles(messages, deregList, entryCount);
        }
    }

    /**
     * Executes longleaf deregister via HTTP API for a batch of file paths
     *
     * @param messages list of original content URIs from the exchange
     * @param deregList newline-separated list of base file paths to deregister
     * @param entryCount number of entries being deregistered
     */
    private void deregisterFiles(List<String> messages, String deregList, int entryCount) {
        batchSizeHistogram.update(entryCount);

        String requestUrl = URIUtil.join(longleafBaseUri, "api/deregister");
        Map<String, Object> bodyMap = Map.of(
                "from_list", "@-",
                "body", deregList);
        LongleafApiResult result = executeHttpPost(requestUrl, bodyMap);

        if (!result.hasFailures()) {
            log.info("Successfully deregistered {} entries in longleaf", entryCount);
        } else {
            // Trim successfully deregistered files from the message before throwing exception
            for (String successPath : result.successes) {
                messages.remove(Paths.get(successPath).toUri().toString());
            }
            if (messages.isEmpty()) {
                log.error("Result from longleaf indicates deregistration failed, but there are "
                        + "no failed URIs remaining. See longleaf logs for details.");
                return;
            }
            throw new ServiceException("Failed to deregister " + entryCount + " entries in Longleaf. "
                    + result.failures.size() + " failures reported.");
        }
    }
}
