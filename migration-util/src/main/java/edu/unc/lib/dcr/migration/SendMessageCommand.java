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
import static edu.unc.lib.dl.rdf.Fcrepo4Repository.Binary;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.fcrepo.client.LinkHeaderConstants.TYPE_REL;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
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
import edu.unc.lib.dl.fcrepo4.FcrepoJmsConstants;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
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
    private static final URI WORK_TYPE_URI = URI.create(Cdr.Work.getURI());

    @ParentCommand
    private MigrationCLI parentCommand;

    @Option(names = {"--recursive", "-r"},
            defaultValue = "false",
            description = "If provided, will send messages for contained objects recursively")
    private boolean recursive;

    @Option(names = {"--follow-primary"},
            defaultValue = "false",
            description = "If provided, will send messages for primary objects of Works")
    private boolean followPrimary;

    @Option(names = {"--skip-missing"},
            defaultValue = "false",
            description = "If provided, ignore ids which do not exist")
    private boolean skipMissing;

    private JmsTemplate jmsTemplate;

    private FcrepoClient fcrepoClient;

    private String fcrepoBaseUrl;

    private String applicationContextPath = "spring/send-message-context.xml";

    @Command(name = "fedora",
            description = "Populate the index of file paths")
    public int fedoraMessages(
            @Parameters(index = "0", description = "List of ids to send messages for")
            String idsParam,
            @Option(names = {"--from-file", "-f"},
            defaultValue = "false",
            description = "First parameter will be read as a file containing a list of ids to process")
            boolean fromFile) throws IOException {
        long start = System.currentTimeMillis();

        String[] ids;
        if (fromFile) {
            ids = FileUtils.readFileToString(new File(idsParam), UTF_8).split("\\r?\\n");
        } else {
            ids = idsParam.split(",");
        }

        output.info("Sending messages for {} objects", ids.length);
        output.info(BannerUtility.getBanner());

        try (ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(applicationContextPath)) {
            jmsTemplate = context.getBean(JmsTemplate.class);
            fcrepoClient = context.getBean(FcrepoClient.class);
            fcrepoBaseUrl = context.getBeanFactory().resolveEmbeddedValue("${fcrepo.baseUrl}");

            String template = loadFile("fedora_message.json.template");

            for (String id : ids) {
                sendFedoraMessage(id, template);
            }
        }

        output.info("Finished sending fedora messages in {}ms", System.currentTimeMillis() - start);

        return 0;
    }

    private void sendFedoraMessage(String id, String messageTemplate) {
        PID pid = PIDs.get(id);

        output.info("Sending message for {}", id);

        String msgId = UUID.randomUUID().toString();
        String timestamp = Instant.now().toString();
        String objUri = pid.getRepositoryPath();
        String parentUri = StringUtils.substringBeforeLast(objUri, "/");
        String fcrepoId = objUri.replaceFirst(fcrepoBaseUrl, "");

        final String body = String.format(messageTemplate, msgId, timestamp, objUri, parentUri);

        // Check for binary resource before proceeding
        List<URI> rdfTypes;
        try (FcrepoResponse resp = fcrepoClient.head(pid.getRepositoryUri()).perform()) {
            rdfTypes = resp.getLinkHeaders(TYPE_REL);
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (FcrepoOperationFailedException e) {
            if (skipMissing && (HttpStatus.SC_NOT_FOUND == e.getStatusCode() ||
                    HttpStatus.SC_GONE == e.getStatusCode())) {
                output.warn("Skipping id {}, resource not found", id);
                return;
            }
            throw new RepositoryException(e);
        }
        boolean isBinary = rdfTypes.contains(BINARY_TYPE_URI);

        jmsTemplate.send(new MessageCreator() {
            @Override
            public Message createMessage(Session session) throws JMSException {
                TextMessage msg = session.createTextMessage(body);
                msg.setStringProperty(FcrepoJmsConstants.IDENTIFIER, fcrepoId);
                msg.setStringProperty(FcrepoJmsConstants.BASE_URL, fcrepoBaseUrl);
                if (isBinary) {
                    msg.setStringProperty(FcrepoJmsConstants.RESOURCE_TYPE, Binary.getURI());
                } else {
                    msg.setStringProperty(FcrepoJmsConstants.RESOURCE_TYPE, rdfTypes.stream()
                            .map(URI::toString).collect(Collectors.joining(",")));
                }
                msg.setStringProperty(FcrepoJmsConstants.EVENT_TYPE,
                        "https://www.w3.org/ns/activitystreams#Create,https://www.w3.org/ns/activitystreams#Update");
                return msg;
            }
        });

        if ((recursive || followPrimary) && !isBinary) {
            Model model;
            try (FcrepoResponse resp = fcrepoClient.get(pid.getRepositoryUri()).perform()) {
                model = RDFModelUtil.createModel(resp.getBody());
            } catch (IOException | FcrepoOperationFailedException e) {
                throw new RepositoryException(e);
            }
            sendMessagesForLinked(model, Ldp.contains, messageTemplate);
            if (rdfTypes.contains(WORK_TYPE_URI) && followPrimary) {
                sendMessagesForLinked(model, Cdr.primaryObject, messageTemplate);
            }
        }
    }

    private void sendMessagesForLinked(Model model, Property property, String messageTemplate) {
        NodeIterator it = model.listObjectsOfProperty(property);
        while (it.hasNext()) {
            RDFNode contained = it.next();
            String containedUri = contained.asResource().getURI();
            sendFedoraMessage(containedUri, messageTemplate);
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
