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
import static org.slf4j.LoggerFactory.getLogger;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import edu.unc.lib.dcr.migration.content.ContentObjectTransformerManager;
import edu.unc.lib.dcr.migration.content.ContentTransformationService;
import edu.unc.lib.dcr.migration.deposit.DepositDirectoryManager;
import edu.unc.lib.dcr.migration.deposit.DepositModelManager;
import edu.unc.lib.dcr.migration.deposit.DepositSubmissionService;
import edu.unc.lib.dcr.migration.paths.PathIndex;
import edu.unc.lib.dl.event.PremisLoggerFactory;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectFactory;
import edu.unc.lib.dl.fcrepo4.RepositoryPIDMinter;
import edu.unc.lib.dl.fedora.PID;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

/**
 * Command for transforming content objects
 *
 * @author bbpennel
 */
@Command(name = "transform_content", aliases = {"tc"},
    description = "Transforms a tree of content objects starting from a single uuid")
public class TransformContentCommand implements Callable<Integer> {

    private static final Logger output = getLogger(OUTPUT_LOGGER);

    @ParentCommand
    private MigrationCLI parentCommand;

    @Parameters(index = "0",
            description = "UUID of the content object from which to start the transformation")
    private String startingId;

    @Option(names = {"-u", "--as-admin-units"},
            description = "Top level collections will be transformed into admin units")
    private boolean topLevelAsUnit;

    @Option(names = {"--no-hash-nesting"}, negatable = true,
            description = "Nest transformed logs in hashed subdirectories. Default: true")
    private boolean hashNesting = true;

    @Option(names = {"-g", "--generate-ids"},
            description = "Generate new ids for transformed objects, for testing.")
    private boolean generateIds;

    @Option(names = {"--deposit-into"},
            description = "Submits the transformed content for deposit to the provided container UUID")
    private String depositInto;

    @Option(names = {"-m", "--missing-deposit-records"},
            description = "Create referenced deposit records which do not have their own bxc3 object")
    private boolean createMissingDepositRecords;

    @Option(names = {"-n", "--dry-run"},
            description = "Perform the transformation but do not save the results")
    private boolean dryRun;

    private String applicationContextPath = "spring/service-context.xml";

    @Override
    public Integer call() throws Exception {
        long start = System.currentTimeMillis();

        output.info("Transforming content tree starting from {}", startingId);
        output.info("===========================================");

        ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(applicationContextPath);

        RepositoryPIDMinter pidMinter = (RepositoryPIDMinter) context.getBean("repositoryPIDMinter");
        PID depositPid = pidMinter.mintDepositRecordPid();

        output.info("Populating deposit: " + depositPid.getId());

        DepositModelManager depositModelManager = new DepositModelManager(depositPid, parentCommand.tdbDir);
        DepositDirectoryManager depositDirectoryManager = new DepositDirectoryManager(
                depositPid, parentCommand.depositBaseDir, hashNesting);

        PathIndex pathIndex = (PathIndex) context.getBean("pathIndex");
        pathIndex.setDatabaseUrl(parentCommand.databaseUrl);

        PremisLoggerFactory premisLoggerFactory = (PremisLoggerFactory) context.getBean("premisLoggerFactory");
        RepositoryObjectFactory repoObjFactory = (RepositoryObjectFactory) context.getBean("repositoryObjectFactory");

        ContentObjectTransformerManager transformerManager = new ContentObjectTransformerManager();
        transformerManager.setModelManager(depositModelManager);
        transformerManager.setPathIndex(pathIndex);
        transformerManager.setTopLevelAsUnit(topLevelAsUnit);
        transformerManager.setPidMinter(pidMinter);
        transformerManager.setDirectoryManager(depositDirectoryManager);
        transformerManager.setGenerateIds(generateIds);
        transformerManager.setPremisLoggerFactory(premisLoggerFactory);
        transformerManager.setRepositoryObjectFactory(repoObjFactory);
        transformerManager.setCreateMissingDepositRecords(createMissingDepositRecords);

        ContentTransformationService transformService = new ContentTransformationService(
                depositPid, startingId, topLevelAsUnit);
        transformService.setTransformerManager(transformerManager);
        transformService.setModelManager(depositModelManager);

        int result = transformService.perform();

        output.info("Finished transformation in {}ms", System.currentTimeMillis() - start);

        context.close();

        if (dryRun) {
            output.info("Dry run, deposit model not saved");
            depositModelManager.removeModel();
            depositDirectoryManager.cleanupDepositDirectory();
            return result;
        }

        if (depositInto != null) {
            if (result != 0) {
                output.info("Encountered issues during transformation, skipping deposit submission");
                return result;
            }

            PID destinationPid = PIDs.get(depositInto);

            DepositSubmissionService depositService = new DepositSubmissionService(
                    parentCommand.redisHost, parentCommand.redisPort);

            output.info("Submitting {} for deposit to {}", depositPid.getId(), destinationPid.getId());

            result = depositService.submitDeposit(parentCommand.username, parentCommand.groups,
                    depositPid, destinationPid);

            output.info("Deposit submitted");
        }

        return result;
    }

    public void setApplicationContextPath(String applicationContextPath) {
        this.applicationContextPath = applicationContextPath;
    }
}
