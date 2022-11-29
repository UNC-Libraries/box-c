package edu.unc.lib.boxc.persist.api.transfer;

import java.net.URI;

import edu.unc.lib.boxc.model.api.ids.PID;

/**
 * Information describing the outcome of a binary transfer operation
 *
 * @author bbpennel
 */
public interface BinaryTransferOutcome {

    /**
     * @return PID of the binary object the transferred file was associated with.
     */
    PID getBinaryPid();

    /**
     * @return URI where the binary is stored after the transfer
     */
    URI getDestinationUri();

    /**
     * @return ID of the storage location where the binary was transferred to
     */
    String getDestinationId();

    /**
     * @return SHA1 calculated of the binary during transfer
     */
    String getSha1();
}
