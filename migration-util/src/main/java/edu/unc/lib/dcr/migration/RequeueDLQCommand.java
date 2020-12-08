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

import java.util.function.Predicate;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.slf4j.Logger;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import edu.unc.lib.dl.exceptions.RepositoryException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

/**
 * Command for interacting with activemq DLQ
 *
 * @author bbpennel
 */
@Command(name = "dlq",
    description = "Interactions with activemq dead letter queue")
public class RequeueDLQCommand {
    private static final Logger output = getLogger(OUTPUT_LOGGER);

    @ParentCommand
    private MigrationCLI parentCommand;

    @Option(names = {"-n", "--dry-run"},
            description = "Perform inspect queue without changing it")
    private boolean dryRun;

    private String applicationContextPath = "spring/activemq-context.xml";

    @Command(name = "requeue_enhancements",
            description = "Requeue failed enhancement messages")
    public int requeueEnhancements() throws Exception {
        moveMessages("ActiveMQ.DLQ",
                "activemq:queue:repository.enhancements",
                "CdrEnhancementSet IS NOT NULL",
                null);
        return 0;
    }

    @Command(name = "requeue_solr",
            description = "Requeue solr messages")
    public int requeueSolr() throws Exception {
        moveMessages("ActiveMQ.DLQ",
                "activemq:queue:repository.solrupdate",
                null,
                m -> {
                    try {
                        if (m instanceof TextMessage) {
                            String text = ((TextMessage) m).getText();
                            return text.contains("http://cdr.unc.edu/schema/message/solr/");
                        }
                    } catch (JMSException e) {
                        throw new RepositoryException(e);
                    }
                    return false;
                });
        return 0;
    }

    @Command(name = "requeue_longleaf",
            description = "Requeue longleaf register messages")
    public int requeueLongleaf() throws Exception {
        moveMessages("longleaf.dlq",
                "register.longleaf",
                "CamelFailureRouteId = 'direct-vm://filter.longleaf'",
                null);
        return 0;
    }

    private void moveMessages(String dlqName, String destName, String filter, Predicate<Message> filterPred)
            throws Exception {
        if (dryRun) {
            output.info("Dry run -- No changes will be made");
        }
        output.info("Preparing to move messages from {} to {}", dlqName, destName);

        long start = System.currentTimeMillis();
        int cnt = 0;

        Connection connection = null;
        try (ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(applicationContextPath)) {
            ConnectionFactory connectionFactory = context.getBean(ConnectionFactory.class);

            connection = connectionFactory.createConnection();
            connection.start();

            Session session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);

            Queue dlq = session.createQueue(dlqName);
            MessageConsumer dlqConsumer;
            if (filter == null) {
                dlqConsumer = session.createConsumer(dlq);
            } else {
                dlqConsumer = session.createConsumer(dlq, filter);
            }

            Queue destQueue = session.createQueue(destName);
            MessageProducer producer = session.createProducer(destQueue);

            while (true) {
                Message m = dlqConsumer.receive(500);
                if (m != null) {
                    if (filterPred == null || filterPred.test(m)) {
                        output.info("Moving message {}", ++cnt);
                        if (!dryRun) {
                            producer.send(m);
                            m.acknowledge();
                        }
                    }
                } else {
                    break;
                }
            }

            dlqConsumer.close();
            session.close();
            connection.stop();
        } finally {
            if (connection != null) {
                connection.close();
            }
        }

        output.info("Finished moving {} messages from {} to {} in {}ms",
                cnt, dlqName, destName, System.currentTimeMillis() - start);
    }
}
