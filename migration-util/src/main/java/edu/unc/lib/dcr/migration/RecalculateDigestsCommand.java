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

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;
import org.fcrepo.client.FedoraHeaderConstants;
import org.slf4j.Logger;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectFactory;
import edu.unc.lib.dl.fcrepo4.RepositoryPathConstants;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.model.DatastreamType;
import edu.unc.lib.dl.rdf.Fcrepo4Repository;
import edu.unc.lib.dl.sparql.SparqlQueryService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

/**
 * Command for recalculate sha1 digests of repository files
 *
 * @author bbpennel
 */
@Command(name = "recalculate_digests",
    description = "Command which recalculates digests for repository files")
public class RecalculateDigestsCommand implements Callable<Integer> {

    private static final Logger output = getLogger(OUTPUT_LOGGER);

    @ParentCommand
    private MigrationCLI parentCommand;

    @Option(names = {"-n", "--dry-run"},
            description = "Perform the recalculation but do not save the results")
    private boolean dryRun;

    @Option(names = {"--count"},
            description = "Count the number of matching objects")
    private boolean countOnly;

    private String applicationContextPath = "spring/service-context.xml";

    private final static String METADATA_SUFFIXES = String.join("|", Arrays.asList(
            DatastreamType.MD_DESCRIPTIVE.getId(),
            DatastreamType.MD_DESCRIPTIVE_HISTORY.getId(),
            DatastreamType.MD_EVENTS.getId(),
            DatastreamType.TECHNICAL_METADATA.getId(),
            RepositoryPathConstants.DEPOSIT_MANIFEST_CONTAINER + "/.+"));

    private final static String ALL_METADATA_BINS_QUERY =
            "SELECT ?bin_pid" +
            " WHERE { ?bin_pid <" + RDF.type.getURI() + "> <" + Fcrepo4Repository.Binary.getURI() + "> ." +
                " FILTER(regex(str(?bin_pid), \"/(" + METADATA_SUFFIXES + ")$\")) }";

    @Override
    public Integer call() throws Exception {
        long start = System.currentTimeMillis();

        output.info("Using properties from {}", System.getProperty("config.properties.uri"));

        try (ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(applicationContextPath)) {
            SparqlQueryService queryService = (SparqlQueryService) context.getBean("sparqlQueryService");

            List<String> binUris = new ArrayList<>();
            try (QueryExecution qexec = queryService.executeQuery(ALL_METADATA_BINS_QUERY)) {
                ResultSet results = qexec.execSelect();

                while (results.hasNext()) {
                    QuerySolution soln = results.nextSolution();
                    Resource binPidResc = soln.getResource("bin_pid");

                    if (binPidResc == null) {
                        continue;
                    }

                    binUris.add(binPidResc.getURI());
                }
            }

            output.info("Retrieved list of {} binaries to update", binUris.size());

            if (countOnly) {
                return 0;
            }

            if (dryRun) {
                output.info("Dry run -- only calculating SHA1 digests");
            } else {
                output.info("Recalculating SHA1 digests and updating in record");
            }
            output.info("===========================================");

            RepositoryObjectFactory repoObjFactory = (RepositoryObjectFactory)
                    context.getBean("repositoryObjectFactory");
            FcrepoClient fcrepoClient = (FcrepoClient) context.getBean("fcrepoClient");

            for (String binPath : binUris) {
                PID binPid = PIDs.get(binPath);
                String sha1;
                String mimetype;
                String filename;
                URI storageUri;
                // Calculate an up to date SHA1 for each binary
                try (FcrepoResponse resp = fcrepoClient.head(binPid.getRepositoryUri()).wantDigest("sha").perform()) {
                    String digest = resp.getHeaderValue(FedoraHeaderConstants.DIGEST);
                    sha1 = digest.split("=")[1];
                    mimetype = resp.getHeaderValue("Content-Type");
                    filename = resp.getContentDisposition().get(FedoraHeaderConstants.CONTENT_DISPOSITION_FILENAME);
                    storageUri = URI.create(resp.getHeaderValue("Content-Location"));
                } catch (FcrepoOperationFailedException e) {
                    output.info("{}: Failed to update -- {}", binPid.getQualifiedId(), e.getMessage());
                    continue;
                }

                if (dryRun) {
                    output.info("{}: {} (dry run)", binPid.getQualifiedId(), sha1);
                } else {
                    // Update the binary with existing metadata plus the requested digest
                    repoObjFactory.createOrUpdateBinary(binPid, storageUri, filename, mimetype, sha1, null, null);
                    output.info("{}: {} (updated)", binPid.getQualifiedId(), sha1);
                }
            }

            output.info("Finished recalculating in {}ms", System.currentTimeMillis() - start);

            return 0;
        }
    }

    public void setApplicationContextPath(String applicationContextPath) {
        this.applicationContextPath = applicationContextPath;
    }
}
