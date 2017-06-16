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
package edu.unc.lib.dl.cdr.sword.server.deposit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

import org.apache.abdera.Abdera;
import org.apache.abdera.writer.Writer;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.swordapp.server.Deposit;
import org.swordapp.server.DepositReceipt;
import org.swordapp.server.SwordConfiguration;
import org.swordapp.server.SwordError;
import org.swordapp.server.UriRegistry;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ErrorURIRegistry;
import edu.unc.lib.dl.util.PackagingType;
import edu.unc.lib.dl.util.RedisWorkerConstants.Priority;

/**
 * Default handler when packaging type is null.
 * May include a file or not.
 * All metadata is inside of the Atom entry.
 * @author count0
 *
 */
public class AtomPubEntryDepositHandler extends AbstractDepositHandler {
    private static Logger log = Logger
            .getLogger(AtomPubEntryDepositHandler.class);

    @Override
    public DepositReceipt doDeposit(PID destination, Deposit deposit,
            PackagingType type, Priority priority, SwordConfiguration config,
            String depositor, String owner) throws SwordError {
        log.debug("Preparing to perform an Atom Pub entry metadata only deposit to "
                + destination.getPid());

        if (deposit.getSwordEntry() == null
                || deposit.getSwordEntry().getEntry() == null) {
            throw new SwordError(UriRegistry.ERROR_CONTENT, 415,
                    "No AtomPub entry was included in the submission");
        }

        if (log.isDebugEnabled()) {
            Abdera abdera = new Abdera();
            Writer writer = abdera.getWriterFactory().getWriter("prettyxml");
            try {
                writer.writeTo(deposit.getSwordEntry().getEntry(), System.out);
            } catch (IOException e) {
                throw new Error(e);
            }
        }

        PID depositPID = null;
        UUID depositUUID = UUID.randomUUID();
        depositPID = new PID("uuid:" + depositUUID.toString());
        File dir = makeNewDepositDirectory(depositPID.getUUID());
        dir.mkdir();

        // write SWORD Atom entry to file
        File atomFile = new File(dir, "atom.xml");
        Abdera abdera = new Abdera();

        try (FileOutputStream fos = new FileOutputStream(atomFile)) {
            Writer writer = abdera.getWriterFactory().getWriter("prettyxml");
            writer.writeTo(deposit.getSwordEntry().getEntry(), fos);
        } catch (IOException e) {
            throw new SwordError(ErrorURIRegistry.INGEST_EXCEPTION, 400,
                    "Unable to unpack your deposit: " + deposit.getFilename(),
                    e);
        }

        // write deposit file to data directory
        if (deposit.getFile() != null) {
            File dataDir = new File(dir, "data");
            dataDir.mkdirs();
            File depositFile = new File(dataDir, deposit.getFilename());
            try {
                FileUtils.moveFile(deposit.getFile(), depositFile);
            } catch (IOException e) {
                throw new Error(e);
            }
        }

        registerDeposit(depositPID, destination, deposit,
                type, priority, depositor, owner, Collections.<String, String>emptyMap());
        return buildReceipt(depositPID, config);
    }
}
