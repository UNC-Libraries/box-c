package edu.unc.lib.boxc.tools.amq;

import java.util.Enumeration;
import java.util.concurrent.Callable;

import jakarta.jms.Connection;
import jakarta.jms.Message;
import jakarta.jms.Queue;
import jakarta.jms.QueueBrowser;
import jakarta.jms.Session;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

/**
 * Prints messages from a queue without consuming them.
 *
 * @author bbpennel
 */
@Command(name = "inspect", description = "Display messages in a queue without consuming them.")
public class InspectCommand implements Callable<Integer> {
    @ParentCommand
    private AmqUtilsCli parent;

    @Option(names = { "-q", "--queue" }, required = true, description = "Queue name to inspect.")
    private String queueName;

    @Mixin
    private SelectorOptions selectorOptions;

    @Option(names = { "-l", "--limit" }, defaultValue = "100",
            description = "Maximum messages to display. Default: ${DEFAULT-VALUE}.")
    private int limit;

    @Option(names = "--body-limit", defaultValue = "5000",
            description = "Maximum body characters or bytes to display. Default: ${DEFAULT-VALUE}.")
    private int bodyLimit;

    @Override
    public Integer call() throws Exception {
        try (Connection connection = parent.createConnectionFactory().createConnection();
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
            connection.start();
            Queue queue = session.createQueue(queueName);
            try (QueueBrowser browser = session.createBrowser(queue, selectorOptions.getSelector())) {
                Enumeration<?> messages = browser.getEnumeration();
                int count = 0;
                while (messages.hasMoreElements() && count < limit) {
                    MessagePrinter.print(++count, (Message) messages.nextElement(), bodyLimit, parent.getOut());
                }
            }
        }
        return 0;
    }
}
