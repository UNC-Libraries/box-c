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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.RDFNode;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;
import org.slf4j.Logger;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

import edu.unc.lib.dl.exceptions.RepositoryException;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Ldp;
import edu.unc.lib.dl.util.RDFModelUtil;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

/**
 * @author bbpennel
 */
@Command(name = "send_message",
        description = "Command for sending JMS messages")
public class SendMessageCommand {
    private static final Logger output = getLogger(OUTPUT_LOGGER);

    private static final URI BINARY_TYPE_URI = URI.create(Ldp.NonRdfSource.getURI());

    @ParentCommand
    private MigrationCLI parentCommand;

    private JmsTemplate jmsTemplate;

    private FcrepoClient fcrepoClient;

    private String applicationContextPath = "spring/send-message-context.xml";

    @Command(name = "fedora",
            description = "Populate the index of file paths")
    public int fedoraMessages(
            @Parameters(index = "0", description = "List of ids to send messages for")
            String idsParam,
            @Option(names = {"--recursive"},
                    defaultValue = "false",
                    description = "If provided, will send messages for contained objects recursively")
            boolean recursive) {
        long start = System.currentTimeMillis();

        String[] ids = idsParam.split(",");

        output.info("Sending messages for {} objects", ids.length);
        output.info(BannerUtility.getBanner());

        try (ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(applicationContextPath)) {
            jmsTemplate = context.getBean(JmsTemplate.class);
            fcrepoClient = context.getBean(FcrepoClient.class);
            String template = loadFile("fedora_message.json.template");

            for (String id : ids) {
                sendFedoraMessage(id, template, recursive);
            }
        }

        output.info("Finished sending fedora messages in {}ms", System.currentTimeMillis() - start);

        return 0;
    }

    private void sendFedoraMessage(String id, String messageTemplate, boolean recursive) {
        PID pid = PIDs.get(id);

        output.info("Indexing {}", id);

        String msgId = UUID.randomUUID().toString();
        String timestamp = Instant.now().toString();
        String objUri = pid.getRepositoryPath();
        String parentUri = StringUtils.substringBeforeLast(objUri, "/");

        final String body = String.format(messageTemplate, msgId, timestamp, objUri, parentUri);

        jmsTemplate.send(new MessageCreator() {
            @Override
            public Message createMessage(Session session) throws JMSException {
                TextMessage msg = session.createTextMessage(body);
                return msg;
            }
        });

        if (recursive) {
            // Check for binary resource before proceeding
            try (FcrepoResponse resp = fcrepoClient.head(pid.getRepositoryUri()).perform()) {
                if (resp.hasType(BINARY_TYPE_URI)) {
                    return;
                }
            } catch (IOException | FcrepoOperationFailedException e) {
                throw new RepositoryException(e);
            }

            try (FcrepoResponse resp = fcrepoClient.get(pid.getRepositoryUri()).perform()) {
                Model model = RDFModelUtil.createModel(resp.getBody());
                NodeIterator it = model.listObjectsOfProperty(Ldp.contains);
                while (it.hasNext()) {
                    RDFNode contained = it.next();
                    String containedUri = contained.asResource().getURI();
                    sendFedoraMessage(containedUri, messageTemplate, recursive);
                }
            } catch (IOException | FcrepoOperationFailedException e) {
                throw new RepositoryException(e);
            }
        }
    }

    private static String loadFile(String rescPath) {
        InputStream stream = SendMessageCommand.class.getResourceAsStream("/" + rescPath);
        try {
            return IOUtils.toString(stream, UTF_8);
        } catch (IOException e) {
            throw new RepositoryException(e);
        }
    }
}
