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

import edu.unc.lib.dcr.migration.content.ContentObjectTransformerManager;
import edu.unc.lib.dcr.migration.content.ContentTransformationService;
import edu.unc.lib.dcr.migration.deposit.DepositModelManager;
import edu.unc.lib.dcr.migration.paths.PathIndex;
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

    @Override
    public Integer call() throws Exception {
        long start = System.currentTimeMillis();

        output.info("Transforming content tree starting from {}", startingId);
        output.info("===========================================");
        RepositoryPIDMinter pidMinter = new RepositoryPIDMinter();
        PID depositPid = pidMinter.mintDepositRecordPid();

        output.info("Populating deposit:");
        output.info(depositPid.getId());

        DepositModelManager depositModelManager = new DepositModelManager(depositPid, parentCommand.tdbDir);

        PathIndex pathIndex = new PathIndex();
        pathIndex.setDatabaseUrl(parentCommand.databaseUrl);

        ContentObjectTransformerManager transformerManager = new ContentObjectTransformerManager();
        transformerManager.setModelManager(depositModelManager);
        transformerManager.setPathIndex(pathIndex);
        transformerManager.setTopLevelAsUnit(topLevelAsUnit);
        transformerManager.setPidMinter(pidMinter);

        ContentTransformationService transformService = new ContentTransformationService(startingId, topLevelAsUnit);
        transformService.setTransformerManager(transformerManager);

        int result = transformService.perform();

        output.info("Finished transformation in {}ms", System.currentTimeMillis() - start);

        return result;
    }
}
