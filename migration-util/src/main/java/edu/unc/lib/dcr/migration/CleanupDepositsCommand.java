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
import static edu.unc.lib.dl.fedora.PIDConstants.DEPOSITS_QUALIFIER;
import static org.slf4j.LoggerFactory.getLogger;

import java.nio.file.Files;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.persist.services.deposit.DepositDirectoryManager;
import edu.unc.lib.dl.persist.services.deposit.DepositModelManager;
import edu.unc.lib.dl.util.DepositStatusFactory;
import edu.unc.lib.dl.util.JobStatusFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

/**
 * Utility for cleaning up incomplete deposits
 * @author bbpennel
 */
@Command(name = "cleanup_deposits",
    description = "Cleans up deposits, including the data dir, redis values and deposit model")
public class CleanupDepositsCommand implements Callable<Integer> {

    private static final Logger output = getLogger(OUTPUT_LOGGER);

    @ParentCommand
    private MigrationCLI parentCommand;

    @Parameters(index = "0",
            description = "Deposit ID, or a comma separate list of deposit ids")
    private String depositIds;

    private String applicationContextPath = "spring/cleanup-deposits-context.xml";

    @Override
    public Integer call() throws Exception {
        long start = System.currentTimeMillis();

        String[] ids = depositIds.split(",");

        output.info("Requesting cleanup of {} deposit(s)", ids.length);

        try (
                ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(applicationContextPath);
                DepositModelManager depositModelManager = new DepositModelManager(parentCommand.tdbDir);
                ) {
            DepositStatusFactory depositStatusFactory = context.getBean(DepositStatusFactory.class);
            JobStatusFactory jobStatusFactory = context.getBean(JobStatusFactory.class);

            for (String id : ids) {
                String depositId = id.trim();
                PID depositPid = PIDs.get(DEPOSITS_QUALIFIER, depositId);

                output.info("===========================================");
                output.info("Cleaning up deposit: {}", depositId);
                output.info("===========================================");

                depositModelManager.removeModel(depositPid);
                output.info("    Removed deposit model");

                DepositDirectoryManager depositDirectoryManager = new DepositDirectoryManager(
                        depositPid, parentCommand.depositBaseDir, true, false);
                if (Files.exists(depositDirectoryManager.getDepositDir())) {
                    depositDirectoryManager.cleanupDepositDirectory();
                    output.info("    Deleted deposit directory: {}", depositDirectoryManager.getDepositDir());
                } else {
                    output.info("    No Deposit directory present");
                }

                depositStatusFactory.expireKeys(depositId, 0);
                jobStatusFactory.expireKeys(depositId, 0);
                output.info("    Deleted deposit/job status details");
            }
        }

        output.info("===========================================");
        output.info("Finished cleanup in {}ms", System.currentTimeMillis() - start);
        output.info("===========================================");

        return 0;
    }

    public void setApplicationContextPath(String applicationContextPath) {
        this.applicationContextPath = applicationContextPath;
    }
}
