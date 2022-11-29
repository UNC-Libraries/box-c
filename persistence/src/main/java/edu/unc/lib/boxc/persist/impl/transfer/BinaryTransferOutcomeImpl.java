package edu.unc.lib.boxc.persist.impl.transfer;

import java.net.URI;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferOutcome;

/**
 * Default implementation of a binary transfer outcome
 *
 * @author bbpennel
 */
public class BinaryTransferOutcomeImpl implements BinaryTransferOutcome {

    private PID binPid;
    private URI destinationUri;
    private String destinationId;
    private String sha1;

    public BinaryTransferOutcomeImpl(PID binPid, URI destinationUri, String destinationId, String sha1) {
        this.binPid = binPid;
        this.destinationUri = destinationUri;
        this.destinationId = destinationId;
        this.sha1 = sha1;
    }

    @Override
    public URI getDestinationUri() {
        return destinationUri;
    }

    @Override
    public String getSha1() {
        return sha1;
    }

    @Override
    public PID getBinaryPid() {
        return binPid;
    }

    @Override
    public String getDestinationId() {
        return destinationId;
    }
}
