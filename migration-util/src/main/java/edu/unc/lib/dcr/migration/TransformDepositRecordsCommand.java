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

import java.nio.file.Path;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import edu.unc.lib.dcr.migration.deposit.DepositRecordTransformationService;
import edu.unc.lib.dcr.migration.paths.PathIndex;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

/**
 * Command for transforming deposit records
 *
 * @author bbpennel
 */
@Command(name = "transform_deposit_records", aliases = {"tdr"},
    description = "Transforms a bxc3 deposit records into boxc5 ones in a repository")
public class TransformDepositRecordsCommand implements Callable<Integer> {

    private static final Logger output = getLogger(OUTPUT_LOGGER);

    @ParentCommand
    private MigrationCLI parentCommand;

    @Parameters(index = "0",
            description = "Path of file containing a list of deposit record pids to transform")
    private Path pidListPath;

    @Option(names = {"-g", "--generate-ids"},
            description = "Generate new ids for transformed objects, for testing.")
    private boolean generateIds;

    @Option(names = {"-s", "--storage-location"},
            defaultValue = "primary_storage",
            description = "Identifier of the storage location manifests will be put in.")
    private String storageLocationId;

    private String applicationContextPath = "spring/service-context.xml";

    @Override
    public Integer call() throws Exception {
        long start = System.currentTimeMillis();

        output.info("Using properties from {}", System.getProperty("config.properties.uri"));

        try (ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(applicationContextPath)) {

            PathIndex pathIndex = (PathIndex) context.getBean("pathIndex");
            pathIndex.setDatabaseUrl(parentCommand.databaseUrl);

            DepositRecordTransformationService transformService =
                    (DepositRecordTransformationService) context.getBean("depositRecordTransformationService");
            transformService.setGenerateIds(generateIds);

            output.info("Transforming deposit records from {}", pidListPath);
            output.info("===========================================");

            int result = transformService.perform(pidListPath, storageLocationId);

            output.info("Finished transformation in {}ms", System.currentTimeMillis() - start);

            return result;
        }
    }

    public void setApplicationContextPath(String applicationContextPath) {
        this.applicationContextPath = applicationContextPath;
    }

}
