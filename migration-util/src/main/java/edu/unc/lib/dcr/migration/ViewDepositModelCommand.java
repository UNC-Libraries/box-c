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
import static edu.unc.lib.dl.util.RDFModelUtil.streamModel;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.jena.riot.RDFFormat.NTRIPLES;
import static org.apache.jena.riot.RDFFormat.TURTLE;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.util.concurrent.Callable;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.jena.riot.RDFFormat;
import org.slf4j.Logger;

import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.persist.services.deposit.DepositModelManager;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

/**
 * View deposit model command
 *
 * @author bbpennel
 */
@Command(name = "view_deposit_model", aliases = {"vdm"},
        description = "Display the stored RDF for a deposit")
public class ViewDepositModelCommand implements Callable<Integer> {

    private static final Logger output = getLogger(OUTPUT_LOGGER);

    private static final String STDIN_PATH = "@-";

    @ParentCommand
    private MigrationCLI parentCommand;

    @Parameters(index = "0",
            description = "ID of the deposit to retrieve")
    private String depositId;

    @Option(names = {"-t", "--turtle"},
            description = "Serialize the RDF as turtle instead of n-triples")
    private boolean asTurtle;

    @Option(names = {"-q", "--query"},
            description = "Sparql query to perform against the model. Results returned as CSV")
    private String sparqlQuery;

    @Override
    public Integer call() throws Exception {
        PID depositPid = PIDs.get(DEPOSIT_RECORD_BASE, depositId);

        try (DepositModelManager depositModelManager = new DepositModelManager(parentCommand.tdbDir)) {

            if (sparqlQuery == null) {
                RDFFormat format = asTurtle ? TURTLE : NTRIPLES;

                output.info(IOUtils.toString(
                        streamModel(depositModelManager.getReadModel(depositPid), format), UTF_8));
            } else {
                String queryString;
                if (sparqlQuery.equals(STDIN_PATH)) {
                    queryString = IOUtils.toString(System.in, UTF_8);
                } else {
                    queryString = FileUtils.readFileToString(new File(sparqlQuery), UTF_8);
                }

                String results = depositModelManager.performQuery(depositPid, queryString);
                output.info(results);
            }

            depositModelManager.commit();
        }

        return 0;
    }
}
