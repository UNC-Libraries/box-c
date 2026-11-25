package edu.unc.lib.boxc.tools.amq;

import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;

import jakarta.jms.Connection;
import jakarta.jms.MessageProducer;
import jakarta.jms.Queue;
import jakarta.jms.Session;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

/**
 * Submits Longleaf registration or deregistration requests.
 *
 * @author bbpennel
 */
@Command(name = "longleaf-submit", mixinStandardHelpOptions = true,
        description = "Submit Longleaf register or deregister requests from a file.")
public class LongleafSubmitCommand implements Callable<Integer> {
    private static final String REGISTER_QUEUE = "activemq:queue:longleaf.register.batch";
    private static final String DEREGISTER_QUEUE = "activemq:queue:longleaf.deregister.batch";

    @ParentCommand
    private AmqUtilsCli parent;

    @Option(names = "--action", required = true, paramLabel = "<register|deregister>",
            description = "Action to submit.")
    private String action;

    @Option(names = { "-i", "--input" }, required = true, description = "One UUID, Fedora URI, or file URI per line.")
    private Path input;

    @Option(names = "--fcrepo-request-base-url",
            description = "Fedora HTTP request base. Overrides fcrepo.requestBaseUrl in configuration.")
    private String fcrepoRequestBaseUrl;

    @Option(names = "--fcrepo-base-uri",
            description = "Canonical submitted URI base. Overrides fcrepo.baseUri in configuration.")
    private String fcrepoBaseUri;

    @Option(names = "--fcrepo-username", description = "Optional Fedora username.")
    private String fcrepoUsername;

    @Option(names = "--fcrepo-password", description = "Optional Fedora password.")
    private String fcrepoPassword;

    @Option(names = "--execute",
            description = "Send messages. Without this option the command only reports candidates.")
    private boolean execute;

    @Override
    public Integer call() throws Exception {
        List<String> inputs = Files.readAllLines(input).stream().map(String::trim)
                .filter(line -> !line.isEmpty()).toList();
        String normalizedAction = action.toLowerCase(Locale.ROOT);
        List<String> messages;
        String destination;
        if ("register".equals(normalizedAction)) {
            String requestBaseUrl = parent.getConfigurationValue("fcrepo.requestBaseUrl", fcrepoRequestBaseUrl,
                    "FCREPO_REQUEST_BASE_URL", null);
            if (requestBaseUrl == null) {
                throw new IllegalArgumentException(
                        "--fcrepo-request-base-url or fcrepo.requestBaseUrl is required for registration");
            }
            String baseUri = parent.getConfigurationValue("fcrepo.baseUri", fcrepoBaseUri, "FCREPO_BASE_URI",
                    requestBaseUrl);
            messages = new FedoraContainmentWalker(HttpClient.newHttpClient(), requestBaseUrl, baseUri,
                    parent.getConfigurationValue("fcrepo.username", fcrepoUsername, "FCREPO_USERNAME", null),
                    parent.getConfigurationValue("fcrepo.password", fcrepoPassword, "FCREPO_PASSWORD", null))
                    .findRegisterableBinaries(inputs);
            destination = REGISTER_QUEUE;
        } else if ("deregister".equals(normalizedAction)) {
            messages = inputs;
            destination = DEREGISTER_QUEUE;
        } else {
            throw new IllegalArgumentException("--action must be either register or deregister");
        }

        if (!execute) {
            parent.getOut().printf("Would send %d %s message(s) to %s. Re-run with --execute to send them.%n",
                    messages.size(), normalizedAction, destination);
            return 0;
        }

        try (Connection connection = parent.createConnectionFactory().createConnection();
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
            Queue queue = session.createQueue(destination);
            try (MessageProducer producer = session.createProducer(queue)) {
                for (String message : messages) {
                    producer.send(session.createTextMessage(message));
                }
            }
        }
        parent.getOut().printf("Sent %d %s message(s) to %s.%n", messages.size(), normalizedAction, destination);
        return 0;
    }
}
