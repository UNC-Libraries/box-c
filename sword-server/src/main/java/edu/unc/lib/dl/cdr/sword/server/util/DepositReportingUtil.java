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
package edu.unc.lib.dl.cdr.sword.server.util;

import org.apache.abdera.i18n.iri.IRI;
import org.swordapp.server.DepositReceipt;

import edu.unc.lib.dl.cdr.sword.server.SwordConfigurationImpl;
import edu.unc.lib.dl.fedora.PID;

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
