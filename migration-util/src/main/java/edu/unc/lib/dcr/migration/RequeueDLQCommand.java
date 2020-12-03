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

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;

import org.slf4j.Logger;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import picocli.CommandLine.Command;
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

    private String applicationContextPath = "spring/activemq-context.xml";

    @Command(name = "requeue_enhancements",
            description = "Requeue failed enhancement messages")
    public int enhancement() throws Exception {
        long start = System.currentTimeMillis();
        int cnt = 0;

        Connection connection = null;
        try (ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(applicationContextPath)) {
            ConnectionFactory connectionFactory = context.getBean(ConnectionFactory.class);

            connection = connectionFactory.createConnection();
            connection.start();

            Session session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);

            Queue dlq = session.createQueue("ActiveMQ.DLQ");
            MessageConsumer dlqConsumer = session.createConsumer(dlq, "CdrEnhancementSet IS NOT NULL");

            Queue enhQueue = session.createQueue("activemq:queue:repository.enhancements");
            MessageProducer producer = session.createProducer(enhQueue);

            while (true) {
                Message m = dlqConsumer.receive(500);
                if (m != null) {
                    output.info("Moving message {}", ++cnt);
                    producer.send(m);
                    m.acknowledge();
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

        output.info("Finished moving {} enhancement messages in {}ms", cnt, System.currentTimeMillis() - start);
        return 0;
    }
}
