package edu.unc.lib.boxc.tools.amq;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.jms.Connection;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import picocli.CommandLine;

public class AmqUtilsCliTest {
    private static final String BROKER_NAME = "amq-utils-cli-test";
    private static final String BROKER_URL = "vm://" + BROKER_NAME + "?create=false";
    private static BrokerService broker;

    @BeforeAll
    public static void startBroker() throws Exception {
        broker = new BrokerService();
        broker.setBrokerName(BROKER_NAME);
        broker.setPersistent(false);
        broker.setUseJmx(false);
        broker.start();
        broker.waitUntilStarted();
    }

    @AfterAll
    public static void stopBroker() throws Exception {
        broker.stop();
        broker.waitUntilStopped();
    }

    @Test
    public void inspectsAndCountsMessagesWithoutRemovingThem() throws Exception {
        String queueName = uniqueQueueName();
        sendTextMessage(queueName, "test message", "failureRoute", "route-a");

        CommandResult count = execute("count", "--queue", queueName, "--broker-url", BROKER_URL);
        assertEquals(0, count.exitCode());
        assertEquals("1", count.output().trim());

        CommandResult inspect = execute("--broker-url", BROKER_URL, "inspect", "--queue", queueName);
        assertEquals(0, inspect.exitCode());
        assertTrue(inspect.output().contains("body: test message"));
        assertTrue(inspect.output().contains("property.failureRoute: route-a"));

        CommandResult after = execute("inspect", "--queue", queueName,
                "--after", "2000-01-01T00:00:00", "--broker-url", BROKER_URL);
        assertEquals(0, after.exitCode());
        assertTrue(after.output().contains("body: test message"));

        assertEquals(1, countMessages(queueName));
    }

    @Test
    public void requeuesOnlyWhenExecuteIsSpecified() throws Exception {
        String sourceQueue = uniqueQueueName();
        String destinationQueue = uniqueQueueName();
        sendTextMessage(sourceQueue, "requeue me", "operation", "retry");

        CommandResult dryRun = execute("--broker-url", BROKER_URL, "requeue", "--queue", sourceQueue,
                "--destination", destinationQueue, "--selector", "operation = 'retry'");
        assertEquals(0, dryRun.exitCode());
        assertTrue(dryRun.output().contains("Would move 1 message"));
        assertEquals(1, countMessages(sourceQueue));
        assertEquals(0, countMessages(destinationQueue));

        CommandResult requeue = execute("--broker-url", BROKER_URL, "requeue", "--queue", sourceQueue,
                "--destination", destinationQueue, "--selector", "operation = 'retry'", "--execute");
        assertEquals(0, requeue.exitCode());
        assertTrue(requeue.output().contains("Moved 1 message"));
        assertEquals(0, countMessages(sourceQueue));
        assertEquals(1, countMessages(destinationQueue));
        assertEquals("requeue me", receiveTextMessage(destinationQueue));
    }

    @Test
    public void inspectsLongleafStyleObjectMessages() throws Exception {
        String queueName = uniqueQueueName();
        List<String> body = new ArrayList<>(List.of("info:fedora/example/123"));
        sendObjectMessage(queueName, (Serializable) body);

        CommandResult inspect = execute("inspect", "--queue", queueName, "--broker-url", BROKER_URL);

        assertEquals(0, inspect.exitCode());
        assertTrue(inspect.output().contains("info:fedora/example/123"));
        assertTrue(inspect.output().contains("object: java.util.ArrayList"));
        assertEquals(1, countMessages(queueName));
    }

    private static CommandResult execute(String... arguments) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        CommandLine commandLine = new CommandLine(new AmqUtilsCli());
        commandLine.setOut(new PrintWriter(output, true));
        int exitCode = commandLine.execute(arguments);
        return new CommandResult(exitCode, output.toString(StandardCharsets.UTF_8));
    }

    private static void sendTextMessage(String queueName, String body, String property, String value) throws Exception {
        try (Connection connection = new ActiveMQConnectionFactory(BROKER_URL).createConnection();
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
            connection.start();
            Queue queue = session.createQueue(queueName);
            try (MessageProducer producer = session.createProducer(queue)) {
                TextMessage message = session.createTextMessage(body);
                message.setStringProperty(property, value);
                producer.send(message);
            }
        }
    }

    private static void sendObjectMessage(String queueName, Serializable body) throws Exception {
        try (Connection connection = new ActiveMQConnectionFactory(BROKER_URL).createConnection();
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
            Queue queue = session.createQueue(queueName);
            try (MessageProducer producer = session.createProducer(queue)) {
                producer.send(session.createObjectMessage(body));
            }
        }
    }

    private static int countMessages(String queueName) throws Exception {
        try (Connection connection = new ActiveMQConnectionFactory(BROKER_URL).createConnection();
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
            connection.start();
            Queue queue = session.createQueue(queueName);
            try (var browser = session.createBrowser(queue)) {
                int count = 0;
                var messages = browser.getEnumeration();
                while (messages.hasMoreElements()) {
                    messages.nextElement();
                    count++;
                }
                return count;
            }
        }
    }

    private static String receiveTextMessage(String queueName) throws Exception {
        try (Connection connection = new ActiveMQConnectionFactory(BROKER_URL).createConnection();
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
            connection.start();
            Queue queue = session.createQueue(queueName);
            try (MessageConsumer consumer = session.createConsumer(queue)) {
                Message message = consumer.receive(1000);
                return ((TextMessage) message).getText();
            }
        }
    }

    private static String uniqueQueueName() {
        return "queue." + UUID.randomUUID();
    }

    private record CommandResult(int exitCode, String output) {
    }
}
