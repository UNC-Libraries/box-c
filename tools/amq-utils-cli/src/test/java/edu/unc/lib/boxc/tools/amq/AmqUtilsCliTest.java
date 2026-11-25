package edu.unc.lib.boxc.tools.amq;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
    private static final String ROOT_ID = "d82a7536-32ca-4b17-a231-d54683366759";
    private static final String DEPOSIT_ID = "9ceadd3d-1b42-4132-bc77-3028a63c2fed";
    private static BrokerService broker;
    private static HttpServer fcrepoServer;
    private static String fcrepoBaseUrl;

    @BeforeAll
    public static void startBroker() throws Exception {
        broker = new BrokerService();
        broker.setBrokerName(BROKER_NAME);
        broker.setPersistent(false);
        broker.setUseJmx(false);
        broker.start();
        broker.waitUntilStarted();
        fcrepoServer = HttpServer.create(new InetSocketAddress(0), 0);
        fcrepoServer.createContext("/rest", AmqUtilsCliTest::respondToFedoraRequest);
        fcrepoServer.start();
        fcrepoBaseUrl = "http://localhost:" + fcrepoServer.getAddress().getPort() + "/rest";
    }

    @AfterAll
    public static void stopBroker() throws Exception {
        fcrepoServer.stop(0);
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

    @Test
    public void submitsEligibleBinariesDiscoveredFromFedora() throws Exception {
        Path input = Files.createTempFile("longleaf-submit", ".txt");
        Files.writeString(input, ROOT_ID + "\n");

        try {
            CommandResult submit = execute("longleaf-submit", "--action", "register", "--input", input.toString(),
                    "--fcrepo-request-base-url", fcrepoBaseUrl, "--broker-url", BROKER_URL, "--execute");

            assertEquals(0, submit.exitCode());
            assertTrue(submit.output().contains("Sent 1 register message"));
            assertEquals(fcrepoBaseUrl + "/content/d8/2a/75/36/" + ROOT_ID + "/datafs/original_file",
                    receiveTextMessage("activemq:queue:longleaf.register.batch"));
        } finally {
            Files.deleteIfExists(input);
        }
    }

    @Test
    public void submitsInternalDatastreamIdentifiedByHeadRequest() throws Exception {
        Path input = Files.createTempFile("longleaf-internal", ".txt");
        Files.writeString(input, ROOT_ID + "/md/alt_text\n");

        try {
            CommandResult submit = execute("longleaf-submit", "--action", "register", "--input", input.toString(),
                    "--fcrepo-request-base-url", fcrepoBaseUrl, "--broker-url", BROKER_URL, "--execute");

            assertEquals(0, submit.exitCode());
            assertEquals(fcrepoBaseUrl + "/content/d8/2a/75/36/" + ROOT_ID + "/md/alt_text",
                    receiveTextMessage("activemq:queue:longleaf.register.batch"));
        } finally {
            Files.deleteIfExists(input);
        }
    }

    @Test
    public void submitsCanonicalUrisWhileFetchingThroughTunnel() throws Exception {
        Path input = Files.createTempFile("longleaf-tunnel", ".txt");
        String canonicalBaseUri = "http://repository.example.test/rest";
        Files.writeString(input, ROOT_ID + "\n");

        try {
            CommandResult submit = execute("longleaf-submit", "--action", "register", "--input", input.toString(),
                    "--fcrepo-request-base-url", fcrepoBaseUrl, "--fcrepo-base-uri", canonicalBaseUri,
                    "--broker-url", BROKER_URL, "--execute");

            assertEquals(0, submit.exitCode());
            assertEquals(canonicalBaseUri + "/content/d8/2a/75/36/" + ROOT_ID + "/datafs/original_file",
                    receiveTextMessage("activemq:queue:longleaf.register.batch"));
        } finally {
            Files.deleteIfExists(input);
        }
    }

    @Test
    public void submitsDeregistrationFileUrisDirectly() throws Exception {
        Path input = Files.createTempFile("longleaf-deregister", ".txt");
        Files.writeString(input, "file:///storage/example/file.txt\n");

        try {
            CommandResult submit = execute("longleaf-submit", "--action", "deregister", "--input", input.toString(),
                    "--broker-url", BROKER_URL, "--execute");

            assertEquals(0, submit.exitCode());
            assertTrue(submit.output().contains("Sent 1 deregister message"));
            assertEquals("file:///storage/example/file.txt",
                    receiveTextMessage("activemq:queue:longleaf.deregister.batch"));
        } finally {
            Files.deleteIfExists(input);
        }
    }

    @Test
    public void submitsBinariesDiscoveredFromQualifiedDepositIds() throws Exception {
        Path input = Files.createTempFile("longleaf-deposit", ".txt");
        Files.writeString(input, "deposits/" + DEPOSIT_ID + "\n");

        try {
            CommandResult submit = execute("longleaf-submit", "--action", "register", "--input", input.toString(),
                    "--fcrepo-request-base-url", fcrepoBaseUrl, "--broker-url", BROKER_URL, "--execute");

            assertEquals(0, submit.exitCode());
            assertTrue(submit.output().contains("Sent 1 register message"));
            assertEquals(fcrepoBaseUrl + "/deposits/9c/ea/dd/3d/" + DEPOSIT_ID + "/md/event_log",
                    receiveTextMessage("activemq:queue:longleaf.register.batch"));
        } finally {
            Files.deleteIfExists(input);
        }
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

    private static void respondToFedoraRequest(HttpExchange exchange) throws IOException {
        if ("HEAD".equals(exchange.getRequestMethod())) {
            String path = exchange.getRequestURI().getPath();
            if (path.endsWith("/original_file") || path.endsWith("/jp2_access_copy") || path.endsWith("/alt_text")
                    || path.endsWith("/md/event_log")) {
                exchange.getResponseHeaders().add("Link", "<http://www.w3.org/ns/ldp#NonRDFSource>; rel=\"type\"");
            }
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
            return;
        }
        String baseUrl = "http://" + exchange.getRequestHeaders().getFirst("Host") + "/rest";
        String rootUri = baseUrl + "/content/d8/2a/75/36/" + ROOT_ID;
        String originalFileUri = rootUri + "/datafs/original_file";
        String accessCopyUri = rootUri + "/datafs/jp2_access_copy";
        String depositUri = baseUrl + "/deposits/9c/ea/dd/3d/" + DEPOSIT_ID;
        String eventLogUri = depositUri + "/md/event_log";
        String body;
        if (exchange.getRequestURI().getPath().equals("/rest/content/d8/2a/75/36/" + ROOT_ID)) {
            body = "<" + rootUri + "> <http://www.w3.org/ns/ldp#contains> <" + originalFileUri + "> .\n"
                    + "<" + rootUri + "> <http://www.w3.org/ns/ldp#contains> <" + accessCopyUri + "> .\n";
        } else if (exchange.getRequestURI().getPath()
                .equals("/rest/content/d8/2a/75/36/" + ROOT_ID + "/datafs/original_file")) {
            body = "<" + originalFileUri + "> a <http://www.w3.org/ns/ldp#NonRDFSource> .\n";
        } else if (exchange.getRequestURI().getPath()
                .equals("/rest/content/d8/2a/75/36/" + ROOT_ID + "/md/alt_text")) {
            exchange.sendResponseHeaders(406, -1);
            exchange.close();
            return;
        } else if (exchange.getRequestURI().getPath().equals("/rest/deposits/9c/ea/dd/3d/" + DEPOSIT_ID)) {
            body = "<" + depositUri + "> <http://www.w3.org/ns/ldp#contains> <" + eventLogUri + "> .\n";
        } else if (exchange.getRequestURI().getPath()
                .equals("/rest/deposits/9c/ea/dd/3d/" + DEPOSIT_ID + "/md/event_log")) {
            body = "<" + eventLogUri + "> a <http://www.w3.org/ns/ldp#NonRDFSource> .\n";
        } else {
            body = "<" + accessCopyUri + "> a <http://www.w3.org/ns/ldp#NonRDFSource> .\n";
        }
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/turtle");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private record CommandResult(int exitCode, String output) {
    }
}
