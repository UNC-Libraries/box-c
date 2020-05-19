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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.Paths;

import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.deposit.work.AbstractDepositJob;
import edu.unc.lib.dl.fcrepo4.BinaryObject;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.model.DatastreamPids;
import edu.unc.lib.dl.rdf.CdrDeposit;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;

/**
 * Job which registers ingested files to longleaf
 *
 * @author smithjp
 *
 */
public class RegisterToLongleafJob extends AbstractDepositJob {
    private static final Logger log = LoggerFactory.getLogger(RegisterToLongleafJob.class);
    private static final Logger longleafLog = LoggerFactory.getLogger("longleaf");

    private String longleafBaseCommand;

    @Autowired
    private RepositoryObjectLoader repoObjLoader;

    private boolean excludeDepositRecord;

    public RegisterToLongleafJob() {
        super();
    }

    public RegisterToLongleafJob(String uuid, String depositUUID) {
        super(uuid, depositUUID);
    }

    @Override
    public void runJob() {
        log.info("Registering files from deposit {} to longleaf", getDepositPID());

        excludeDepositRecord = Boolean.parseBoolean(getDepositStatus().get(DepositField.excludeDepositRecord.name()));

        Model model = getReadOnlyModel();
        Bag depositBag = model.getBag(getDepositPID().getRepositoryPath());

        registerFilesToLongleaf(depositBag);
    }

    /**
     * Iterates through deposit bag to find files which can be registered with
     * longleaf
     *
     * @param resc
     */
    public void registerFilesToLongleaf(Resource resc) {
        // register premis for deposit record, if one was generated
        if (!excludeDepositRecord) {
            PID objPid = PIDs.get(resc.toString());
            BinaryObject premisBin = repoObjLoader.getBinaryObject(DatastreamPids.getMdEventsPid(objPid));
            URI premisStorageUri = premisBin.getContentUri();

            if (premisStorageUri != null) {
                registerFile(premisStorageUri, null);
            }
        }

        StmtIterator statementIterator = resc.listProperties();
        try {
            while (statementIterator.hasNext()) {
                Statement currentStatement = statementIterator.nextStatement();

                // find storageUri, descriptiveStorageUri, descriptiveHistoryStorageUri, fitsStorageUri
                if (currentStatement.getPredicate().toString().matches(".*Uri")) {
                    URI storageUri = URI.create(currentStatement.getString());
                    if (currentStatement.getString().matches(".*original_file")) {
                        String checksum = resc.getProperty(CdrDeposit.md5sum).getString();
                        registerFile(storageUri, checksum);
                    } else {
                        registerFile(storageUri, null);
                    }
                }
            }
        } finally {
            statementIterator.close();
        }

        NodeIterator nodeIterator = getChildIterator(resc);
        // No more children, nothing further to do in this tree
        if (nodeIterator == null) {
            return;
        }

        try {
            while (nodeIterator.hasNext()) {
                Resource childResc = (Resource) nodeIterator.next();
                registerFilesToLongleaf(childResc);
            }
        } finally {
            nodeIterator.close();
        }
    }

    /**
     * Executes longleaf register command for file
     *
     * @param storageUri
     * @param checksum
     */
    private void registerFile(URI storageUri, String checksum) {
        long start = System.currentTimeMillis();

        String fileLocation = Paths.get(storageUri).toString();

        try {
            // only register binaries with md5sum
            String longleafCommmand;
            if (checksum != null) {
                longleafCommmand = longleafBaseCommand + " register -f " + fileLocation + " --checksums \"md5:" +
                        checksum + "\"";
            } else {
                longleafCommmand = longleafBaseCommand + " register -f " + fileLocation;
            }
            log.info("Registering with longleaf: {}", longleafCommmand);

            Process process = Runtime.getRuntime().exec(longleafCommmand);

            int exitVal = process.waitFor();

            // log longleaf output
            String line;
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                while ((line = in.readLine()) != null) {
                    longleafLog.info(line);
                }
            }
            try (BufferedReader err = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()))) {
                while ((line = err.readLine()) != null) {
                    longleafLog.error(line);
                }
            }

            if (exitVal == 0) {
                log.info("Successfully registered: {}", fileLocation);
            } else {
                failJob("Failed to register " + fileLocation + "to Longleaf",
                        "Check longleaf logs, command returned: " + exitVal);
            }
        } catch (IOException e) {
            log.error("IOException while trying to register {} to longleaf: {}", fileLocation, e);
        } catch (InterruptedException e) {
            log.error("InterruptedException while trying to register {} to longleaf: {}", fileLocation, e);
        }

        log.info("Longleaf registration completed in: {} ms", (System.currentTimeMillis() - start));
    }

    public void setLongleafBaseCommand(String longleafBaseCommand) {
        this.longleafBaseCommand = longleafBaseCommand;
    }
}
