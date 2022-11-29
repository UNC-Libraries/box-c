package edu.unc.lib.boxc.services.camel.longleaf;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.common.metrics.HistogramFactory;
import edu.unc.lib.boxc.common.metrics.TimerFactory;
import edu.unc.lib.boxc.fcrepo.exceptions.ServiceException;
import edu.unc.lib.boxc.persist.impl.transfer.FileSystemTransferHelpers;
import io.dropwizard.metrics5.Histogram;
import io.dropwizard.metrics5.Timer;

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
        if (deregList.length() == 0) {
            return;
        }

        try (Timer.Context context = timer.time()) {
            ExecuteResult result = executeCommand("deregister -l @-", deregList);

            if (result.exitVal == 0) {
                log.info("Successfully deregistered {} entries in longleaf", entryCount);
            } else {
                // Trim successfully deregistered files from the message before throwing exception
                if (!result.completed.isEmpty()) {
                    result.completed.stream()
                        .map(c -> Paths.get(c).toUri().toString())
                        .forEach(messages::remove);
                }
                if (messages.isEmpty()) {
                    log.error("Result from longleaf indicates deregistration failed, but there are "
                            + "no failed URIs remaining. See longleaf logs for details.");
                    return;
                }
                throw new ServiceException("Failed to deregister " + entryCount + " entries in Longleaf.  "
                        + "Check longleaf logs, command returned: " + result.exitVal);
            }

            batchSizeHistogram.update(entryCount);
        }
    }

}
