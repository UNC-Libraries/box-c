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
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.slf4j.LoggerFactory.getLogger;

import java.nio.file.Path;
import java.util.concurrent.Callable;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.persist.services.destroy.DestroyObjectsService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

/**
 * @author bbpennel
 */
@Command(name = "destroy_objects",
        description = "Destroy one or more objects")
public class DestroyObjectsCommand implements Callable<Integer> {
    private static final Logger output = getLogger(OUTPUT_LOGGER);

    @ParentCommand
    private MigrationCLI parentCommand;

    @Option(names = {"--completely"},
            defaultValue = "false",
            description = "If provided, objects will be destroyed and completely cleaned up")
    private boolean completely;

    @Option(names = {"--from-file", "-f"},
            description = "File path from which ids will be read. They should be newline separated")
    private Path fromFile;

    @Option(names = {"--ids", "-i"},
            split = ",",
            description = "Comma separated list of IDs. Must provide either this option of -f")
    private String[] inputIds;

    private String applicationContextPath = "spring/destroy-objects-context.xml";

    private DestroyObjectsService destroyService;

    @Override
    public Integer call() throws Exception {
        long start = System.currentTimeMillis();

        if (fromFile == null && (inputIds == null || inputIds.length == 0)) {
            output.error("Must provide IDs via either -f or -i parameters");
            return 1;
        }
        if (fromFile != null && inputIds != null && inputIds.length > 0) {
            output.error("Must only provide one of the following options: -f and -i");
            return 1;
        }

        String[] ids;
        if (fromFile != null) {
            ids = FileUtils.readFileToString(fromFile.toFile(), UTF_8).split("\\r?\\n");
        } else {
            ids = inputIds;
        }

        output.info("Sending requests to destroy {} objects", ids.length);
        if (completely) {
            output.info("**Objects will be completely cleaned up**");
        }
        output.info(BannerUtility.getChompBanner("Destroy"));

        AgentPrincipals agent = new AgentPrincipals(parentCommand.username, new AccessGroupSet(parentCommand.groups));

        try (ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(applicationContextPath)) {
            destroyService = context.getBean(DestroyObjectsService.class);
            destroyService.destroyObjects(agent, completely, ids);
        }

        output.info("Finished destroy requests in {}ms", System.currentTimeMillis() - start);
        return 0;
    }
}
