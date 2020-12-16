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
import static org.slf4j.LoggerFactory.getLogger;

import java.util.concurrent.Callable;

import org.slf4j.Logger;

import edu.unc.lib.dcr.migration.deposit.PreconstructedDepositSubmissionService;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

/**
 * Command for submitting existing deposits
 *
 * @author bbpennel
 */
@Command(name = "submit_deposit", aliases = {"sd"},
        description = "Submit preconstructed deposits for ingest")
public class SubmitDepositCommand implements Callable<Integer> {

    private static final Logger output = getLogger(OUTPUT_LOGGER);

    @ParentCommand
    private MigrationCLI parentCommand;

    @Parameters(index = "0",
            description = "UUID of the deposit to submit")
    private String depositId;

    @Parameters(index = "1",
            description = "UUID of the container into which the deposit will be ingest")
    private String destinationId;

    @Option(names = {"--display-label"},
            defaultValue = "",
            description = "Text of display label to use, instead of the UUID, for info display.")
    private String displayLabel;

    @Override
    public Integer call() throws Exception {
        output.info(BannerUtility.getBanner());

        try (PreconstructedDepositSubmissionService depositService = new PreconstructedDepositSubmissionService(
                parentCommand.redisHost, parentCommand.redisPort)) {

            PID depositPid = PIDs.get(DEPOSIT_RECORD_BASE, depositId);
            PID destinationPid = PIDs.get(destinationId);

            output.info("Submitting {} for deposit to {}", depositPid.getQualifiedId(), destinationPid.getId());

            int result = depositService.submitDeposit(parentCommand.username, parentCommand.groups,
                    depositPid, destinationPid, displayLabel);

            output.info("Deposit submitted");

            return result;
        }
    }
}
