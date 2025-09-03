package edu.unc.lib.boxc.services.camel.routing;

import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.slf4j.LoggerFactory.getLogger;

import edu.unc.lib.boxc.fcrepo.FcrepoJmsConstants;
import edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.fcrepo.camel.FcrepoHeaders;
import org.slf4j.Logger;

/**
 * Processor which ensures fedora related headers are present and appear valid
 *
 * @author bbpennel
 */
public class FedoraHeadersProcessor implements Processor {
    private static final Logger log = getLogger(FedoraHeadersProcessor.class);

    @Override
    public void process(Exchange exchange) throws Exception {
        var msg = exchange.getIn();
        // Replace fedora:description with fcr:metadata if it appears in the identifier
        String identifier = msg.getHeader(FcrepoJmsConstants.IDENTIFIER, String.class);
        if (identifier != null && identifier.contains("fedora:description")) {
            log.debug("Replacing fedora:description with fcr:metadata in identifier {}", identifier);
            identifier = identifier.replace("fedora:description", RepositoryPathConstants.FCR_METADATA);
            msg.setHeader(FcrepoJmsConstants.IDENTIFIER, identifier);
        }
        // Ensure that the CamelFcrepoUri header is set
        String fcrepoUri = msg.getHeader(FcrepoHeaders.FCREPO_URI, String.class);
        if (fcrepoUri == null) {
            String fcrepoBaseUrl = msg.getHeader(FcrepoJmsConstants.BASE_URL, String.class);
            if (fcrepoBaseUrl == null || identifier == null) {
                return;
            }
            msg.setHeader(FCREPO_URI, fcrepoBaseUrl + identifier);
        }
    }
}
