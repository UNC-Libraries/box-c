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
package edu.unc.lib.deposit.fcrepo4;

import edu.unc.lib.deposit.work.AbstractDepositJob;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.CdrDeposit;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

/**
 * Job which registers ingested files to longleaf
 *
 * @author smithjp
 *
 */
public class RegisterToLongleafJob extends AbstractDepositJob {
    private static final Logger log = LoggerFactory.getLogger(RegisterToLongleafJob.class);

    private String longleafBaseCommand;

    public RegisterToLongleafJob() {
        super();
    }

    public RegisterToLongleafJob(String uuid, String depositUUID) {
        super(uuid, depositUUID);
    }

    @Override
    public void runJob() {
        log.debug("Registering files from deposit {} to longleaf", getDepositPID());

        Model model = getReadOnlyModel();
        Bag depositBag = model.getBag(getDepositPID().getRepositoryPath());

        registerFilesToLongleaf(depositBag);
    }

    public void registerFilesToLongleaf(Resource resc) {
        Set<Resource> rescTypes = resc.listProperties(RDF.type).toList().stream()
                .map(Statement::getResource).collect(toSet());

        if (rescTypes.contains(Cdr.FileObject)) {
            String checksum = resc.getProperty(CdrDeposit.sha1sum).getString();
            if (resc.hasProperty(CdrDeposit.storageUri)) {
                registerFile(resc.getProperty(CdrDeposit.storageUri).getString(), resc.getURI(), checksum);
            } else if (resc.hasProperty(CdrDeposit.descriptiveStorageUri)) {
                registerFile(resc.getProperty(CdrDeposit.descriptiveStorageUri).getString(), resc.getURI(), checksum);
            } else {
                log.debug("File {} does not have storageUri or descriptiveStorageUri properties", resc.getURI());
            }
        }

        NodeIterator iterator = getChildIterator(resc);
        // No more children, nothing further to do in this tree
        if (iterator == null) {
            return;
        }

        try {
            while (iterator.hasNext()) {
                Resource childResc = (Resource) iterator.next();
                registerFilesToLongleaf(childResc);
            }
        } finally {
            iterator.close();
        }
    }

    private void registerFile(String fileLocation, String fileId, String checksum) {
        long start = System.currentTimeMillis();

        try {
            String longleafCommmand = longleafBaseCommand + " register -f " + fileLocation + " --checksums 'sha1:" +
                    checksum + "' --force";
            log.debug("Registering with longleaf: {}", longleafCommmand);

            Process process = Runtime.getRuntime().exec(longleafCommmand);

            int exitVal = process.waitFor();
            if (exitVal == 0) {
                log.info("Successfully registered: {}", fileId);
            } else {
                log.error("Failed to register {} to Longleaf: {}", fileId, exitVal);
            }
        } catch (IOException e) {
            log.error("IOException while trying to register {} to longleaf: {}", fileId, e);
        } catch (InterruptedException e) {
            log.error("InterruptedException while trying to register {} to longleaf: {}", fileId, e);
        }

        log.debug("Longleaf registration completed in: {} ms", (System.currentTimeMillis() - start));
    }

    public void setLongleafBaseCommand(String longleafBaseCommand) {
        this.longleafBaseCommand = longleafBaseCommand;
    }
}
