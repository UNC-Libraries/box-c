package edu.unc.lib.boxc.web.sword.utils;

import org.apache.abdera.i18n.iri.IRI;
import org.swordapp.server.DepositReceipt;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.web.sword.SwordConfigurationImpl;

/**
 * Utility for generating reports of SWORD deposits
 *
 * @author bbpennel
 *
 */
public class DepositReportingUtil {

    /**
     * Generates a DepositReceipt object for the specified PID.  This represents state of the target, how it has
     * been unpacked, as well as paths to its individual components and deposit manifest.
     * @param targetPID
     * @param config
     * @return
     */
    public DepositReceipt retrieveDepositReceipt(PID targetPID, SwordConfigurationImpl config) {
        DepositReceipt receipt = new DepositReceipt();
        return retrieveDepositReceipt(receipt, targetPID, config);
    }

    /**
     * Adds receipt information to the DepositReceipt object for the specified PID. This represents state of the target,
     * how it has been unpacked, as well as paths to its individual components and deposit manifest.
     *
     * @param receipt
     * @param targetPID
     * @param config
     * @return
     */
    public DepositReceipt retrieveDepositReceipt(DepositReceipt receipt, PID targetPID, SwordConfigurationImpl config) {
        IRI editIRI = new IRI(config.getSwordPath() + SwordConfigurationImpl.EDIT_PATH + "/" + targetPID.getId());
        receipt.setEditIRI(editIRI);
        IRI swordEditIRI = new IRI(config.getSwordPath() + SwordConfigurationImpl.EDIT_PATH + "/" + targetPID.getId());
        receipt.setSwordEditIRI(swordEditIRI);
        receipt.addEditMediaIRI(
                new IRI(config.getSwordPath() + SwordConfigurationImpl.EDIT_MEDIA_PATH + "/" + targetPID.getId()));

        return receipt;
    }
}
