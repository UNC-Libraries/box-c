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

import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.services.IndexingMessageSender;
import edu.unc.lib.dl.util.IndexingActionType;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

/**
 * Command for sending solr indexing JMS messages
 * @author bbpennel
 */
@Command(name = "send_solr_message",
    description = "Command for sending solr indexing JMS messages")
public class SendSolrMessageCommand implements Callable<Integer> {
    private static final Logger output = getLogger(OUTPUT_LOGGER);

    @ParentCommand
    private MigrationCLI parentCommand;

    @Option(names = {"--from-file", "-f"},
            description = "File path from which ids will be read. They should be newline separated")
    private Path fromFile;

    @Option(names = {"--ids", "-i"},
            split = ",",
            description = "Comma separated list of IDs. Must provide either this option of -f")
    private String[] inputIds;

    @Option(names = {"--show-types", "-s"},
            defaultValue = "false",
            description = "Output the list of possible indexing action types. No messages will be sent")
    private boolean displayIndexingTypes;

    @Option(names = {"--indexing-type", "-t"},
            description = "Type of indexing message to send. Use the -s option to"
                    + " show all available actions. Default: ADD")
    private IndexingActionType indexingActionType;

    private String applicationContextPath = "spring/send-solr-message-context.xml";

    private IndexingMessageSender indexingMessageSender;

    @Command(name = "show_types",
            description = "List the indexing action types")
    public int showTypes() {
        output.info("Showing indexing action types");
        output.info("======================================================================================");
        for (IndexingActionType type: IndexingActionType.values()) {
            output.info("{}\n       {}", type.name(), type.getDescription());
        }
        return 0;
    }

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

        if (indexingActionType == null) {
            indexingActionType = IndexingActionType.ADD;
        }

        output.info("Sending {} messages for {} objects", indexingActionType.name(), ids.length);
        output.info(BannerUtility.getBanner());

        try (ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(applicationContextPath)) {
            indexingMessageSender = context.getBean(IndexingMessageSender.class);

            for (String id : ids) {
                PID pid = PIDs.get(id);
                output.info("Sending message for {}", pid.getId());
                indexingMessageSender.sendIndexingOperation(parentCommand.username, pid, indexingActionType);
            }
        }

        output.info("Finished sending solr messages in {}ms", System.currentTimeMillis() - start);

        return 0;
    }

}
