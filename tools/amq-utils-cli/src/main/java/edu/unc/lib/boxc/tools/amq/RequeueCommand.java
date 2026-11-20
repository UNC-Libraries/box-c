package edu.unc.lib.boxc.tools.amq;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.Callable;

import jakarta.jms.Connection;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.Queue;
import jakarta.jms.QueueBrowser;
import jakarta.jms.Session;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

/**
 * Moves selected messages from one queue to another.
 *
 * @author bbpennel
 */
@Command(name = "requeue", description = "Move messages between queues. Dry-run unless --execute is specified.")
public class RequeueCommand implements Callable<Integer> {
    @ParentCommand
    private AmqUtilsCli parent;

    @Option(names = { "-q", "--queue" }, required = true, description = "Source queue name.")
    private String sourceQueueName;

    @Option(names = { "-d", "--destination" }, required = true, description = "Destination queue name.")
    private String destinationQueueName;

    @Mixin
    private SelectorOptions selectorOptions;

    @Option(names = { "-l", "--limit" }, defaultValue = "100",
            description = "Maximum messages to move. Default: ${DEFAULT-VALUE}.")
    private int limit;

    @Option(names = "--execute",
            description = "Perform the move. Without this option the command only reports candidates.")
    private boolean execute;

    @Override
    public Integer call() throws Exception {
        try (Connection connection = parent.createConnectionFactory().createConnection()) {
            connection.start();
            List<String> messageIds = findMessageIds(connection);
            if (!execute) {
                parent.out().printf("Would move %d message(s) from %s to %s. Re-run with --execute to move them.%n",
                        messageIds.size(), sourceQueueName, destinationQueueName);
                return 0;
            }

            int moved = 0;
            for (String messageId : messageIds) {
                if (moveMessage(connection, messageId)) {
                    moved++;
                }
            }
            parent.out().printf("Moved %d message(s) from %s to %s.%n", moved, sourceQueueName, destinationQueueName);
        }
        return 0;
    }

    private List<String> findMessageIds(Connection connection) throws Exception {
        try (Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
            Queue sourceQueue = session.createQueue(sourceQueueName);
            try (QueueBrowser browser = session.createBrowser(sourceQueue, selectorOptions.selector())) {
                List<String> messageIds = new ArrayList<>();
                Enumeration<?> messages = browser.getEnumeration();
                while (messages.hasMoreElements() && messageIds.size() < limit) {
                    String messageId = ((Message) messages.nextElement()).getJMSMessageID();
                    if (messageId != null) {
                        messageIds.add(messageId);
                    }
                }
                return messageIds;
            }
        }
    }

    private boolean moveMessage(Connection connection, String messageId) throws Exception {
        try (Session session = connection.createSession(true, Session.SESSION_TRANSACTED)) {
            Queue sourceQueue = session.createQueue(sourceQueueName);
            Queue destinationQueue = session.createQueue(destinationQueueName);
            try (MessageConsumer consumer = session.createConsumer(sourceQueue, messageIdSelector(messageId));
                    MessageProducer producer = session.createProducer(destinationQueue)) {
                Message message = consumer.receive(1000);
                if (message == null) {
                    session.rollback();
                    return false;
                }
                producer.send(message);
                session.commit();
                return true;
            }
        }
    }

    private String messageIdSelector(String messageId) {
        return "JMSMessageID = '" + messageId.replace("'", "''") + "'";
    }
}
