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
package edu.unc.lib.dcr.migration;

import static edu.unc.lib.dcr.migration.MigrationConstants.OUTPUT_LOGGER;
import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.DEPOSIT_RECORD_BASE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.util.concurrent.Callable;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.persist.services.deposit.DepositModelManager;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

/**
 * Command for performing updates against a deposit model
 *
 * @author bbpennel
 */
@Command(name = "update_model", aliases = {"um"},
description = "Performs updates against deposit models")
public class UpdateModelCommand implements Callable<Integer> {

    private static final Logger output = getLogger(OUTPUT_LOGGER);

    private static final String STDIN_PATH = "@-";

    @ParentCommand
    private MigrationCLI parentCommand;

    @Parameters(index = "0",
            description = "Id of the deposit to update")
    private String depositId;

    @Parameters(index = "1",
            description = "Path to sparql update file, or @- to read from STDIN")
    private String sparqlQuery;

    @Override
    public Integer call() throws Exception {
        long start = System.currentTimeMillis();

        PID depositPid = PIDs.get(DEPOSIT_RECORD_BASE, depositId);
        try (DepositModelManager depositModelManager = new DepositModelManager(parentCommand.tdbDir)) {

            String queryString;
            if (sparqlQuery.equals(STDIN_PATH)) {
                queryString = IOUtils.toString(System.in, UTF_8);
            } else {
                queryString = FileUtils.readFileToString(new File(sparqlQuery), UTF_8);
            }

            output.info("Executing sparql update on deposit {}:\n{}", depositPid.getId(), queryString);

            depositModelManager.performUpdate(depositPid, queryString);

            output.info("Completed sparql update on deposit {} in {}ms",
                    depositPid.getId(), (System.currentTimeMillis() - start));
        } catch (Exception e) {
            output.error("Failed to perform update", e);
        }

        return 0;
    }

}
