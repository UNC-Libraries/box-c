package edu.unc.lib.boxc.tools.amq;

import java.util.Enumeration;
import java.util.concurrent.Callable;

import jakarta.jms.Connection;
import jakarta.jms.Queue;
import jakarta.jms.QueueBrowser;
import jakarta.jms.Session;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

/**
 * Counts messages visible in a queue without consuming them.
 *
 * @author bbpennel
 */
@Command(name = "count", description = "Count messages in a queue without consuming them.")
public class CountCommand implements Callable<Integer> {
    @ParentCommand
    private AmqUtilsCli parent;

    @Option(names = { "-q", "--queue" }, required = true, description = "Queue name to count.")
    private String queueName;

    @Mixin
    private SelectorOptions selectorOptions;

    @Override
    public Integer call() throws Exception {
        try (Connection connection = parent.createConnectionFactory().createConnection();
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
            connection.start();
            Queue queue = session.createQueue(queueName);
            try (QueueBrowser browser = session.createBrowser(queue, selectorOptions.selector())) {
                int count = 0;
                Enumeration<?> messages = browser.getEnumeration();
                while (messages.hasMoreElements()) {
                    messages.nextElement();
                    count++;
                }
                parent.out().println(count);
            }
        }
        return 0;
    }
}
