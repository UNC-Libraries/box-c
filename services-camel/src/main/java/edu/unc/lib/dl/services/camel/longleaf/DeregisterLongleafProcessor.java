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

import java.net.URI;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.fedora.ServiceException;
import edu.unc.lib.dl.metrics.HistogramFactory;
import edu.unc.lib.dl.metrics.TimerFactory;
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
            if (uri.getScheme().equals("file")) {
                return Paths.get(uri).toString();
            }
            return m;
        }).collect(Collectors.joining("\n"));

        try (Timer.Context context = timer.time()) {
            int exitVal = executeCommand("deregister -l @-", deregList);

            if (exitVal == 0) {
                log.info("Successfully deregistered {} entries in longleaf", entryCount);
            } else {
                throw new ServiceException("Failed to deregister " + entryCount + " entries in Longleaf.  "
                        + "Check longleaf logs, command returned: " + exitVal);
            }

            batchSizeHistogram.update(entryCount);
        }
    }

}
