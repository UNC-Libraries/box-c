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
import static edu.unc.lib.dl.model.DatastreamType.ORIGINAL_FILE;
import static edu.unc.lib.dl.rdf.CdrDeposit.stagingLocation;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.slf4j.LoggerFactory.getLogger;

import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.Callable;

import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import edu.unc.lib.dcr.migration.deposit.PreconstructedDepositSubmissionService;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryPIDMinter;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.persist.services.deposit.DepositModelHelpers;
import edu.unc.lib.dl.persist.services.deposit.DepositModelManager;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.CdrDeposit;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

/**
 * @author bbpennel
 */
@Command(name = "generate_work",
        description = "Command for producing test objects")
public class GenerateWorkCommand implements Callable<Integer> {
    private static final Logger output = getLogger(OUTPUT_LOGGER);

    @ParentCommand
    private MigrationCLI parentCommand;

    @Parameters(index = "0",
            description = "PID of the destination object to which new objects are added")
    private String destinationId;

    @Option(names = {"--num-files", "-n"},
            defaultValue = "10",
            description = "Number of file objects to add to the work")
    private int numberOfFiles;

    @Option(names = {"--file-path"},
            description = "Path of the file to ingest into the work")
    private Path filePath;

    @Option(names = {"--file-ext"},
            description = "Extension for filename of added files. Default: none")
    private String fileExt;

    private String applicationContextPath = "spring/service-context.xml";

    @Override
    public Integer call() throws Exception {
        long start = System.currentTimeMillis();

        PID destPid = PIDs.get(destinationId);

        output.info("Creating work with {} files", numberOfFiles);
        output.info("===========================================");

        try (
                ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(applicationContextPath);
                DepositModelManager depositModelManager = new DepositModelManager(parentCommand.tdbDir);
            ) {
            RepositoryPIDMinter repoPidMinter = (RepositoryPIDMinter)
                    context.getBean("repositoryPIDMinter");

            PID depositPid = repoPidMinter.mintDepositRecordPid();

            Model depositObjModel = createDefaultModel();
            Bag depositBag = depositObjModel.createBag(depositPid.getRepositoryPath());

            PID workPid = repoPidMinter.mintContentPid();
            Bag workResc = depositObjModel.createBag(workPid.getRepositoryPath());
            depositBag.add(workResc);
            workResc.addProperty(RDF.type, Cdr.Work);
            workResc.addLiteral(CdrDeposit.label, "Generated Test Work " + Instant.now());

            String filePathString = filePath.toUri().toString();
            String ext = fileExt == null ? "" : ("." + fileExt);
            for (int i = 0; i < numberOfFiles; i++) {
                PID filePid = repoPidMinter.mintContentPid();
                Resource fileResc = depositObjModel.getResource(filePid.getRepositoryPath());
                workResc.add(fileResc);
                fileResc.addProperty(RDF.type, Cdr.FileObject);
                fileResc.addLiteral(CdrDeposit.label, "file" + i + ext);
                Resource origResc = DepositModelHelpers.addDatastream(fileResc, ORIGINAL_FILE);
                origResc.addLiteral(stagingLocation, filePathString);

            }

            depositModelManager.addTriples(depositPid, depositObjModel);

            try (PreconstructedDepositSubmissionService depositService =
                    new PreconstructedDepositSubmissionService(parentCommand.redisHost, parentCommand.redisPort)) {

                output.info("Submitting {} for deposit to {}", depositPid.getId(), destPid);

                depositService.submitDeposit(parentCommand.username, parentCommand.groups,
                        depositPid, destPid, "generated_worked_" + numberOfFiles);

                output.info("Deposit submitted");
            }

            output.info("Finished generating deposit in {}ms", System.currentTimeMillis() - start);

            return 0;
        }

    }
}
